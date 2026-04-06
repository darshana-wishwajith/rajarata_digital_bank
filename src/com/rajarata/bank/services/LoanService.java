package com.rajarata.bank.services;

import com.rajarata.bank.models.loan.Loan;
import com.rajarata.bank.models.loan.LoanType;
import com.rajarata.bank.models.loan.LoanStatus;
import com.rajarata.bank.models.account.Account;
import com.rajarata.bank.models.notification.AlertType;
import com.rajarata.bank.models.transaction.*;
import com.rajarata.bank.models.user.Customer;
import com.rajarata.bank.exceptions.*;
import com.rajarata.bank.utils.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for loan management operations.
 * Handles loan applications, approval workflow, disbursement, and repayments.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class LoanService {

    /** Map of all loans by loan ID */
    private final Map<String, Loan> loansById;
    /** Reference to account service */
    private final AccountService accountService;
    /** Reference to authentication service */
    private final AuthenticationService authService;
    /** Reference to notification service */
    private NotificationService notificationService;
    /** Reference to transaction service */
    private final TransactionService transactionService;

    public LoanService(AccountService accountService, AuthenticationService authService, TransactionService transactionService) {
        this.loansById = new HashMap<>();
        this.accountService = accountService;
        this.authService = authService;
        this.transactionService = transactionService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Submits a new loan application.
     * Validates eligibility criteria before creating the application.
     * 
     * @param customerId The applying customer's ID
     * @param loanType Type of loan requested
     * @param amount Loan amount requested
     * @param termMonths Loan term in months
     * @param purpose Purpose of the loan
     * @param employmentDetails Employment information
     * @param disbursementAccount Account to receive loan funds
     * @return The created Loan application
     * @throws InvalidInputException if eligibility checks fail
     */
    public Loan applyForLoan(String customerId, LoanType loanType, double amount,
                              int termMonths, String purpose, String employmentDetails,
                              String disbursementAccount) throws InvalidInputException, InvalidAccountException {

        Customer customer = authService.getCustomer(customerId);
        if (customer == null) {
            throw new InvalidInputException("Customer not found", "customerId");
        }

        // Check if customer has any active account
        if (customer.getAccountCount() == 0) {
            throw new InvalidInputException("You must have at least one active account to apply for a loan", "accounts");
        }

        // Validate loan amount
        if (amount <= 0) {
            throw new InvalidInputException("Loan amount must be positive", "amount");
        }

        // Validate term
        if (termMonths <= 0 || termMonths > loanType.getMaxTermMonths()) {
            throw new InvalidInputException(
                "Invalid term. Maximum for " + loanType.getDisplayName() + ": " + 
                loanType.getMaxTermMonths() + " months", "termMonths");
        }

        // Credit score check
        // Check interest rate eligibility
        int creditScore = customer.getCreditScore();
        double rate = Loan.calculateInterestRate(loanType, creditScore);
        if (rate <= 0) {
            throw new InvalidInputException(
                "Loan application rejected: Credit score (" + creditScore + ") is below minimum requirement (550)",
                "creditScore");
        }

        // Create loan application
        Account account = accountService.getAccount(disbursementAccount);
        String currency = account != null ? account.getCurrency() : "LKR";

        Loan loan = new Loan(customerId, loanType, amount, currency, termMonths, purpose,
                            employmentDetails, creditScore);
        loan.setDisbursementAccount(disbursementAccount);

        loansById.put(loan.getLoanId(), loan);
        saveLoan(loan);

        FileHandler.logAudit("LOAN_APPLICATION",
                "Loan application " + loan.getLoanId() + " submitted by " + customerId +
                " - " + loanType.getDisplayName() + " $" + ValidationUtil.formatAmount(amount));

        // Notify staff about new application
        if (notificationService != null) {
            notificationService.broadcastToStaff(AlertType.LOAN_STATUS_UPDATE,
                    "New Loan Application",
                    "Loan " + loan.getLoanId() + " from " + customer.getFullName() +
                    " - $" + ValidationUtil.formatAmount(amount));
        }

        return loan;
    }

    /**
     * Approves a pending loan application and disburses funds.
     * 
     * @param loanId The loan ID to approve
     * @param staffId The approving staff member's ID
     * @param comments Approval comments
     * @return The approved Loan
     * @throws InvalidInputException if loan not found or not pending
     */
    public Loan approveLoan(String loanId, String staffId, String comments) 
            throws InvalidInputException, InvalidAccountException {

        Loan loan = loansById.get(loanId);
        if (loan == null) {
            throw new InvalidInputException("Loan not found: " + loanId, "loanId");
        }
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new InvalidInputException("Loan is not in pending status", "status");
        }

        String disbursementAccount = loan.getDisbursementAccount();
        Account account = accountService.getAccount(disbursementAccount);

        // Disbursement
        Transaction txn = new Transaction(TransactionType.LOAN_DISBURSEMENT,
                disbursementAccount, loan.getLoanAmount(),
                "Loan disbursement - " + loan.getLoanId());
        
        transactionService.processDeposit(account, txn, loan.getLoanAmount(), false);

        // Save loan status
        loan.approve(staffId, comments, disbursementAccount);
        saveLoan(loan);

        FileHandler.logAudit("LOAN_APPROVED",
                "Loan " + loanId + " approved by " + staffId + " - $" +
                ValidationUtil.formatAmount(loan.getLoanAmount()));

        // Notify customer
        if (notificationService != null) {
            notificationService.sendNotification(loan.getCustomerId(),
                    AlertType.LOAN_STATUS_UPDATE,
                    "Loan Approved!",
                    "Your loan " + loanId + " for $" + ValidationUtil.formatAmount(loan.getLoanAmount()) +
                    " has been approved. Funds disbursed to " + disbursementAccount);
        }

        return loan;
    }

    /**
     * Rejects a pending loan application.
     */
    public Loan rejectLoan(String loanId, String staffId, String comments) 
            throws InvalidInputException {

        Loan loan = loansById.get(loanId);
        if (loan == null) {
            throw new InvalidInputException("Loan not found: " + loanId, "loanId");
        }
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new InvalidInputException("Loan is not in pending status", "status");
        }

        loan.reject(staffId, comments);
        saveLoan(loan);
        FileHandler.logAudit("LOAN_REJECTED", "Loan " + loanId + " rejected by " + staffId);

        // Notify customer
        if (notificationService != null) {
            notificationService.sendNotification(loan.getCustomerId(),
                    AlertType.LOAN_STATUS_UPDATE,
                    "Loan Application Update",
                    "Your loan " + loanId + " has been rejected. Reason: " + comments);
        }

        return loan;
    }

    /**
     * Makes a payment towards an active loan.
     * 
     * @param loanId The loan ID
     * @param sourceAccount Account to deduct payment from
     * @param paymentAmount Payment amount
     * @return true if payment was processed
     */
    public boolean makeLoanPayment(String loanId, String sourceAccount, double paymentAmount) 
            throws InvalidInputException, InsufficientFundsException, InvalidAccountException {

        Loan loan = loansById.get(loanId);
        if (loan == null) {
            throw new InvalidInputException("Loan not found: " + loanId, "loanId");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new InvalidInputException("Loan is not active", "status");
        }

        Account account = accountService.getAccount(sourceAccount);

        // Create transaction for repayment
        Transaction txn = new Transaction(TransactionType.LOAN_REPAYMENT,
                sourceAccount, paymentAmount,
                "Loan payment - " + loanId);

        // Process withdrawal using transactionService
        transactionService.processWithdrawal(account, txn, paymentAmount, false);

        // Update loan status
        loan.makePayment(paymentAmount);
        saveLoan(loan);

        FileHandler.logAudit("LOAN_PAYMENT",
                "Loan payment $" + ValidationUtil.formatAmount(paymentAmount) + " for " + loanId);

        return true;
    }

    /**
     * Gets all pending loan applications (for staff review).
     * @return List of pending loans
     */
    public List<Loan> getPendingLoans() {
        return loansById.values().stream()
                .filter(l -> l.getStatus() == LoanStatus.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * Gets all loans for a specific customer.
     * @param customerId The customer ID
     * @return List of customer's loans
     */
    public List<Loan> getCustomerLoans(String customerId) {
        return loansById.values().stream()
                .filter(l -> l.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    /**
     * Gets a loan by ID.
     * @param loanId The loan ID
     * @return Loan object or null
     */
    public Loan getLoan(String loanId) {
        return loansById.get(loanId);
    }

    /**
     * Gets all loans.
     * @return Map of all loans
     */
    public Map<String, Loan> getAllLoans() {
        return new HashMap<>(loansById);
    }

    // ==================== DATA PERSISTENCE ====================

    private void saveLoan(Loan loan) {
        // Update or append
        List<String> lines = FileHandler.readAllLines(FileHandler.LOANS_FILE);
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(loan.getLoanId() + "|")) {
                lines.set(i, loan.toFileString());
                found = true;
                break;
            }
        }
        if (found) {
            FileHandler.writeAllLines(FileHandler.LOANS_FILE, lines);
        } else {
            FileHandler.appendLine(FileHandler.LOANS_FILE, loan.toFileString());
        }
    }

    /**
     * Loads all loan data from file.
     */
    public void loadLoans() {
        loansById.clear();
        List<String> lines = FileHandler.readAllLines(FileHandler.LOANS_FILE);
        int maxCounter = 0;

        for (String line : lines) {
            try {
                String[] parts = line.split(FileHandler.DELIMITER_REGEX, -1);
                if (parts.length < 20) continue;

                Loan loan = new Loan();
                loan.setLoanId(parts[0]);
                loan.setCustomerId(parts[1]);
                loan.setDisbursementAccount(parts[2]);
                loan.setLoanType(LoanType.valueOf(parts[3]));
                loan.setLoanAmount(Double.parseDouble(parts[4]));
                loan.setCurrency(parts[5]);
                loan.setInterestRate(Double.parseDouble(parts[6]));
                loan.setTermMonths(Integer.parseInt(parts[7]));
                loan.setMonthlyInstallment(Double.parseDouble(parts[8]));
                loan.setRemainingBalance(Double.parseDouble(parts[9]));
                loan.setTotalInterest(Double.parseDouble(parts[10]));
                loan.setTotalPenalties(Double.parseDouble(parts[11]));
                loan.setPenaltyCount(Integer.parseInt(parts[12]));
                loan.setStatus(LoanStatus.valueOf(parts[13]));
                if (!parts[14].isEmpty()) loan.setApprovalDate(parts[14]);
                if (!parts[15].isEmpty()) loan.setNextPaymentDate(parts[15]);
                loan.setPaymentsMade(Integer.parseInt(parts[16]));
                if (!parts[17].isEmpty()) loan.setApprovedBy(parts[17]);
                if (!parts[18].isEmpty()) loan.setApproverComments(parts[18]);
                if (!parts[19].isEmpty()) loan.setPurpose(parts[19]);
                if (!parts[20].isEmpty()) loan.setEmploymentDetails(parts[20]);
                loan.setCreditScoreAtApplication(Integer.parseInt(parts[21]));
                if (parts.length > 22 && !parts[22].isEmpty()) loan.setApplicationDate(parts[22]);

                loansById.put(loan.getLoanId(), loan);

                try {
                    int num = Integer.parseInt(loan.getLoanId().replace("LOAN-", ""));
                    maxCounter = Math.max(maxCounter, num);
                } catch (NumberFormatException e) { /* ignore */ }

            } catch (Exception e) {
                System.err.println("Warning: Failed to load loan: " + e.getMessage());
            }
        }

        Loan.setLoanCounter(maxCounter);
    }

    /**
     * Checks all active loans for upcoming payment deadlines and sends
     * reminder notifications to customers. Flags:
     * - Loans with payments due within 7 days (friendly reminder)
     * - Loans with overdue payments (urgent reminder)
     * 
     * OOP Concept: Observer Pattern - Triggers notification to the customer
     * (observer) when their loan payment deadline is approaching.
     */
    public void checkUpcomingLoanPayments() {
        if (notificationService == null) return;

        String today = DateUtil.getCurrentDate();

        for (Loan loan : loansById.values()) {
            if (loan.getStatus() != LoanStatus.ACTIVE) continue;

            String nextPayment = loan.getNextPaymentDate();
            if (nextPayment == null || nextPayment.isEmpty()) continue;

            long daysUntilDue = DateUtil.daysBetween(today, nextPayment);

            if (daysUntilDue < 0) {
                // Overdue payment — urgent notification
                String overdueMsg = "Your loan " + loan.getLoanId() + " payment of " +
                        CurrencyUtil.getCurrencySymbol(loan.getCurrency()) +
                        ValidationUtil.formatAmount(loan.getMonthlyInstallment()) +
                        " was due on " + DateUtil.formatForDisplay(nextPayment) + ". ";
                
                // Check if grace period exceeded and penalty not yet applied for THIS installment
                // Logic: if overdue days >= Grace period AND we haven't applied a penalty today/recently
                if (Math.abs(daysUntilDue) >= Loan.GRACE_PERIOD_DAYS) {
                    boolean alreadyPenalized = loan.getPaymentHistory().stream()
                            .anyMatch(h -> h.contains("PENALTY") && h.contains(nextPayment)); // Simple check
                    
                    if (!alreadyPenalized) {
                        loan.applyLatePenalty();
                        overdueMsg += "A late penalty of " + 
                                CurrencyUtil.getCurrencySymbol(loan.getCurrency()) + 
                                ValidationUtil.formatAmount(loan.calculateLatePaymentPenalty()) + 
                                " has been applied to your balance.";
                        saveLoan(loan);
                    }
                }
                
                notificationService.sendNotification(loan.getCustomerId(),
                        AlertType.LOAN_REMINDER,
                        "Loan Payment Overdue!",
                        overdueMsg + " Please make the payment immediately.");

                FileHandler.logAudit("LOAN_OVERDUE",
                        "Loan " + loan.getLoanId() + " payment overdue by " +
                        Math.abs(daysUntilDue) + " day(s). Customer: " + loan.getCustomerId());

            } else if (daysUntilDue <= 7) {
                // Approaching deadline — friendly reminder
                notificationService.sendNotification(loan.getCustomerId(),
                        AlertType.LOAN_REMINDER,
                        "Upcoming Loan Installment",
                        "Reminder: Your loan " + loan.getLoanId() + " installment of $" +
                        ValidationUtil.formatAmount(loan.getMonthlyInstallment()) +
                        " is due on " + DateUtil.formatForDisplay(nextPayment) +
                        " (" + daysUntilDue + " day(s) remaining).");
            }
        }
    }
}

