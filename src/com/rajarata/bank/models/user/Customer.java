package com.rajarata.bank.models.user;

import com.rajarata.bank.models.account.Account;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bank customer who can hold accounts, make transactions,
 * apply for loans, and pay bills.
 * 
 * OOP Concept: Inheritance - Extends abstract User class, inheriting common
 * properties and implementing abstract methods getRole() and getDashboardMenu().
 * 
 * OOP Concept: Composition - Customer HAS-A list of Accounts. The accounts
 * are composed within the Customer and their lifecycle is managed here.
 * 
 * OOP Concept: Polymorphism - Overrides getRole() and getDashboardMenu()
 * to provide customer-specific behavior.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class Customer extends User {

    // ==================== PRIVATE FIELDS ====================

    /** Unique customer ID (format: CUST-XXXX) */
    private String customerId;
    /** List of bank accounts owned by this customer */
    private List<Account> accounts;                                // OOP: Composition
    /** The primary/default account number for this customer */
    private String primaryAccountNumber;
    /** Simulated credit score (300-850) */
    private int creditScore;
    /** Employment status for loan eligibility */
    private String employmentStatus;
    /** Monthly income for loan assessment */
    private double monthlyIncome;

    /** Static counter for generating unique customer IDs */
    private static int customerCounter = 1000;                     // OOP: Static Members

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor.
     */
    public Customer() {
        super();
        this.accounts = new ArrayList<>();
        this.creditScore = generateCreditScore();
    }

    /**
     * Parameterized constructor for creating a new customer.
     * Automatically generates a unique customer ID.
     * 
     * @param fullName Customer's full name
     * @param email Customer's email (used as login username)
     * @param phone Customer's phone number
     * @param address Customer's address
     * @param dateOfBirth Customer's date of birth
     * @param governmentId Government-issued ID
     * @param password Plain text password (will be hashed)
     */
    public Customer(String fullName, String email, String phone, String address,
                    String dateOfBirth, String governmentId, String password) {
        super(generateCustomerId(), fullName, email, phone, address, dateOfBirth, governmentId, password);
        this.customerId = getUserId();
        this.accounts = new ArrayList<>();
        this.creditScore = generateCreditScore();
        this.employmentStatus = "Not Specified";
        this.monthlyIncome = 0.0;
    }

    // ==================== ABSTRACT METHOD IMPLEMENTATIONS ====================

    /**
     * {@inheritDoc}
     * Returns "Customer" as the role for this user type.
     * 
     * OOP Concept: Polymorphism - Runtime polymorphism through method overriding.
     */
    @Override
    public String getRole() {
        return "Customer";
    }

    /**
     * {@inheritDoc}
     * Returns the customer-specific dashboard menu.
     * 
     * OOP Concept: Polymorphism - Each user type displays a different dashboard.
     */
    @Override
    public String getDashboardMenu() {
        int unread = getUnreadNotificationCount();
        String notifBadge = unread > 0 ? " (" + unread + " new)" : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append("║       CUSTOMER DASHBOARD                 ║\n");
        sb.append("║       Welcome, ").append(String.format("%-24s", getFullName())).append(" ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append("║  1. View My Accounts                     ║\n");
        sb.append("║  2. Open New Account                     ║\n");
        sb.append("║  3. Make a Deposit                       ║\n");
        sb.append("║  4. Make a Withdrawal                    ║\n");
        sb.append("║  5. Transfer Funds                       ║\n");
        sb.append("║  6. View Transaction History             ║\n");
        sb.append("║  7. Apply for Loan                       ║\n");
        sb.append("║  8. View Loan Status                     ║\n");
        sb.append("║  9. Make Loan Payment                    ║\n");
        sb.append("║ 10. Pay Bills                            ║\n");
        sb.append("║ 11. Manage Payees                        ║\n");
        sb.append("║ 12. Currency Exchange                    ║\n");
        sb.append("║ 13. Generate Statement                   ║\n");
        sb.append("║ 14. Notifications").append(String.format("%-22s", notifBadge)).append(" ║\n");
        sb.append("║ 15. My Profile                           ║\n");
        sb.append("║ 16. Change Password                      ║\n");
        sb.append("║ 17. Interest Simulator                   ║\n");
        sb.append("║  0. Logout                               ║\n");
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    // ==================== ACCOUNT MANAGEMENT ====================
    // OOP Concept: Composition - Managing the lifecycle of composed Account objects

    /**
     * Adds an account to this customer's account list.
     * @param account The account to add
     */
    public void addAccount(Account account) {
        if (accounts == null) {
            accounts = new ArrayList<>();
        }
        accounts.add(account);
        // Set as primary if it's the first account
        if (accounts.size() == 1) {
            primaryAccountNumber = account.getAccountNumber();
        }
    }

    /**
     * Removes an account from this customer's account list.
     * @param accountNumber The account number to remove
     * @return true if the account was found and removed
     */
    public boolean removeAccount(String accountNumber) {
        boolean removed = accounts.removeIf(a -> a.getAccountNumber().equals(accountNumber));
        // If primary account was removed, reassign
        if (removed && accountNumber.equals(primaryAccountNumber) && !accounts.isEmpty()) {
            primaryAccountNumber = accounts.get(0).getAccountNumber();
        }
        return removed;
    }

    /**
     * Finds an account by account number.
     * @param accountNumber The account number to search for
     * @return The Account object, or null if not found
     */
    public Account getAccountByNumber(String accountNumber) {
        if (accounts == null) return null;
        for (Account account : accounts) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Gets the total number of accounts owned by this customer.
     * @return Number of accounts
     */
    public int getAccountCount() {
        return accounts != null ? accounts.size() : 0;
    }

    /**
     * Gets total balances grouped by currency for all active accounts.
     * @return Map of currency code to total balance in that currency
     */
    public java.util.Map<String, Double> getBalanceByCurrency() {
        java.util.Map<String, Double> balances = new java.util.LinkedHashMap<>();
        if (accounts == null) return balances;
        for (Account acc : accounts) {
            if ("Active".equals(acc.getStatus())) {
                balances.merge(acc.getCurrency(), acc.getBalance(), Double::sum);
            }
        }
        return balances;
    }

    /**
     * Gets a formatted string showing balance per currency.
     * @return e.g. "LKR 25,000.00 | USD 1,500.00"
     */
    public String getFormattedBalanceSummary() {
        java.util.Map<String, Double> balances = getBalanceByCurrency();
        if (balances.isEmpty()) return "No active accounts";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (java.util.Map.Entry<String, Double> entry : balances.entrySet()) {
            if (!first) sb.append(" | ");
            sb.append(entry.getKey()).append(" ").append(String.format("%,.2f", entry.getValue()));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Gets the total balance across all active accounts (same-currency sum only for LKR).
     * For backward compatibility — returns the LKR balance or total if single currency.
     * @return Combined balance of LKR accounts, or all if single currency
     */
    public double getTotalBalance() {
        if (accounts == null) return 0.0;
        java.util.Map<String, Double> balances = getBalanceByCurrency();
        if (balances.size() <= 1) {
            return balances.values().stream().mapToDouble(Double::doubleValue).sum();
        }
        // If multi-currency, return LKR balance (default currency)
        return balances.getOrDefault("LKR", 0.0);
    }

    /**
     * Displays a summary of all accounts.
     * @return Formatted string listing all accounts
     */
    public String getAccountsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n┌──────────────────┬──────────────────┬──────┬────────────────┬──────────┐\n");
        sb.append(String.format("│ %-16s │ %-16s │ %-4s │ %-14s │ %-8s │\n",
                "Account Number", "Type", "Cur.", "Balance", "Status"));
        sb.append("├──────────────────┼──────────────────┼──────┼────────────────┼──────────┤\n");

        if (accounts != null && !accounts.isEmpty()) {
            for (Account acc : accounts) {
                String primary = acc.getAccountNumber().equals(primaryAccountNumber) ? " *" : "";
                sb.append(String.format("│ %-16s │ %-16s │ %-4s │ %,14.2f │ %-8s │\n",
                        acc.getAccountNumber() + primary,
                        acc.getAccountType(),
                        acc.getCurrency(),
                        acc.getBalance(),
                        acc.getStatus()));
            }
            // Per-currency subtotals
            sb.append("├──────────────────┴──────────────────┴──────┴────────────────┴──────────┤\n");
            java.util.Map<String, Double> totals = getBalanceByCurrency();
            for (java.util.Map.Entry<String, Double> entry : totals.entrySet()) {
                sb.append(String.format("│   Total %-4s Balance: %,47.2f │\n", entry.getKey(), entry.getValue()));
            }
        } else {
            sb.append("│                       No accounts found                              │\n");
        }

        sb.append("└──────────────────────────────────────────────────────────────────────┘\n");
        sb.append("  * = Primary Account\n");
        return sb.toString();
    }

    // ==================== STATIC METHODS ====================

    /**
     * Generates a unique customer ID with format CUST-XXXX.
     * Uses a static counter to ensure uniqueness.
     * 
     * OOP Concept: Static Members - Static counter for ID generation.
     * 
     * @return A unique customer ID string
     */
    private static synchronized String generateCustomerId() {
        customerCounter++;
        return String.format("CUST-%04d", customerCounter);
    }

    /**
     * Generates a simulated credit score between 300 and 850.
     * Uses random generation for simulation purposes.
     * 
     * @return Simulated credit score
     */
    private int generateCreditScore() {
        return 300 + (int) (Math.random() * 551); // 300-850
    }

    /**
     * Sets the customer counter to a specified value.
     * Used when loading data to maintain ID sequence.
     * @param counter The counter value to set
     */
    public static void setCustomerCounter(int counter) {
        customerCounter = counter;
    }

    /**
     * Gets the current customer counter value.
     * @return Current counter value
     */
    public static int getCustomerCounter() {
        return customerCounter;
    }

    // ==================== GETTERS AND SETTERS ====================

    /** @return The customer's unique ID */
    public String getCustomerId() { return customerId; }
    /** @param customerId The customer ID to set */
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    /** @return List of customer's accounts */
    public List<Account> getAccounts() { return accounts; }
    /** @param accounts The accounts list to set */
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }

    /** @return The primary account number */
    public String getPrimaryAccountNumber() { return primaryAccountNumber; }
    /** @param primaryAccountNumber The primary account number to set */
    public void setPrimaryAccountNumber(String primaryAccountNumber) { this.primaryAccountNumber = primaryAccountNumber; }

    /** @return The customer's credit score */
    public int getCreditScore() { return creditScore; }
    /** @param creditScore The credit score to set */
    public void setCreditScore(int creditScore) { this.creditScore = creditScore; }

    /** @return The customer's employment status */
    public String getEmploymentStatus() { return employmentStatus; }
    /** @param employmentStatus The employment status to set */
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }

    /** @return The customer's monthly income */
    public double getMonthlyIncome() { return monthlyIncome; }
    /** @param monthlyIncome The monthly income to set */
    public void setMonthlyIncome(double monthlyIncome) { this.monthlyIncome = monthlyIncome; }
}
