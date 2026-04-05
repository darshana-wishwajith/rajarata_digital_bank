package com.rajarata.bank.models.loan;

import com.rajarata.bank.utils.DateUtil;
import com.rajarata.bank.utils.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a loan in the banking system. Supports Personal, Auto, Home,
 * and Education loans with amortization-based repayment schedules.
 * 
 * OOP Concept: Encapsulation - All loan data is private with controlled access.
 * Loan amount and interest rate are final once approved.
 * 
 * OOP Concept: Composition - Loan HAS-A list of payment records.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class Loan {

    // ==================== PRIVATE FIELDS ====================

    /** Unique loan ID */
    private String loanId;
    /** Customer ID of the borrower */
    private String customerId;
    /** Account number where loan was disbursed */
    private String disbursementAccount;
    /** Type of loan */
    private LoanType loanType;
    /** Approved loan amount */
    private double loanAmount;
    /** Annual interest rate (APR) */
    private double interestRate;
    /** Loan term in months */
    private int termMonths;
    /** Monthly installment amount (EMI) */
    private double monthlyInstallment;
    /** Remaining principal balance */
    private double remainingBalance;
    /** Total interest to be paid over loan life */
    private double totalInterest;
    /** Current loan status */
    private LoanStatus status;
    /** Date the loan was approved and disbursed */
    private String approvalDate;
    /** Date of the next payment due */
    private String nextPaymentDate;
    /** Number of payments made */
    private int paymentsMade;
    /** Staff member who approved the loan */
    private String approvedBy;
    /** Comments from the approver */
    private String approverComments;
    /** Application purpose / reason */
    private String purpose;
    /** Employment details for the application */
    private String employmentDetails;
    /** Credit score at time of application */
    private int creditScoreAtApplication;
    /** Date the loan application was submitted */
    private String applicationDate;
    /** Payment history records */
    private List<String> paymentHistory;

    /** Late payment penalty rate (5% of installment) */
    public static final double LATE_PAYMENT_PENALTY_RATE = 0.05;
    /** Grace period for late payments in days */
    public static final int GRACE_PERIOD_DAYS = 5;

    /** Static counter for loan ID generation */
    private static int loanCounter = 0;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor.
     */
    public Loan() {
        this.paymentHistory = new ArrayList<>();
        this.status = LoanStatus.PENDING;
        this.applicationDate = DateUtil.getCurrentDate();
    }

    /**
     * Creates a new loan application.
     * 
     * @param customerId The applying customer's ID
     * @param loanType Type of loan
     * @param loanAmount Requested loan amount
     * @param termMonths Requested term in months
     * @param purpose Loan purpose description
     * @param employmentDetails Employment information
     * @param creditScore Credit score at application time
     */
    public Loan(String customerId, LoanType loanType, double loanAmount,
                int termMonths, String purpose, String employmentDetails, int creditScore) {
        this();
        this.loanId = generateLoanId();
        this.customerId = customerId;
        this.loanType = loanType;
        this.loanAmount = loanAmount;
        this.termMonths = termMonths;
        this.purpose = purpose;
        this.employmentDetails = employmentDetails;
        this.creditScoreAtApplication = creditScore;
        this.remainingBalance = loanAmount;
        this.interestRate = calculateInterestRate(creditScore);
        this.monthlyInstallment = calculateMonthlyInstallment();
        this.totalInterest = (monthlyInstallment * termMonths) - loanAmount;
    }

    // ==================== INTEREST RATE CALCULATION ====================

    /**
     * Calculates the interest rate based on credit score.
     * - 750+: 5.5% APR
     * - 650-749: 7.5% APR
     * - 550-649: 10.5% APR
     * - Below 550: Application rejected (returns -1)
     * 
     * @param creditScore The applicant's credit score
     * @return Annual interest rate as decimal, or -1 if rejected
     */
    public static double calculateInterestRate(int creditScore) {
        if (creditScore >= 750) return 0.055;
        if (creditScore >= 650) return 0.075;
        if (creditScore >= 550) return 0.105;
        return -1; // Below 550 - rejected
    }

    /**
     * Calculates the monthly installment (EMI) using the amortization formula.
     * EMI = P × r × (1+r)^n / ((1+r)^n - 1)
     * Where: P = principal, r = monthly rate, n = number of months
     * 
     * @return Monthly installment amount
     */
    public double calculateMonthlyInstallment() {
        if (interestRate <= 0 || termMonths <= 0) return 0;

        double monthlyRate = interestRate / 12;
        double factor = Math.pow(1 + monthlyRate, termMonths);
        return loanAmount * monthlyRate * factor / (factor - 1);
    }

    // ==================== LOAN OPERATIONS ====================

    /**
     * Approves the loan and sets it to active status.
     * 
     * @param staffId Staff member who approved
     * @param comments Approval comments
     * @param disbursementAccount Account to disburse funds to
     */
    public void approve(String staffId, String comments, String disbursementAccount) {
        this.status = LoanStatus.ACTIVE;
        this.approvedBy = staffId;
        this.approverComments = comments;
        this.disbursementAccount = disbursementAccount;
        this.approvalDate = DateUtil.getCurrentDate();
        this.nextPaymentDate = DateUtil.addMonths(approvalDate, 1);
        this.paymentsMade = 0;
    }

    /**
     * Rejects the loan application.
     * 
     * @param staffId Staff member who rejected
     * @param comments Rejection reason
     */
    public void reject(String staffId, String comments) {
        this.status = LoanStatus.REJECTED;
        this.approvedBy = staffId;
        this.approverComments = comments;
    }

    /**
     * Records a loan payment.
     * 
     * @param paymentAmount The amount being paid
     * @return true if the payment was processed successfully
     */
    public boolean makePayment(double paymentAmount) {
        if (status != LoanStatus.ACTIVE) return false;
        if (paymentAmount <= 0) return false;

        // Calculate interest and principal portions
        double monthlyRate = interestRate / 12;
        double interestPortion = remainingBalance * monthlyRate;
        double principalPortion = paymentAmount - interestPortion;

        if (principalPortion < 0) {
            // Payment doesn't cover interest - still record it
            principalPortion = 0;
            interestPortion = paymentAmount;
        }

        remainingBalance -= principalPortion;
        paymentsMade++;

        // Record payment
        String record = String.format("%s|%.2f|%.2f|%.2f|%.2f",
                DateUtil.getCurrentDate(), paymentAmount, principalPortion,
                interestPortion, remainingBalance);
        paymentHistory.add(record);

        // Update next payment date
        if (nextPaymentDate != null) {
            nextPaymentDate = DateUtil.addMonths(nextPaymentDate, 1);
        }

        // Check if loan is fully paid
        if (remainingBalance <= 0.01) { // Allow for floating point precision
            remainingBalance = 0;
            status = LoanStatus.PAID;
        }

        return true;
    }

    /**
     * Calculates the late payment penalty amount.
     * @return Penalty amount (5% of monthly installment)
     */
    public double calculateLatePaymentPenalty() {
        return monthlyInstallment * LATE_PAYMENT_PENALTY_RATE;
    }

    /**
     * Generates the full amortization/repayment schedule.
     * @return Formatted string of the repayment schedule
     */
    public String generateRepaymentSchedule() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                    LOAN REPAYMENT SCHEDULE                      ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Loan ID     : %-49s ║\n", loanId));
        sb.append(String.format("║ Loan Type   : %-49s ║\n", loanType.getDisplayName()));
        sb.append(String.format("║ Principal   : $%-48s ║\n", ValidationUtil.formatAmount(loanAmount)));
        sb.append(String.format("║ Rate (APR)  : %-49s ║\n", String.format("%.1f%%", interestRate * 100)));
        sb.append(String.format("║ Term        : %-49s ║\n", termMonths + " months"));
        sb.append(String.format("║ EMI         : $%-48s ║\n", ValidationUtil.formatAmount(monthlyInstallment)));
        sb.append("╠══════════════════════════════════════════════════════════════════╣\n");
        sb.append("║  Month │    Payment    │  Principal   │  Interest   │  Balance   ║\n");
        sb.append("╠════════╪═══════════════╪══════════════╪═════════════╪════════════╣\n");

        double balance = loanAmount;
        double monthlyRate = interestRate / 12;

        for (int month = 1; month <= termMonths && balance > 0.01; month++) {
            double interestPortion = balance * monthlyRate;
            double principalPortion = monthlyInstallment - interestPortion;

            if (principalPortion > balance) {
                principalPortion = balance;
            }

            balance -= principalPortion;
            if (balance < 0) balance = 0;

            sb.append(String.format("║  %4d  │ %13s │ %12s │ %11s │ %10s ║\n",
                    month,
                    "$" + ValidationUtil.formatAmount(monthlyInstallment),
                    "$" + ValidationUtil.formatAmount(principalPortion),
                    "$" + ValidationUtil.formatAmount(interestPortion),
                    "$" + ValidationUtil.formatAmount(balance)));
        }

        sb.append("╠══════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Total Payment  : $%-44s ║\n",
                ValidationUtil.formatAmount(monthlyInstallment * termMonths)));
        sb.append(String.format("║ Total Interest : $%-44s ║\n",
                ValidationUtil.formatAmount(totalInterest)));
        sb.append("╚══════════════════════════════════════════════════════════════════╝\n");

        return sb.toString();
    }

    /**
     * Gets a summary view of the loan.
     * @return Formatted loan summary
     */
    public String getLoanSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  %-12s | %-14s | $%12s | $%12s | %-10s",
                loanId,
                loanType.getDisplayName(),
                ValidationUtil.formatAmount(loanAmount),
                ValidationUtil.formatAmount(remainingBalance),
                status.getDisplayName()));
        return sb.toString();
    }

    // ==================== SERIALIZATION ====================

    /**
     * Serializes the loan to a delimited string for file storage.
     * @return Pipe-delimited string representation
     */
    public String toFileString() {
        return String.join("|",
                loanId, customerId,
                disbursementAccount != null ? disbursementAccount : "",
                loanType.name(),
                String.valueOf(loanAmount), String.valueOf(interestRate),
                String.valueOf(termMonths), String.valueOf(monthlyInstallment),
                String.valueOf(remainingBalance), String.valueOf(totalInterest),
                status.name(),
                approvalDate != null ? approvalDate : "",
                nextPaymentDate != null ? nextPaymentDate : "",
                String.valueOf(paymentsMade),
                approvedBy != null ? approvedBy : "",
                approverComments != null ? approverComments : "",
                purpose != null ? purpose : "",
                employmentDetails != null ? employmentDetails : "",
                String.valueOf(creditScoreAtApplication),
                applicationDate != null ? applicationDate : ""
        );
    }

    // ==================== STATIC METHODS ====================

    private static synchronized String generateLoanId() {
        loanCounter++;
        return String.format("LOAN-%06d", loanCounter);
    }

    public static void setLoanCounter(int counter) {
        loanCounter = counter;
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getDisbursementAccount() { return disbursementAccount; }
    public void setDisbursementAccount(String account) { this.disbursementAccount = account; }

    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }

    public double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }

    public double getMonthlyInstallment() { return monthlyInstallment; }
    public void setMonthlyInstallment(double monthlyInstallment) { this.monthlyInstallment = monthlyInstallment; }

    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }

    public double getTotalInterest() { return totalInterest; }
    public void setTotalInterest(double totalInterest) { this.totalInterest = totalInterest; }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public String getApprovalDate() { return approvalDate; }
    public void setApprovalDate(String approvalDate) { this.approvalDate = approvalDate; }

    public String getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(String date) { this.nextPaymentDate = date; }

    public int getPaymentsMade() { return paymentsMade; }
    public void setPaymentsMade(int paymentsMade) { this.paymentsMade = paymentsMade; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApproverComments() { return approverComments; }
    public void setApproverComments(String comments) { this.approverComments = comments; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getEmploymentDetails() { return employmentDetails; }
    public void setEmploymentDetails(String details) { this.employmentDetails = details; }

    public int getCreditScoreAtApplication() { return creditScoreAtApplication; }
    public void setCreditScoreAtApplication(int score) { this.creditScoreAtApplication = score; }

    public String getApplicationDate() { return applicationDate; }
    public void setApplicationDate(String date) { this.applicationDate = date; }

    public List<String> getPaymentHistory() { return paymentHistory; }
    public void setPaymentHistory(List<String> history) { this.paymentHistory = history; }
}
