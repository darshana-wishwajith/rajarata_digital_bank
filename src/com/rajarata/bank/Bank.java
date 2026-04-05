package com.rajarata.bank;

import com.rajarata.bank.services.*;
import com.rajarata.bank.utils.FileHandler;

/**
 * Singleton class representing the Rajarata Digital Bank.
 * Centralizes access to all services and manages application lifecycle.
 * 
 * OOP Concept: Singleton Design Pattern - Only one instance of Bank exists.
 * This ensures a single point of access for all banking services and
 * prevents inconsistent state from multiple bank instances.
 * 
 * OOP Concept: Aggregation - Bank aggregates all services, managing their
 * lifecycle and providing coordinated access.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class Bank {

    /** The single instance of the Bank */
    private static Bank instance;

    /** Bank name constant */
    public static final String BANK_NAME = "Rajarata Digital Bank";
    /** Bank code constant */
    public static final String BANK_CODE = "RDB";
    /** Bank version */
    public static final String VERSION = "1.0.0";

    // ==================== SERVICE REFERENCES ====================
    private final AuthenticationService authService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final LoanService loanService;
    private final BillPaymentService billPaymentService;
    private final NotificationService notificationService;
    private final CurrencyService currencyService;
    private final FraudDetectionService fraudDetectionService;
    private final ReportingService reportingService;

    /**
     * Private constructor - Singleton pattern prevents external instantiation.
     */
    private Bank() {
        // Initialize services in dependency order
        this.authService = new AuthenticationService();
        this.accountService = new AccountService(authService);
        this.transactionService = new TransactionService(accountService, authService);
        this.loanService = new LoanService(accountService, authService);
        this.notificationService = new NotificationService(authService);
        this.billPaymentService = new BillPaymentService(transactionService, accountService);
        this.currencyService = new CurrencyService();
        this.fraudDetectionService = new FraudDetectionService(accountService);
        this.reportingService = new ReportingService(accountService, authService, loanService);

        // Wire notification service (avoids circular dependency)
        transactionService.setNotificationService(notificationService);
        transactionService.setCurrencyService(currencyService);
        loanService.setNotificationService(notificationService);
        billPaymentService.setNotificationService(notificationService);
        fraudDetectionService.setNotificationService(notificationService);

        // Wire auth service to notification + fraud detection for login-lockout alerts
        authService.setNotificationService(notificationService);
        authService.setFraudDetectionService(fraudDetectionService);
    }

    /**
     * Gets the singleton Bank instance. Creates it on first access.
     * OOP Concept: Singleton Pattern - Thread-safe lazy initialization.
     * 
     * @return The single Bank instance
     */
    public static synchronized Bank getInstance() {
        if (instance == null) {
            instance = new Bank();
        }
        return instance;
    }

    /**
     * Initializes the bank: data files, loads existing data, creates default admin.
     */
    public void initialize() {
        System.out.println("\n  Initializing " + BANK_NAME + " v" + VERSION + "...");

        // Initialize data files
        FileHandler.initializeDataFiles();
        System.out.println("  ✓ Data files initialized");

        // Load existing data
        authService.loadUsers();
        System.out.println("  ✓ Users loaded (" + authService.getUserCount() + " users)");

        accountService.loadAccounts();
        System.out.println("  ✓ Accounts loaded (" + accountService.getAccountCount() + " accounts)");

        loanService.loadLoans();
        System.out.println("  ✓ Loans loaded (" + loanService.getAllLoans().size() + " loans)");

        notificationService.loadNotifications();
        System.out.println("  ✓ Notifications loaded");

        billPaymentService.loadPayees();
        System.out.println("  ✓ Payees loaded");

        fraudDetectionService.loadFraudCases();
        System.out.println("  ✓ Fraud cases loaded");

        // Create default admin if no users exist
        authService.createDefaultAdmin();

        System.out.println("  ✓ Bank initialization complete!");

        FileHandler.logAudit("SYSTEM_START", BANK_NAME + " v" + VERSION + " started");

        // Check for upcoming loan payment deadlines and send reminders
        loanService.checkUpcomingLoanPayments();
        System.out.println("  ✓ Loan payment reminders checked\n");
    }

    /**
     * Shuts down the bank, saving all data.
     */
    public void shutdown() {
        System.out.println("\n  Shutting down " + BANK_NAME + "...");
        authService.saveAllUsers();
        accountService.saveAllAccounts();
        FileHandler.logAudit("SYSTEM_SHUTDOWN", BANK_NAME + " shutdown");
        System.out.println("  ✓ All data saved. Goodbye!\n");
    }

    /**
     * Gets the bank welcome banner.
     * @return Formatted welcome banner string
     */
    public String getWelcomeBanner() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║                                                              ║\n");
        sb.append("║    ██████╗  ██████╗ ██████╗     ██████╗  █████╗ ███╗   ██╗   ║\n");
        sb.append("║    ██╔══██╗██╔═══██╗██╔══██╗    ██╔══██╗██╔══██╗████╗  ██║   ║\n");
        sb.append("║    ██████╔╝██║   ██║██████╔╝    ██████╔╝███████║██╔██╗ ██║   ║\n");
        sb.append("║    ██╔══██╗██║   ██║██╔══██╗    ██╔══██╗██╔══██║██║╚██╗██║   ║\n");
        sb.append("║    ██║  ██║╚██████╔╝██████╔╝    ██████╔╝██║  ██║██║ ╚████║   ║\n");
        sb.append("║    ╚═╝  ╚═╝ ╚═════╝ ╚═════╝     ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝   ║\n");
        sb.append("║                                                              ║\n");
        sb.append("║            RAJARATA DIGITAL BANK                             ║\n");
        sb.append("║         Your Trusted Banking Partner                         ║\n");
        sb.append("║                 Version " + VERSION + "                                ║\n");
        sb.append("║                                                              ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    // ==================== SERVICE GETTERS ====================

    public AuthenticationService getAuthService() { return authService; }
    public AccountService getAccountService() { return accountService; }
    public TransactionService getTransactionService() { return transactionService; }
    public LoanService getLoanService() { return loanService; }
    public BillPaymentService getBillPaymentService() { return billPaymentService; }
    public NotificationService getNotificationService() { return notificationService; }
    public CurrencyService getCurrencyService() { return currencyService; }
    public FraudDetectionService getFraudDetectionService() { return fraudDetectionService; }
    public ReportingService getReportingService() { return reportingService; }
}
