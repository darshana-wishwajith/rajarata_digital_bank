package com.rajarata.bank.models.account;

import com.rajarata.bank.interfaces.InterestBearing;
import com.rajarata.bank.interfaces.Reportable;
import com.rajarata.bank.interfaces.Transactable;
import com.rajarata.bank.models.transaction.Transaction;
import com.rajarata.bank.exceptions.InsufficientFundsException;
import com.rajarata.bank.exceptions.InvalidAccountException;
import com.rajarata.bank.utils.DateUtil;
import com.rajarata.bank.utils.ValidationUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class representing a bank account.
 * Defines the common structure and behavior shared by all account types.
 * 
 * OOP Concept: Abstraction - This abstract class defines a contract that all
 * concrete account types must fulfil. The method calculateInterest() is abstract,
 * requiring each subclass to define its own interest calculation strategy.
 * 
 * OOP Concept: Inheritance - SavingsAccount, CheckingAccount, StudentAccount,
 * and FixedDepositAccount all extend this class, inheriting common properties.
 * 
 * OOP Concept: Composition - Each Account HAS-A list of Transactions.
 * The transaction lifecycle is managed within the account.
 * 
 * OOP Concept: Encapsulation - All fields are private with controlled access.
 * Balance can only be modified through deposit/withdraw methods.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public abstract class Account implements Transactable, InterestBearing, Reportable {

    // ==================== PRIVATE FIELDS ====================
    // OOP Concept: Encapsulation - All fields are private

    /** Unique account number (format: RDBXXXX-YYYY) */
    private String accountNumber;
    /** Customer ID who owns this account */
    private String customerId;
    /** Current account balance */
    private double balance;
    /** Account creation date */
    private String openDate;
    /** Account status: Active, Suspended, Closed */
    private String status;
    /** Currency of the account */
    private String currency;
    /** List of transactions on this account */
    private LinkedList<Transaction> transactionHistory;             // OOP: Composition
    /** Number of withdrawals this month */
    private int monthlyWithdrawalCount;
    /** Date of last interest calculation */
    private String lastInterestDate;
    /** Total interest earned on this account */
    private double totalInterestEarned;

    /** Static counter for sequential account numbering */
    private static int accountSequence = 0;                         // OOP: Static Members

    // ==================== CONSTANTS ====================
    // OOP Concept: Final Keyword - Constants for account rules

    /** Minimum deposit amount */
    public static final double MIN_DEPOSIT = 10.0;
    /** Default currency for new accounts */
    public static final String DEFAULT_CURRENCY = "LKR";

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor - initializes default values.
     * OOP Concept: Constructors - Default constructor for framework use.
     */
    protected Account() {
        this.transactionHistory = new LinkedList<>();
        this.status = "Active";
        this.currency = DEFAULT_CURRENCY;
        this.openDate = DateUtil.getCurrentDate();
        this.monthlyWithdrawalCount = 0;
        this.totalInterestEarned = 0.0;
    }

    /**
     * Parameterized constructor for creating a new account.
     * OOP Concept: Constructors - Parameterized constructor with validation.
     * 
     * @param customerId The ID of the customer who owns this account
     * @param initialDeposit The initial deposit amount
     * @param currency The currency for the account
     */
    protected Account(String customerId, double initialDeposit, String currency) {
        this();
        this.customerId = customerId;
        this.accountNumber = generateAccountNumber(customerId);
        this.balance = initialDeposit;
        this.currency = currency;
    }

    // ==================== ABSTRACT METHODS ====================
    // OOP Concept: Abstraction - Subclasses must implement these

    /**
     * Gets the type name of this account (Savings, Checking, etc.).
     * @return The account type as a string
     */
    public abstract String getAccountType();

    /**
     * Gets the minimum balance requirement for this account type.
     * @return The minimum balance, or 0 if none required
     */
    public abstract double getMinimumBalance();

    /**
     * Gets the monthly withdrawal limit for this account type.
     * @return Maximum withdrawals per month, or -1 for unlimited
     */
    public abstract int getMonthlyWithdrawalLimit();

    /**
     * Gets the maximum overdraft amount for this account type.
     * @return Maximum overdraft, or 0 if overdraft is not allowed
     */
    public abstract double getOverdraftLimit();

    // ==================== TRANSACTABLE INTERFACE IMPLEMENTATION ====================
    // OOP Concept: Polymorphism - Implementing the Transactable interface

    /**
     * {@inheritDoc}
     * Deposits funds into the account. Common implementation for all account types.
     */
    @Override
    public boolean deposit(double amount) throws InvalidAccountException {
        // Validate account status
        if (!"Active".equals(status)) {
            throw new InvalidAccountException(
                "Cannot deposit to " + status + " account", accountNumber);
        }
        // Validate amount
        if (amount < MIN_DEPOSIT) {
            throw new InvalidAccountException(
                "Minimum deposit amount is $" + ValidationUtil.formatAmount(MIN_DEPOSIT), accountNumber);
        }
        if (!ValidationUtil.isValidAmount(amount)) {
            throw new InvalidAccountException(
                "Invalid deposit amount", accountNumber);
        }

        this.balance += amount;
        return true;
    }

    /**
     * {@inheritDoc}
     * Withdraws funds from the account. Can be overridden by subclasses
     * for account-specific validation (e.g., withdrawal limits, overdraft).
     */
    @Override
    public boolean withdraw(double amount) throws InsufficientFundsException, InvalidAccountException {
        // Validate account status
        if (!"Active".equals(status)) {
            throw new InvalidAccountException(
                "Cannot withdraw from " + status + " account", accountNumber);
        }
        // Validate amount
        if (amount <= 0 || !ValidationUtil.isValidAmount(amount)) {
            throw new InvalidAccountException("Invalid withdrawal amount", accountNumber);
        }
        // Check withdrawal limit
        if (getMonthlyWithdrawalLimit() != -1 && monthlyWithdrawalCount >= getMonthlyWithdrawalLimit()) {
            throw new InvalidAccountException(
                "Monthly withdrawal limit (" + getMonthlyWithdrawalLimit() + ") exceeded", accountNumber);
        }
        // Check if withdrawal is allowed (can be overridden by subclasses)
        if (!canWithdraw(amount)) {
            throw new InsufficientFundsException(
                "Insufficient funds for withdrawal",
                accountNumber, amount, balance);
        }

        this.balance -= amount;
        this.monthlyWithdrawalCount++;
        return true;
    }

    /**
     * {@inheritDoc}
     * Returns the balance available for transactions.
     * Subclasses may override to include overdraft availability.
     */
    @Override
    public double getAvailableBalance() {
        return balance + getOverdraftLimit();
    }

    /**
     * {@inheritDoc}
     * Default implementation - checks against available balance.
     * Subclasses may override for additional checks.
     */
    @Override
    public boolean canWithdraw(double amount) {
        return amount <= getAvailableBalance() && amount > 0;
    }

    // ==================== REPORTABLE INTERFACE IMPLEMENTATION ====================

    /**
     * {@inheritDoc}
     * Generates a summary report for this account.
     */
    @Override
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n═══════════ ACCOUNT REPORT ═══════════\n");
        sb.append(String.format("Account Number : %s\n", accountNumber));
        sb.append(String.format("Account Type   : %s\n", getAccountType()));
        sb.append(String.format("Customer ID    : %s\n", customerId));
        sb.append(String.format("Status         : %s\n", status));
        sb.append(String.format("Balance        : $%s\n", ValidationUtil.formatAmount(balance)));
        sb.append(String.format("Currency       : %s\n", currency));
        sb.append(String.format("Interest Rate  : %.1f%%\n", getInterestRate() * 100));
        sb.append(String.format("Interest Earned: $%s\n", ValidationUtil.formatAmount(totalInterestEarned)));
        sb.append(String.format("Opened         : %s\n", openDate));
        sb.append(String.format("Transactions   : %d\n", transactionHistory.size()));
        sb.append("══════════════════════════════════════\n");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * Generates a detailed report for a date range.
     */
    @Override
    public String generateDetailedReport(String startDate, String endDate) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateReport());
        sb.append("\nTransactions from " + startDate + " to " + endDate + ":\n");
        sb.append("────────────────────────────────────────\n");

        List<Transaction> filtered = getTransactionsByDateRange(startDate, endDate);
        if (filtered.isEmpty()) {
            sb.append("  No transactions in this period.\n");
        } else {
            double totalDeposits = 0, totalWithdrawals = 0;
            for (Transaction txn : filtered) {
                sb.append(txn.getTransactionSummary()).append("\n");
                if (txn.getAmount() > 0 && "DEPOSIT".equals(txn.getType().name())) {
                    totalDeposits += txn.getAmount();
                } else {
                    totalWithdrawals += txn.getAmount();
                }
            }
            sb.append("────────────────────────────────────────\n");
            sb.append(String.format("Total Deposits    : $%s\n", ValidationUtil.formatAmount(totalDeposits)));
            sb.append(String.format("Total Withdrawals : $%s\n", ValidationUtil.formatAmount(totalWithdrawals)));
            sb.append(String.format("Transaction Count : %d\n", filtered.size()));
        }
        return sb.toString();
    }

    // ==================== TRANSACTION HISTORY MANAGEMENT ====================
    // OOP Concept: Composition - Managing Transaction objects within Account

    /**
     * Adds a transaction to the account's history.
     * @param transaction The transaction to add
     */
    public void addTransaction(Transaction transaction) {
        if (transactionHistory == null) {
            transactionHistory = new LinkedList<>();
        }
        transactionHistory.add(transaction);
    }

    /**
     * Gets all transactions for this account.
     * @return List of all transactions
     */
    public List<Transaction> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }

    /**
     * Gets transactions filtered by date range.
     * @param startDate Start date (yyyy-MM-dd)
     * @param endDate End date (yyyy-MM-dd)
     * @return Filtered list of transactions
     */
    public List<Transaction> getTransactionsByDateRange(String startDate, String endDate) {
        return transactionHistory.stream()
                .filter(t -> {
                    String txnDate = t.getTimestamp().substring(0, 10); // Extract date part
                    return DateUtil.isWithinRange(txnDate, startDate, endDate);
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets the last N transactions.
     * @param count Number of recent transactions to retrieve
     * @return List of recent transactions
     */
    public List<Transaction> getRecentTransactions(int count) {
        int size = transactionHistory.size();
        int fromIndex = Math.max(0, size - count);
        return new ArrayList<>(transactionHistory.subList(fromIndex, size));
    }

    /**
     * Resets the monthly withdrawal counter. Should be called at start of each month.
     */
    public void resetMonthlyWithdrawalCount() {
        this.monthlyWithdrawalCount = 0;
    }

    /**
     * Checks if minimum balance penalty needs to be applied.
     * @return true if balance is below minimum requirement
     */
    public boolean isBelowMinimumBalance() {
        return getMinimumBalance() > 0 && balance < getMinimumBalance();
    }

    // ==================== SERIALIZATION ====================

    /**
     * Serializes the account to a delimited string for file storage.
     * @return Pipe-delimited string representation
     */
    public String toFileString() {
        return String.join("|",
                accountNumber,
                customerId,
                getAccountType(),
                String.valueOf(balance),
                openDate != null ? openDate : "",
                status,
                currency,
                String.valueOf(monthlyWithdrawalCount),
                lastInterestDate != null ? lastInterestDate : "",
                String.valueOf(totalInterestEarned),
                getExtraFieldsForFile()
        );
    }

    /**
     * Template method for subclasses to add extra fields to file serialization.
     * @return Additional fields as a pipe-delimited string
     */
    protected String getExtraFieldsForFile() {
        return "";
    }

    // ==================== STATIC METHODS ====================

    /**
     * Generates a unique account number with format RDBXXXX-YYYY.
     * XXXX = customer ID digits, YYYY = sequential number.
     * 
     * @param customerId The customer ID for account association
     * @return A unique account number string
     */
    public static synchronized String generateAccountNumber(String customerId) {
        accountSequence++;
        // Extract numeric part from customer ID
        String custNum = customerId.replaceAll("[^0-9]", "");
        if (custNum.length() > 4) {
            custNum = custNum.substring(custNum.length() - 4);
        }
        return String.format("RDB%s-%04d", custNum, accountSequence);
    }

    /**
     * Sets the account sequence counter.
     * @param sequence The sequence value to set
     */
    public static void setAccountSequence(int sequence) {
        accountSequence = sequence;
    }

    /**
     * Gets the current account sequence.
     * @return Current sequence value
     */
    public static int getAccountSequence() {
        return accountSequence;
    }

    // ==================== GETTERS AND SETTERS ====================

    /** @return The unique account number */
    public String getAccountNumber() { return accountNumber; }
    /** @param accountNumber The account number to set */
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    /** @return The owning customer's ID */
    public String getCustomerId() { return customerId; }
    /** @param customerId The customer ID to set */
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    /** @return The current balance */
    public double getBalance() { return balance; }
    /** @param balance The balance to set (use only for data loading) */
    public void setBalance(double balance) { this.balance = balance; }

    /** @return The account opening date */
    public String getOpenDate() { return openDate; }
    /** @param openDate The open date to set */
    public void setOpenDate(String openDate) { this.openDate = openDate; }

    /** @return The account status */
    public String getStatus() { return status; }
    /** @param status The status to set */
    public void setStatus(String status) { this.status = status; }

    /** @return The account currency */
    public String getCurrency() { return currency; }
    /** @param currency The currency to set */
    public void setCurrency(String currency) { this.currency = currency; }

    /** @return The number of withdrawals this month */
    public int getMonthlyWithdrawalCount() { return monthlyWithdrawalCount; }
    /** @param count The withdrawal count to set */
    public void setMonthlyWithdrawalCount(int count) { this.monthlyWithdrawalCount = count; }

    /** @return The date of last interest calculation */
    public String getLastInterestDate() { return lastInterestDate; }
    /** @param date The last interest date to set */
    public void setLastInterestDate(String date) { this.lastInterestDate = date; }

    /** @return Total interest earned on this account */
    public double getTotalInterestEarned() { return totalInterestEarned; }
    /** @param amount The total interest earned to set */
    public void setTotalInterestEarned(double amount) { this.totalInterestEarned = amount; }

    /** @param history The transaction history to set */
    public void setTransactionHistory(LinkedList<Transaction> history) {
        this.transactionHistory = history;
    }

    /**
     * Adds interest amount to total earned and to balance.
     * @param interest The interest amount to add
     */
    protected void creditInterest(double interest) {
        this.balance += interest;
        this.totalInterestEarned += interest;
        this.lastInterestDate = DateUtil.getCurrentDate();
    }
}
