package com.rajarata.bank.ui;

import com.rajarata.bank.Bank;
import com.rajarata.bank.models.user.*;
import com.rajarata.bank.models.account.*;
import com.rajarata.bank.models.transaction.*;
import com.rajarata.bank.models.loan.*;
import com.rajarata.bank.models.notification.AlertType;
import com.rajarata.bank.services.*;
import com.rajarata.bank.factory.AccountFactory;
import com.rajarata.bank.exceptions.*;
import com.rajarata.bank.utils.*;

import java.util.List;
import java.util.Map;

/**
 * Main menu controller that manages navigation and user interactions.
 * Routes users to role-specific dashboards and handles all menu operations.
 * 
 * OOP Concept: Polymorphism - Uses User base type to determine which
 * dashboard to show, then processes account operations polymorphically.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class MainMenu {

    /** Reference to the Bank singleton */
    private final Bank bank;

    /**
     * Constructor.
     */
    public MainMenu() {
        this.bank = Bank.getInstance();
    }

    /**
     * Starts the main loop of the application.
     */
    public void start() {
        System.out.println(bank.getWelcomeBanner());
        boolean running = true;

        while (running) {
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║          MAIN MENU                       ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  1. Login                                ║");
            System.out.println("║  2. Register New Account                 ║");
            System.out.println("║  3. Forgot Password                      ║");
            System.out.println("║  0. Exit                                 ║");
            System.out.println("╚══════════════════════════════════════════╝");

            int choice = ConsoleUI.readInt("Enter your choice");

            switch (choice) {
                case 1:
                    handleLogin();
                    break;
                case 2:
                    handleRegistration();
                    break;
                case 3:
                    handleForgotPassword();
                    break;
                case 0:
                    running = false;
                    bank.shutdown();
                    break;
                default:
                    ConsoleUI.printError("Invalid choice. Please select 0-3.");
            }
        }
    }

    // ==================== LOGIN ====================

    private void handleLogin() {
        ConsoleUI.printHeader("LOGIN");
        String email = ConsoleUI.readString("Email");
        String password = ConsoleUI.readPassword("Password");

        try {
            User user = bank.getAuthService().login(email, password);
            ConsoleUI.printSuccess("Welcome back, " + user.getFullName() + "!");

            // Route to role-specific dashboard
            // OOP Concept: Polymorphism - Different dashboard per user type
            switch (user.getRole()) {
                case "Customer":
                    handleCustomerDashboard((Customer) user);
                    break;
                case "Staff":
                    handleStaffDashboard((Staff) user);
                    break;
                case "Administrator":
                    handleAdminDashboard((Administrator) user);
                    break;
            }
        } catch (AccountLockedException e) {
            ConsoleUI.printError(e.getMessage());
        } catch (InvalidInputException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    // ==================== REGISTRATION ====================

    private void handleRegistration() {
        ConsoleUI.printHeader("CUSTOMER REGISTRATION");
        System.out.println("  Fill in each field below. You can type 'cancel' at any time to abort.\n");

        // --- 1. Full Name (validated immediately) ---
        String fullName = readValidatedInput("Full Name",
                "Invalid name. Use letters, spaces, dots, or hyphens (min 2 chars).",
                input -> ValidationUtil.isValidName(input));
        if (fullName == null) return;

        // --- 2. Email (validated immediately, also check for duplicates) ---
        String email = readValidatedInput("Email",
                "Invalid email format. Example: user@example.com",
                input -> {
                    if (!ValidationUtil.isValidEmail(input)) return false;
                    // Check duplicate in real-time
                    if (bank.getAuthService().getUserByEmail(input) != null) {
                        ConsoleUI.printError("An account with this email already exists.");
                        return false;
                    }
                    return true;
                });
        if (email == null) return;

        // --- 3. Phone (validated immediately) ---
        String phone = readValidatedInput("Phone (e.g., +94771234567)",
                "Invalid phone number. Use 10-15 digits, optional + prefix.",
                input -> ValidationUtil.isValidPhone(input));
        if (phone == null) return;

        // --- 4. Address (validated immediately) ---
        String address = readValidatedInput("Address",
                "Address must be at least 10 characters.",
                input -> ValidationUtil.isValidAddress(input));
        if (address == null) return;

        // --- 5. Date of Birth (validated immediately) ---
        String dob = readValidatedInput("Date of Birth (yyyy-MM-dd)",
                "Invalid date of birth. Format: yyyy-MM-dd. Must be 18+ years old.",
                input -> ValidationUtil.isValidDateOfBirth(input));
        if (dob == null) return;

        // --- 6. Government ID (validated immediately) ---
        String govtId = readValidatedInput("Government ID",
                "Invalid government ID. Use 5-20 alphanumeric characters.",
                input -> ValidationUtil.isValidGovtId(input));
        if (govtId == null) return;

        // --- 7. Password (validated immediately with requirements shown) ---
        System.out.println("\n  " + ValidationUtil.getPasswordRequirements());
        String password = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            String pwd = ConsoleUI.readPassword("Password");
            if ("cancel".equalsIgnoreCase(pwd)) {
                ConsoleUI.printInfo("Registration cancelled.");
                return;
            }
            if (!ValidationUtil.isValidPassword(pwd)) {
                ConsoleUI.printError(ValidationUtil.getPasswordRequirements());
                if (attempt < 3) {
                    ConsoleUI.printInfo("Please try again. Attempt " + (attempt + 1) + " of 3.");
                } else {
                    ConsoleUI.printError("Maximum attempts reached. Returning to main menu.");
                    return;
                }
                continue;
            }
            String confirmPwd = ConsoleUI.readPassword("Confirm Password");
            if (!pwd.equals(confirmPwd)) {
                ConsoleUI.printError("Passwords do not match.");
                if (attempt < 3) {
                    ConsoleUI.printInfo("Please try again. Attempt " + (attempt + 1) + " of 3.");
                } else {
                    ConsoleUI.printError("Maximum attempts reached. Returning to main menu.");
                    return;
                }
                continue;
            }
            password = pwd;
            break;
        }
        if (password == null) return;

        // --- 8. Security Question (basic non-empty check) ---
        String secQuestion = readValidatedInput("Security Question (for password recovery)",
                "Security question cannot be empty.",
                input -> ValidationUtil.isNotEmpty(input));
        if (secQuestion == null) return;

        // --- 9. Security Answer (basic non-empty check) ---
        String secAnswer = readValidatedInput("Security Answer",
                "Security answer cannot be empty.",
                input -> ValidationUtil.isNotEmpty(input));
        if (secAnswer == null) return;

        // All fields validated — now register
        try {
            Customer customer = bank.getAuthService().registerCustomer(
                    fullName, email, phone, address, dob, govtId,
                    password, secQuestion, secAnswer);

            ConsoleUI.printSuccess("Registration successful!");
            System.out.println("  Your Customer ID: " + customer.getUserId());
            System.out.println("  Login with your email and password.\n");

        } catch (InvalidInputException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    /**
     * Helper method for real-time input validation with retry.
     * Prompts the user for input, validates immediately, and allows up to 3 retries.
     * User can type 'cancel' to abort the registration process.
     * 
     * @param prompt The input prompt label
     * @param errorMessage The error message to show on invalid input
     * @param validator A functional interface that returns true if input is valid
     * @return The validated input, or null if the user cancelled or exceeded retries
     */
    private String readValidatedInput(String prompt, String errorMessage, 
                                       java.util.function.Predicate<String> validator) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String input = ConsoleUI.readString(prompt);
            if ("cancel".equalsIgnoreCase(input)) {
                ConsoleUI.printInfo("Registration cancelled.");
                return null;
            }
            if (validator.test(input)) {
                return input; // Valid — move on immediately
            }
            ConsoleUI.printError(errorMessage);
            if (attempt < 3) {
                ConsoleUI.printInfo("Please try again. Attempt " + (attempt + 1) + " of 3.");
            } else {
                ConsoleUI.printError("Maximum attempts reached. Returning to main menu.");
                return null;
            }
        }
        return null;
    }

    // ==================== FORGOT PASSWORD ====================

    private void handleForgotPassword() {
        ConsoleUI.printHeader("PASSWORD RECOVERY");
        String email = ConsoleUI.readString("Enter your email");

        String question = bank.getAuthService().getSecurityQuestion(email);
        if (question == null) {
            ConsoleUI.printError("No account found for this email.");
            return;
        }

        System.out.println("  Security Question: " + question);
        String answer = ConsoleUI.readString("Your Answer");
        String newPassword = ConsoleUI.readPassword("New Password");

        try {
            boolean recovered = bank.getAuthService().recoverPassword(email, answer, newPassword);
            if (recovered) {
                ConsoleUI.printSuccess("Password has been reset. You can now login.");
            } else {
                ConsoleUI.printError("Incorrect security answer.");
            }
        } catch (InvalidInputException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    // ==================== CUSTOMER DASHBOARD ====================

    private void handleCustomerDashboard(Customer customer) {
        boolean active = true;
        while (active) {
            System.out.println(customer.getDashboardMenu());
            int choice = ConsoleUI.readInt("Enter your choice");

            try {
                switch (choice) {
                    case 1: viewAccounts(customer); break;
                    case 2: openNewAccount(customer); break;
                    case 3: makeDeposit(customer); break;
                    case 4: makeWithdrawal(customer); break;
                    case 5: makeTransfer(customer); break;
                    case 6: viewTransactionHistory(customer); break;
                    case 7: applyForLoan(customer); break;
                    case 8: viewLoanStatus(customer); break;
                    case 9: makeLoanPayment(customer); break;
                    case 10: payBills(customer); break;
                    case 11: managePayees(customer); break;
                    case 12: currencyExchange(customer); break;
                    case 13: generateStatement(customer); break;
                    case 14: viewNotifications(customer); break;
                    case 15: System.out.println(customer.getProfileSummary()); ConsoleUI.pressEnterToContinue(); break;
                    case 16: changePassword(); break;
                    case 17: interestSimulator(customer); break;
                    case 0:
                        bank.getAuthService().logout();
                        ConsoleUI.printInfo("Logged out successfully.");
                        active = false;
                        break;
                    default:
                        ConsoleUI.printError("Invalid choice.");
                }
            } catch (Exception e) {
                ConsoleUI.printError(e.getMessage());
            }
        }
    }

    // ==================== ACCOUNT OPERATIONS ====================

    private void viewAccounts(Customer customer) {
        System.out.println(customer.getAccountsSummary());
        ConsoleUI.pressEnterToContinue();
    }

    private void openNewAccount(Customer customer) {
        System.out.println(AccountFactory.getAccountTypeDescriptions());
        int typeChoice = ConsoleUI.readInt("Select account type (1-4)");

        String accountType;
        switch (typeChoice) {
            case 1: accountType = "Savings"; break;
            case 2: accountType = "Checking"; break;
            case 3: accountType = "Student"; break;
            case 4: accountType = "Fixed Deposit"; break;
            default: ConsoleUI.printError("Invalid choice."); return;
        }

        double minDeposit = AccountFactory.getMinimumDeposit(accountType);
        System.out.println("  Minimum initial deposit: $" + ValidationUtil.formatAmount(minDeposit));
        double deposit = ConsoleUI.readDouble("Initial deposit amount ($)");

        int lockIn = 0;
        if ("Fixed Deposit".equals(accountType)) {
            System.out.println("  Available lock-in periods: 6, 12, 24, 36 months");
            lockIn = ConsoleUI.readInt("Lock-in period (months)");
        }

        try {
            Account account = bank.getAccountService().openAccount(
                    customer.getUserId(), accountType, deposit, "USD", lockIn);
            ConsoleUI.printSuccess("Account created! Number: " + account.getAccountNumber());
            System.out.println("  Type: " + account.getAccountType());
            System.out.println("  Balance: $" + ValidationUtil.formatAmount(account.getBalance()));
        } catch (InvalidAccountException | InvalidInputException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void makeDeposit(Customer customer) {
        ConsoleUI.printHeader("DEPOSIT");
        if (customer.getAccountCount() == 0) {
            ConsoleUI.printError("No accounts found. Please open an account first.");
            return;
        }
        System.out.println(customer.getAccountsSummary());
        String accNum = ConsoleUI.readString("Account number");
        double amount = ConsoleUI.readDouble("Deposit amount ($)");
        String desc = ConsoleUI.readString("Description (optional, press Enter to skip)");
        if (desc.isEmpty()) desc = "Cash deposit";

        try {
            Transaction txn = bank.getTransactionService().deposit(accNum, amount, desc);
            ConsoleUI.printSuccess("Deposit successful!");
            System.out.println(txn.getDetailedView());
        } catch (InvalidAccountException | InvalidInputException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void makeWithdrawal(Customer customer) {
        ConsoleUI.printHeader("WITHDRAWAL");
        if (customer.getAccountCount() == 0) {
            ConsoleUI.printError("No accounts found."); return;
        }
        System.out.println(customer.getAccountsSummary());
        String accNum = ConsoleUI.readString("Account number");
        double amount = ConsoleUI.readDouble("Withdrawal amount ($)");

        try {
            Transaction txn = bank.getTransactionService().withdraw(accNum, amount, "Cash withdrawal");
            ConsoleUI.printSuccess("Withdrawal successful!");
            System.out.println(txn.getDetailedView());
        } catch (InsufficientFundsException e) {
            ConsoleUI.printError("Insufficient funds. Available: $" + ValidationUtil.formatAmount(e.getAvailableBalance()));
        } catch (InvalidAccountException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void makeTransfer(Customer customer) {
        ConsoleUI.printHeader("FUND TRANSFER");
        System.out.println(customer.getAccountsSummary());
        String sourceAcc = ConsoleUI.readString("Source account number");
        String destAcc = ConsoleUI.readString("Destination account number");
        double amount = ConsoleUI.readDouble("Transfer amount");
        String desc = ConsoleUI.readString("Description (optional)");
        if (desc.isEmpty()) desc = null;

        // Show cross-currency conversion preview if applicable
        try {
            Account srcAccount = bank.getAccountService().getAccount(sourceAcc);
            Account dstAccount = bank.getAccountService().getAccount(destAcc);
            String srcCurr = srcAccount.getCurrency();
            String dstCurr = dstAccount.getCurrency();

            String confirmMsg = "Transfer " + ValidationUtil.formatAmount(amount) + " " +
                    srcCurr + " from " + sourceAcc + " to " + destAcc;

            if (!srcCurr.equals(dstCurr)) {
                double rate = bank.getCurrencyService().getExchangeRate(srcCurr, dstCurr);
                double converted = bank.getCurrencyService().convert(amount, srcCurr, dstCurr);
                double fee = converted * 0.005; // 0.5% fee
                double netAmount = converted - fee;

                System.out.println("\n  ┌─── Currency Conversion Preview ───────┐");
                System.out.printf("  │ Source         : %s %-20s │\n", ValidationUtil.formatAmount(amount), srcCurr);
                System.out.printf("  │ Exchange Rate  : 1 %s = %.4f %s    │\n", srcCurr, rate, dstCurr);
                System.out.printf("  │ Converted      : %s %-20s │\n", ValidationUtil.formatAmount(converted), dstCurr);
                System.out.printf("  │ Fee (0.5%%)     : %s %-20s │\n", ValidationUtil.formatAmount(fee), dstCurr);
                System.out.printf("  │ Net to Dest    : %s %-20s │\n", ValidationUtil.formatAmount(netAmount), dstCurr);
                System.out.println("  └─────────────────────────────────────────┘");

                confirmMsg += " (" + ValidationUtil.formatAmount(netAmount) + " " + dstCurr + " after fee)";
            }

            if (!ConsoleUI.confirm(confirmMsg + "?")) {
                ConsoleUI.printInfo("Transfer cancelled.");
                return;
            }

            Transaction txn = bank.getTransactionService().transfer(sourceAcc, destAcc, amount, desc);
            ConsoleUI.printSuccess("Transfer successful!");
            System.out.println(txn.getDetailedView());
        } catch (InsufficientFundsException | InvalidAccountException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void viewTransactionHistory(Customer customer) {
        ConsoleUI.printHeader("TRANSACTION HISTORY");
        System.out.println(customer.getAccountsSummary());
        String accNum = ConsoleUI.readString("Account number");

        try {
            int pageSize = 10;
            int totalPages = bank.getTransactionService().getTransactionPageCount(accNum, pageSize);
            if (totalPages == 0) {
                ConsoleUI.printInfo("No transactions found.");
                return;
            }

            int page = 1;
            boolean viewing = true;
            while (viewing) {
                List<Transaction> txns = bank.getTransactionService()
                        .getTransactionHistory(accNum, page, pageSize);

                System.out.println("\n  Transactions (Page " + page + " of " + totalPages + "):");
                System.out.println("  " + "─".repeat(80));
                System.out.printf("  %-12s %-14s %14s %-12s %s\n",
                        "Date", "Type", "Amount ($)", "Status", "Description");
                System.out.println("  " + "─".repeat(80));
                for (Transaction txn : txns) {
                    System.out.println(txn.getTransactionSummary());
                }
                System.out.println("  " + "─".repeat(80));
                System.out.println("  [N]ext  [P]rev  [E]xport CSV  [Q]uit");

                String cmd = ConsoleUI.readString("Command").toUpperCase();
                switch (cmd) {
                    case "N": if (page < totalPages) page++; break;
                    case "P": if (page > 1) page--; break;
                    case "E":
                        String path = "data/transactions_" + accNum + ".csv";
                        bank.getTransactionService().exportTransactionHistory(accNum, path);
                        ConsoleUI.printSuccess("Exported to " + path);
                        break;
                    case "Q": viewing = false; break;
                }
            }
        } catch (InvalidAccountException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    // ==================== LOAN OPERATIONS ====================

    private void applyForLoan(Customer customer) {
        ConsoleUI.printHeader("LOAN APPLICATION");
        System.out.println("  Loan Types:");
        System.out.println("  1. Personal Loan (max 60 months)");
        System.out.println("  2. Auto Loan (max 84 months)");
        System.out.println("  3. Home Loan (max 360 months)");
        System.out.println("  4. Education Loan (max 120 months)");

        int typeChoice = ConsoleUI.readInt("Select loan type");
        LoanType loanType;
        switch (typeChoice) {
            case 1: loanType = LoanType.PERSONAL; break;
            case 2: loanType = LoanType.AUTO; break;
            case 3: loanType = LoanType.HOME; break;
            case 4: loanType = LoanType.EDUCATION; break;
            default: ConsoleUI.printError("Invalid choice."); return;
        }

        double amount = ConsoleUI.readDouble("Loan amount ($)");
        int term = ConsoleUI.readInt("Term (months)");
        String purpose = ConsoleUI.readString("Purpose of loan");
        String employment = ConsoleUI.readString("Employment details");

        System.out.println(customer.getAccountsSummary());
        String disbAccount = ConsoleUI.readString("Disbursement account number");

        // Show estimated rate
        double rate = Loan.calculateInterestRate(customer.getCreditScore());
        if (rate < 0) {
            ConsoleUI.printError("Your credit score (" + customer.getCreditScore() + ") does not meet minimum requirements.");
            return;
        }
        System.out.printf("  Your credit score: %d  |  Estimated rate: %.1f%% APR\n",
                customer.getCreditScore(), rate * 100);

        if (!ConsoleUI.confirm("Submit loan application?")) {
            ConsoleUI.printInfo("Application cancelled.");
            return;
        }

        try {
            Loan loan = bank.getLoanService().applyForLoan(
                    customer.getUserId(), loanType, amount, term,
                    purpose, employment, disbAccount);
            ConsoleUI.printSuccess("Loan application submitted! ID: " + loan.getLoanId());
            System.out.println("  Status: " + loan.getStatus().getDisplayName());
            System.out.printf("  Monthly EMI: $%s\n", ValidationUtil.formatAmount(loan.getMonthlyInstallment()));
        } catch (InvalidInputException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void viewLoanStatus(Customer customer) {
        ConsoleUI.printHeader("MY LOANS");
        List<Loan> loans = bank.getLoanService().getCustomerLoans(customer.getUserId());
        if (loans.isEmpty()) {
            ConsoleUI.printInfo("No loans found.");
            return;
        }

        System.out.printf("  %-12s %-14s %14s %14s %-10s\n",
                "Loan ID", "Type", "Amount ($)", "Remaining ($)", "Status");
        System.out.println("  " + "─".repeat(70));
        for (Loan loan : loans) {
            System.out.println(loan.getLoanSummary());
        }

        String loanId = ConsoleUI.readString("Enter Loan ID for details (or press Enter to skip)");
        if (!loanId.isEmpty()) {
            Loan loan = bank.getLoanService().getLoan(loanId);
            if (loan != null) {
                System.out.println(loan.generateRepaymentSchedule());
            }
        }
        ConsoleUI.pressEnterToContinue();
    }

    private void makeLoanPayment(Customer customer) {
        ConsoleUI.printHeader("LOAN PAYMENT");
        List<Loan> loans = bank.getLoanService().getCustomerLoans(customer.getUserId());
        List<Loan> activeLoans = new java.util.ArrayList<>();
        for (Loan l : loans) { if (l.getStatus() == LoanStatus.ACTIVE) activeLoans.add(l); }

        if (activeLoans.isEmpty()) {
            ConsoleUI.printInfo("No active loans.");
            return;
        }

        for (Loan l : activeLoans) {
            System.out.printf("  %s - EMI: $%s, Remaining: $%s\n",
                    l.getLoanId(),
                    ValidationUtil.formatAmount(l.getMonthlyInstallment()),
                    ValidationUtil.formatAmount(l.getRemainingBalance()));
        }

        String loanId = ConsoleUI.readString("Loan ID");
        System.out.println(customer.getAccountsSummary());
        String sourceAcc = ConsoleUI.readString("Pay from account");
        double amount = ConsoleUI.readDouble("Payment amount ($)");

        try {
            bank.getLoanService().makeLoanPayment(loanId, sourceAcc, amount);
            Loan loan = bank.getLoanService().getLoan(loanId);
            ConsoleUI.printSuccess("Payment processed!");
            System.out.printf("  Remaining balance: $%s\n", ValidationUtil.formatAmount(loan.getRemainingBalance()));
        } catch (Exception e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    // ==================== BILL PAYMENT ====================

    private void payBills(Customer customer) {
        ConsoleUI.printHeader("BILL PAYMENT");
        System.out.println(bank.getBillPaymentService().getProviderList());

        List<String[]> payees = bank.getBillPaymentService().getSavedPayees(customer.getUserId());
        if (!payees.isEmpty()) {
            System.out.println("\n  Saved Payees:");
            for (int i = 0; i < payees.size(); i++) {
                String[] p = payees.get(i);
                System.out.printf("  %d. %s - %s (%s)\n", i + 1, p[2], p[0], p[1]);
            }
        }

        int providerChoice = ConsoleUI.readInt("Select provider (1-4)");
        if (providerChoice < 1 || providerChoice > 4) {
            ConsoleUI.printError("Invalid choice."); return;
        }

        String providerType = BillPaymentService.PROVIDERS[providerChoice - 1][1];
        String billAccount = ConsoleUI.readString("Bill account number");
        String nickname = ConsoleUI.readString("Payee nickname");
        double amount = ConsoleUI.readDouble("Payment amount ($)");

        System.out.println(customer.getAccountsSummary());
        String sourceAcc = ConsoleUI.readString("Pay from account");

        if (!ConsoleUI.confirm("Pay $" + ValidationUtil.formatAmount(amount) + " to " + nickname + "?")) {
            ConsoleUI.printInfo("Payment cancelled.");
            return;
        }

        try {
            Transaction txn = bank.getBillPaymentService().payBill(
                    sourceAcc, providerType, billAccount, amount, nickname);
            ConsoleUI.printSuccess("Payment successful!");
            System.out.println(bank.getBillPaymentService().generateReceipt(txn));

            if (ConsoleUI.confirm("Save this payee for future payments?")) {
                bank.getBillPaymentService().addPayee(customer.getUserId(),
                        providerType, billAccount, nickname);
                ConsoleUI.printSuccess("Payee saved.");
            }
        } catch (Exception e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void managePayees(Customer customer) {
        ConsoleUI.printHeader("MY PAYEES");
        List<String[]> payees = bank.getBillPaymentService().getSavedPayees(customer.getUserId());
        if (payees.isEmpty()) {
            ConsoleUI.printInfo("No saved payees.");
        } else {
            for (int i = 0; i < payees.size(); i++) {
                String[] p = payees.get(i);
                System.out.printf("  %d. %-15s | %-12s | %s\n", i + 1, p[2], p[0], p[1]);
            }
        }

        if (ConsoleUI.confirm("Add a new payee?")) {
            System.out.println(bank.getBillPaymentService().getProviderList());
            int choice = ConsoleUI.readInt("Select provider (1-4)");
            if (choice >= 1 && choice <= 4) {
                String type = BillPaymentService.PROVIDERS[choice - 1][1];
                String accNum = ConsoleUI.readString("Account number");
                String nick = ConsoleUI.readString("Nickname");
                try {
                    bank.getBillPaymentService().addPayee(customer.getUserId(), type, accNum, nick);
                    ConsoleUI.printSuccess("Payee added.");
                } catch (InvalidInputException e) {
                    ConsoleUI.printError(e.getMessage());
                }
            }
        }
    }

    // ==================== CURRENCY EXCHANGE ====================

    private void currencyExchange(Customer customer) {
        ConsoleUI.printHeader("CURRENCY EXCHANGE");
        System.out.println(bank.getCurrencyService().getExchangeRateDisplay());

        String from = ConsoleUI.readString("Source currency (USD/EUR/GBP/LKR)").toUpperCase();
        String to = ConsoleUI.readString("Target currency (USD/EUR/GBP/LKR)").toUpperCase();
        double amount = ConsoleUI.readDouble("Amount to convert");

        if (!bank.getCurrencyService().isSupportedCurrency(from) ||
            !bank.getCurrencyService().isSupportedCurrency(to)) {
            ConsoleUI.printError("Unsupported currency.");
            return;
        }

        double converted = bank.getCurrencyService().convert(amount, from, to);
        double rate = bank.getCurrencyService().getExchangeRate(from, to);
        System.out.printf("\n  %s %s = %s %s\n", ValidationUtil.formatAmount(amount), from,
                ValidationUtil.formatAmount(converted), to);
        System.out.printf("  Exchange Rate: 1 %s = %.4f %s\n", from, rate, to);
        ConsoleUI.pressEnterToContinue();
    }

    // ==================== INTEREST SIMULATOR ====================

    /**
     * Interactive interest simulation tool.
     * Allows customers to simulate projected interest growth for any amount,
     * account type, and duration using the bank's actual interest rates.
     *
     * OOP Concept: Polymorphism - Each account type has different interest
     * rates and calculation methods (simple vs compound).
     */
    private void interestSimulator(Customer customer) {
        ConsoleUI.printHeader("INTEREST SIMULATOR");
        System.out.println("  Simulate projected returns on your deposits.\n");
        System.out.println("  Account Types & Rates:");
        System.out.println("  ─────────────────────────────────────────");
        System.out.println("  1. Savings Account       - 3.50% p.a. (Simple Interest)");
        System.out.println("  2. Checking Account      - 0.50% p.a. (Simple Interest)");
        System.out.println("  3. Student Account       - 2.00% p.a. (Simple Interest)");
        System.out.println("  4. Fixed Deposit Account - 5.50% p.a. (Compound Interest, Monthly)");

        int typeChoice = ConsoleUI.readInt("\n  Select account type (1-4)");
        String accountType;
        double annualRate;
        boolean isCompound;

        switch (typeChoice) {
            case 1: accountType = "Savings Account";       annualRate = 0.035; isCompound = false; break;
            case 2: accountType = "Checking Account";      annualRate = 0.005; isCompound = false; break;
            case 3: accountType = "Student Account";       annualRate = 0.020; isCompound = false; break;
            case 4: accountType = "Fixed Deposit Account"; annualRate = 0.055; isCompound = true;  break;
            default: ConsoleUI.printError("Invalid choice."); return;
        }

        double principal = ConsoleUI.readDouble("  Principal amount ($)");
        if (principal <= 0) {
            ConsoleUI.printError("Amount must be positive.");
            return;
        }

        int durationMonths = ConsoleUI.readInt("  Duration (months, 1-360)");
        if (durationMonths <= 0 || durationMonths > 360) {
            ConsoleUI.printError("Duration must be between 1 and 360 months.");
            return;
        }

        // Generate simulation report
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              INTEREST SIMULATION REPORT                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Account Type    : %-42s ║\n", accountType);
        System.out.printf("║ Principal       : $%-41s ║\n", ValidationUtil.formatAmount(principal));
        System.out.printf("║ Annual Rate     : %-42s ║\n", String.format("%.2f%%", annualRate * 100));
        System.out.printf("║ Duration        : %-42s ║\n", durationMonths + " months (" + String.format("%.1f", durationMonths / 12.0) + " years)");
        System.out.printf("║ Method          : %-42s ║\n", isCompound ? "Compound (Monthly)" : "Simple Interest");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Year │   Opening Bal  │    Interest   │   Closing Bal     ║");
        System.out.println("╠═══════╪════════════════╪═══════════════╪═══════════════════╣");

        double currentBalance = principal;
        double totalInterest = 0;
        int totalYears = (int) Math.ceil(durationMonths / 12.0);
        int remainingMonths = durationMonths;

        for (int year = 1; year <= totalYears; year++) {
            double openingBalance = currentBalance;
            int monthsThisYear = Math.min(12, remainingMonths);
            double yearInterest = 0;

            if (isCompound) {
                // Compound interest (monthly compounding)
                double monthlyRate = annualRate / 12;
                for (int m = 0; m < monthsThisYear; m++) {
                    double monthlyInterest = currentBalance * monthlyRate;
                    yearInterest += monthlyInterest;
                    currentBalance += monthlyInterest;
                }
            } else {
                // Simple interest
                yearInterest = currentBalance * annualRate * monthsThisYear / 12.0;
                currentBalance += yearInterest;
            }

            totalInterest += yearInterest;
            remainingMonths -= monthsThisYear;

            String yearLabel = (monthsThisYear < 12)
                    ? year + " (" + monthsThisYear + "m)"
                    : String.valueOf(year);

            System.out.printf("║  %-4s │ $%13s │ $%12s │ $%16s ║\n",
                    yearLabel,
                    ValidationUtil.formatAmount(openingBalance),
                    ValidationUtil.formatAmount(yearInterest),
                    ValidationUtil.formatAmount(currentBalance));
        }

        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Final Balance        : $%-37s ║\n", ValidationUtil.formatAmount(currentBalance));
        System.out.printf("║ Total Interest Earned: $%-37s ║\n", ValidationUtil.formatAmount(totalInterest));
        System.out.printf("║ Effective Return     : %-38s ║\n",
                String.format("%.2f%%", (totalInterest / principal) * 100));
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║   * This is a simulation only. Actual returns may vary.     ║");
        System.out.println("║   * Rates are subject to change at the bank's discretion.   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        // Log the simulation
        FileHandler.logAudit("INTEREST_SIMULATION",
                "Customer " + customer.getUserId() + " simulated " + accountType +
                " with $" + ValidationUtil.formatAmount(principal) +
                " for " + durationMonths + " months. Projected return: $" +
                ValidationUtil.formatAmount(totalInterest));

        ConsoleUI.pressEnterToContinue();
    }

    // ==================== STATEMENT ====================

    private void generateStatement(Customer customer) {
        ConsoleUI.printHeader("GENERATE STATEMENT");
        System.out.println(customer.getAccountsSummary());
        String accNum = ConsoleUI.readString("Account number");
        int month = ConsoleUI.readInt("Month (1-12)");
        int year = ConsoleUI.readInt("Year (e.g., 2026)");

        String statement = bank.getReportingService().generateMonthlyStatement(accNum, month, year);
        System.out.println(statement);

        if (ConsoleUI.confirm("Save statement to file?")) {
            bank.getReportingService().saveStatementToFile(accNum, statement);
        }
    }

    // ==================== NOTIFICATIONS ====================

    private void viewNotifications(User user) {
        ConsoleUI.printHeader("NOTIFICATIONS");
        System.out.println(bank.getNotificationService().getNotificationDisplay(user.getUserId()));

        System.out.println("  [A] Mark all as read  [Q] Back");
        String cmd = ConsoleUI.readString("Command").toUpperCase();
        if ("A".equals(cmd)) {
            bank.getNotificationService().markAllAsRead(user.getUserId());
            ConsoleUI.printSuccess("All notifications marked as read.");
        }
    }

    // ==================== PASSWORD CHANGE ====================

    private void changePassword() {
        ConsoleUI.printHeader("CHANGE PASSWORD");
        String oldPwd = ConsoleUI.readPassword("Current password");
        String newPwd = ConsoleUI.readPassword("New password");
        String confirmPwd = ConsoleUI.readPassword("Confirm new password");

        if (!newPwd.equals(confirmPwd)) {
            ConsoleUI.printError("Passwords do not match.");
            return;
        }
        try {
            boolean changed = bank.getAuthService().changePassword(oldPwd, newPwd);
            if (changed) {
                ConsoleUI.printSuccess("Password changed successfully.");
            } else {
                ConsoleUI.printError("Current password is incorrect.");
            }
        } catch (InvalidInputException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    // ==================== STAFF DASHBOARD ====================

    private void handleStaffDashboard(Staff staff) {
        boolean active = true;
        while (active) {
            System.out.println(staff.getDashboardMenu());
            int choice = ConsoleUI.readInt("Enter your choice");

            try {
                switch (choice) {
                    case 1: reviewLoanApplications(); break;
                    case 2: viewAllCustomers(); break;
                    case 3: searchCustomer(); break;
                    case 4: viewCustomerAccounts(); break;
                    case 5: monitorTransactions(); break;
                    case 6: flagSuspiciousActivity(); break;
                    case 7: viewFraudCases(); break;
                    case 8: System.out.println(bank.getReportingService().generateSystemStatistics()); ConsoleUI.pressEnterToContinue(); break;
                    case 9: System.out.println(bank.getReportingService().generateLoanPerformanceReport()); ConsoleUI.pressEnterToContinue(); break;
                    case 10: viewNotifications(staff); break;
                    case 11: System.out.println(staff.getProfileSummary()); ConsoleUI.pressEnterToContinue(); break;
                    case 12: changePassword(); break;
                    case 0:
                        bank.getAuthService().logout();
                        ConsoleUI.printInfo("Logged out successfully.");
                        active = false;
                        break;
                    default: ConsoleUI.printError("Invalid choice.");
                }
            } catch (Exception e) {
                ConsoleUI.printError(e.getMessage());
            }
        }
    }

    // ==================== STAFF OPERATIONS ====================

    private void reviewLoanApplications() {
        ConsoleUI.printHeader("PENDING LOAN APPLICATIONS");
        List<Loan> pending = bank.getLoanService().getPendingLoans();
        if (pending.isEmpty()) {
            ConsoleUI.printInfo("No pending applications.");
            return;
        }

        for (Loan loan : pending) {
            System.out.println("  " + loan.getLoanSummary());
        }

        String loanId = ConsoleUI.readString("Enter Loan ID to review (or Enter to skip)");
        if (loanId.isEmpty()) return;

        Loan loan = bank.getLoanService().getLoan(loanId);
        if (loan == null) {
            ConsoleUI.printError("Loan not found.");
            return;
        }

        System.out.println(loan.generateRepaymentSchedule());
        System.out.println("  Credit Score: " + loan.getCreditScoreAtApplication());
        System.out.println("  Purpose: " + loan.getPurpose());

        System.out.println("\n  1. Approve  2. Reject  0. Skip");
        int action = ConsoleUI.readInt("Action");
        String comments = ConsoleUI.readString("Comments");
        User currentUser = bank.getAuthService().getCurrentUser();

        try {
            if (action == 1) {
                bank.getLoanService().approveLoan(loanId, currentUser.getUserId(), comments);
                ConsoleUI.printSuccess("Loan approved and funds disbursed.");
            } else if (action == 2) {
                bank.getLoanService().rejectLoan(loanId, currentUser.getUserId(), comments);
                ConsoleUI.printSuccess("Loan rejected.");
            }
        } catch (Exception e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void viewAllCustomers() {
        ConsoleUI.printHeader("ALL CUSTOMERS");
        Map<String, User> users = bank.getAuthService().getAllUsersById();
        System.out.printf("  %-12s %-20s %-25s %-10s\n", "ID", "Name", "Email", "Status");
        System.out.println("  " + "─".repeat(70));
        for (User u : users.values()) {
            if ("Customer".equals(u.getRole())) {
                System.out.printf("  %-12s %-20s %-25s %-10s\n",
                        u.getUserId(), u.getFullName(), u.getEmail(),
                        u.isAccountLocked() ? "LOCKED" : "Active");
            }
        }
        ConsoleUI.pressEnterToContinue();
    }

    private void searchCustomer() {
        String query = ConsoleUI.readString("Search by ID or email");
        User user = bank.getAuthService().getUserById(query);
        if (user == null) user = bank.getAuthService().getUserByEmail(query);
        if (user != null) {
            System.out.println(user.getProfileSummary());
        } else {
            ConsoleUI.printError("Customer not found.");
        }
        ConsoleUI.pressEnterToContinue();
    }

    private void viewCustomerAccounts() {
        String custId = ConsoleUI.readString("Customer ID");
        Customer customer = bank.getAuthService().getCustomer(custId);
        if (customer != null) {
            System.out.println(customer.getAccountsSummary());
            System.out.printf("  Total Balance: $%s\n", ValidationUtil.formatAmount(customer.getTotalBalance()));
        } else {
            ConsoleUI.printError("Customer not found.");
        }
        ConsoleUI.pressEnterToContinue();
    }

    private void monitorTransactions() {
        ConsoleUI.printHeader("TRANSACTION MONITOR");
        String accNum = ConsoleUI.readString("Account number to monitor");
        try {
            List<Transaction> recent = bank.getTransactionService()
                    .getTransactionHistory(accNum, 1, 20);
            for (Transaction txn : recent) {
                System.out.println(txn.getTransactionSummary());
            }
        } catch (InvalidAccountException e) {
            ConsoleUI.printError(e.getMessage());
        }
        ConsoleUI.pressEnterToContinue();
    }

    private void flagSuspiciousActivity() {
        String custId = ConsoleUI.readString("Customer ID");
        String accNum = ConsoleUI.readString("Account number");
        String description = ConsoleUI.readString("Description of suspicious activity");
        bank.getFraudDetectionService().flagSuspiciousActivity(
                custId, accNum, description, "STAFF_FLAGGED");
        ConsoleUI.printSuccess("Activity flagged for investigation.");
    }

    private void viewFraudCases() {
        System.out.println(bank.getFraudDetectionService().getFraudCaseDisplay());
        ConsoleUI.pressEnterToContinue();
    }

    // ==================== ADMIN DASHBOARD ====================

    private void handleAdminDashboard(Administrator admin) {
        boolean active = true;
        while (active) {
            System.out.println(admin.getDashboardMenu());
            int choice = ConsoleUI.readInt("Enter your choice");

            try {
                switch (choice) {
                    case 1: viewAllUsers(); break;
                    case 2: createStaffAccount(); break;
                    case 3: lockUnlockUser(); break;
                    case 4: resetUserPassword(); break;
                    case 5: System.out.println(bank.getReportingService().generateSystemStatistics()); ConsoleUI.pressEnterToContinue(); break;
                    case 6: manageExchangeRates(); break;
                    case 7:
                        System.out.println(bank.getFraudDetectionService().getFraudCaseDisplay());
                        updateFraudCase();
                        break;
                    case 8: freezeUnfreezeAccount(); break;
                    case 9: viewAllCustomers(); break;
                    case 10: System.out.println(bank.getReportingService().generateSystemStatistics()); ConsoleUI.pressEnterToContinue(); break;
                    case 11: System.out.println(bank.getReportingService().generateLoanPerformanceReport()); ConsoleUI.pressEnterToContinue(); break;
                    case 12: System.out.println(bank.getReportingService().generateAuditReport()); ConsoleUI.pressEnterToContinue(); break;
                    case 13: viewNotifications(admin); break;
                    case 14: System.out.println(admin.getProfileSummary()); ConsoleUI.pressEnterToContinue(); break;
                    case 15: changePassword(); break;
                    case 0:
                        bank.getAuthService().logout();
                        ConsoleUI.printInfo("Logged out successfully.");
                        active = false;
                        break;
                    default: ConsoleUI.printError("Invalid choice.");
                }
            } catch (Exception e) {
                ConsoleUI.printError(e.getMessage());
            }
        }
    }

    // ==================== ADMIN OPERATIONS ====================

    private void viewAllUsers() {
        ConsoleUI.printHeader("ALL USERS");
        Map<String, User> users = bank.getAuthService().getAllUsersById();
        System.out.printf("  %-12s %-20s %-25s %-12s %-8s\n",
                "ID", "Name", "Email", "Role", "Status");
        System.out.println("  " + "─".repeat(80));
        for (User u : users.values()) {
            System.out.printf("  %-12s %-20s %-25s %-12s %-8s\n",
                    u.getUserId(), u.getFullName(), u.getEmail(), u.getRole(),
                    u.isAccountLocked() ? "LOCKED" : "Active");
        }
        ConsoleUI.pressEnterToContinue();
    }

    private void createStaffAccount() {
        ConsoleUI.printHeader("CREATE STAFF ACCOUNT");
        try {
            String name = ConsoleUI.readString("Full Name");
            String email = ConsoleUI.readString("Email");
            String phone = ConsoleUI.readString("Phone");
            String address = ConsoleUI.readString("Address");
            String dob = ConsoleUI.readString("Date of Birth (yyyy-MM-dd)");
            String govtId = ConsoleUI.readString("Government ID");
            String password = ConsoleUI.readPassword("Password");
            String dept = ConsoleUI.readString("Department");
            String position = ConsoleUI.readString("Position");

            Staff staff = bank.getAuthService().createStaffAccount(
                    name, email, phone, address, dob, govtId, password, dept, position);
            ConsoleUI.printSuccess("Staff account created! ID: " + staff.getUserId());
        } catch (Exception e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void lockUnlockUser() {
        String userId = ConsoleUI.readString("User ID to lock/unlock");
        User user = bank.getAuthService().getUserById(userId);
        if (user == null) {
            ConsoleUI.printError("User not found.");
            return;
        }

        try {
            if (user.isAccountLocked()) {
                bank.getAuthService().unlockUser(userId);
                ConsoleUI.printSuccess("Account unlocked.");
            } else {
                bank.getAuthService().lockUser(userId);
                ConsoleUI.printSuccess("Account locked.");
            }
        } catch (UnauthorizedAccessException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void resetUserPassword() {
        String userId = ConsoleUI.readString("User ID");
        String newPwd = ConsoleUI.readPassword("New password");
        try {
            bank.getAuthService().resetUserPassword(userId, newPwd);
            ConsoleUI.printSuccess("Password reset successfully.");
        } catch (UnauthorizedAccessException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void manageExchangeRates() {
        System.out.println(bank.getCurrencyService().getExchangeRateDisplay());
        if (ConsoleUI.confirm("Update a rate?")) {
            String currency = ConsoleUI.readString("Currency code").toUpperCase();
            double rate = ConsoleUI.readDouble("New rate (relative to USD)");
            bank.getCurrencyService().updateExchangeRate(currency, rate);
            ConsoleUI.printSuccess("Exchange rate updated.");
        }
    }

    private void updateFraudCase() {
        String caseId = ConsoleUI.readString("Case ID to update (or Enter to skip)");
        if (!caseId.isEmpty()) {
            System.out.println("  Status options: Under Investigation, False Positive, Confirmed Fraud");
            String status = ConsoleUI.readString("New status");
            String notes = ConsoleUI.readString("Notes");
            bank.getFraudDetectionService().updateCase(caseId, status, notes);
            ConsoleUI.printSuccess("Case updated.");
        }
    }

    private void freezeUnfreezeAccount() {
        String accNum = ConsoleUI.readString("Account number");
        try {
            Account account = bank.getAccountService().getAccount(accNum);
            if ("Suspended".equals(account.getStatus())) {
                bank.getAccountService().reactivateAccount(accNum);
                ConsoleUI.printSuccess("Account reactivated.");
            } else {
                bank.getAccountService().suspendAccount(accNum);
                ConsoleUI.printSuccess("Account suspended/frozen.");
            }
        } catch (InvalidAccountException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }
}
