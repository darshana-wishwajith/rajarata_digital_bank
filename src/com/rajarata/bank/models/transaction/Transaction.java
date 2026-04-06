package com.rajarata.bank.models.transaction;

import com.rajarata.bank.utils.CurrencyUtil;
import com.rajarata.bank.utils.DateUtil;
import com.rajarata.bank.utils.ValidationUtil;

/**
 * Represents a single financial transaction in the banking system.
 * Each transaction is immutable once created (status can be updated).
 * 
 * OOP Concept: Encapsulation - All transaction data is private with controlled access.
 * Transaction amounts cannot be modified after creation.
 * 
 * OOP Concept: Polymorphism (Method Overloading) - Multiple constructors accept
 * different parameter combinations for different transaction types.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class Transaction {

    // ==================== PRIVATE FIELDS ====================

    /** Unique transaction ID (format: TXN-YYYYMMDD-XXXXXX) */
    private final String transactionId;
    /** Date and time of the transaction */
    private final String timestamp;
    /** Type of transaction */
    private final TransactionType type;
    /** Source account number */
    private final String sourceAccount;
    /** Destination account number (for transfers) */
    private final String destinationAccount;
    /** Transaction amount */
    private final double amount;
    /** Currency of the transaction */
    private final String currency;
    /** Current status of the transaction */
    private TransactionStatus status;
    /** Description/memo for the transaction */
    private final String description;
    /** Balance after this transaction */
    private double balanceAfter;
    /** Source currency amount (for currency exchange) */
    private double sourceAmount;
    /** Target currency amount (for currency exchange) */
    private double targetAmount;
    /** Source currency (for currency exchange) */
    private String sourceCurrency;
    /** Target currency (for currency exchange) */
    private String targetCurrency;

    /** Static counter for generating unique transaction IDs */
    private static int transactionCounter = 0;

    // ==================== CONSTRUCTORS ====================
    // OOP Concept: Polymorphism (Method Overloading) - Multiple constructor signatures

    /**
     * Full constructor for a complete transaction record.
     * 
     * @param type The type of transaction
     * @param sourceAccount Source account number
     * @param destinationAccount Destination account number (null for non-transfer)
     * @param amount Transaction amount
     * @param currency Transaction currency
     * @param description Description/memo
     */
    public Transaction(TransactionType type, String sourceAccount, String destinationAccount,
                       double amount, String currency, String description) {
        this.transactionId = generateTransactionId();
        this.timestamp = DateUtil.getCurrentDateTime();
        this.type = type;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = TransactionStatus.PENDING;
    }

    /**
     * Simplified constructor for single-account transactions (deposit/withdrawal).
     * OOP Concept: Method Overloading - Fewer parameters for simpler transactions.
     * 
     * @param type The type of transaction
     * @param accountNumber Account number
     * @param amount Transaction amount
     * @param description Description/memo
     */
    public Transaction(TransactionType type, String accountNumber, double amount, String description) {
        this(type, accountNumber, null, amount, "LKR", description);
    }

    /**
     * Constructor for a transaction with explicit currency.
     * 
     * @param type Transaction type
     * @param accountNumber Account number
     * @param amount Amount
     * @param currency Currency
     * @param description Memo
     */
    public Transaction(TransactionType type, String accountNumber, double amount, String currency, String description) {
        this(type, accountNumber, null, amount, currency, description);
    }

    /**
     * Constructor for loading transaction data from file.
     * OOP Concept: Method Overloading - Constructor for data loading with all fields.
     * 
     * @param transactionId Existing transaction ID
     * @param timestamp Existing timestamp
     * @param type Transaction type
     * @param sourceAccount Source account
     * @param destinationAccount Destination account
     * @param amount Transaction amount
     * @param currency Currency
     * @param status Transaction status
     * @param description Description
     * @param balanceAfter Balance after transaction
     */
    public Transaction(String transactionId, String timestamp, TransactionType type,
                       String sourceAccount, String destinationAccount, double amount,
                       String currency, TransactionStatus status, String description,
                       double balanceAfter) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.type = type;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.balanceAfter = balanceAfter;
    }

    // ==================== TRANSACTION METHODS ====================

    /**
     * Marks the transaction as completed.
     * @param resultingBalance The balance after the transaction
     */
    public void complete(double resultingBalance) {
        this.status = TransactionStatus.COMPLETED;
        this.balanceAfter = resultingBalance;
    }

    /**
     * Marks the transaction as failed.
     * @param reason The reason for failure (appended to description)
     */
    public void fail(String reason) {
        this.status = TransactionStatus.FAILED;
    }

    /**
     * Reverses a completed transaction.
     */
    public void reverse() {
        this.status = TransactionStatus.REVERSED;
    }

    /**
     * Sets currency exchange details for multi-currency transactions.
     * 
     * @param sourceAmount Amount in source currency
     * @param targetAmount Amount in target currency
     * @param sourceCurrency Source currency code
     * @param targetCurrency Target currency code
     */
    public void setCurrencyExchangeDetails(double sourceAmount, double targetAmount,
                                           String sourceCurrency, String targetCurrency) {
        this.sourceAmount = sourceAmount;
        this.targetAmount = targetAmount;
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
    }

    /**
     * Gets a one-line summary of the transaction for display.
     * @return Formatted transaction summary
     */
    public String getTransactionSummary() {
        String sym = CurrencyUtil.getCurrencySymbol(currency);
        return String.format("  %s | %-12s | %s%12s | %-10s | %s",
                timestamp.substring(0, 10),
                type.getDisplayName(),
                sym,
                ValidationUtil.formatAmount(amount),
                status.getDisplayName(),
                description != null ? description : "");
    }

    /**
     * Gets a detailed view of the transaction.
     * @return Multi-line formatted transaction details
     */
    public String getDetailedView() {
        StringBuilder sb = new StringBuilder();
        String sym = CurrencyUtil.getCurrencySymbol(currency);
        sb.append("\n┌─── Transaction Details ────────────────┐\n");
        sb.append(String.format("│ Transaction ID : %-21s │\n", transactionId));
        sb.append(String.format("│ Date/Time      : %-21s │\n", timestamp));
        sb.append(String.format("│ Type           : %-21s │\n", type.getDisplayName()));
        sb.append(String.format("│ Amount         : %s%-19s │\n", sym, ValidationUtil.formatAmount(amount)));
        sb.append(String.format("│ Currency       : %-21s │\n", currency));
        sb.append(String.format("│ Status         : %-21s │\n", status.getDisplayName()));
        sb.append(String.format("│ Source Account : %-21s │\n", sourceAccount != null ? sourceAccount : "N/A"));
        sb.append(String.format("│ Dest Account   : %-21s │\n", destinationAccount != null ? destinationAccount : "N/A"));
        sb.append(String.format("│ Balance After  : %s%-19s │\n", sym, ValidationUtil.formatAmount(balanceAfter)));
        if (description != null && !description.isEmpty()) {
            sb.append(String.format("│ Description    : %-21s │\n", description));
        }
        if (sourceCurrency != null && targetCurrency != null) {
            String sSym = CurrencyUtil.getCurrencySymbol(sourceCurrency);
            String tSym = CurrencyUtil.getCurrencySymbol(targetCurrency);
            sb.append(String.format("│ Exchange       : %s → %-16s │\n", sourceCurrency, targetCurrency));
            sb.append(String.format("│ Source Amount  : %s%-19s │\n", sSym, ValidationUtil.formatAmount(sourceAmount)));
            sb.append(String.format("│ Target Amount  : %s%-19s │\n", tSym, ValidationUtil.formatAmount(targetAmount)));
        }
        sb.append("└────────────────────────────────────────┘\n");
        return sb.toString();
    }

    // ==================== SERIALIZATION ====================

    /**
     * Serializes the transaction to a delimited string for file storage.
     * @return Pipe-delimited string representation
     */
    public String toFileString() {
        return String.join("|",
                transactionId,
                timestamp,
                type.name(),
                sourceAccount != null ? sourceAccount : "",
                destinationAccount != null ? destinationAccount : "",
                String.valueOf(amount),
                currency,
                status.name(),
                description != null ? description : "",
                String.valueOf(balanceAfter),
                String.valueOf(sourceAmount),
                String.valueOf(targetAmount),
                sourceCurrency != null ? sourceCurrency : "",
                targetCurrency != null ? targetCurrency : ""
        );
    }

    // ==================== STATIC METHODS ====================

    /**
     * Generates a unique transaction ID.
     * Format: TXN-YYYYMMDD-XXXXXX
     * @return Unique transaction ID
     */
    private static synchronized String generateTransactionId() {
        transactionCounter++;
        return String.format("TXN-%s-%06d", DateUtil.getTransactionDate(), transactionCounter);
    }

    /**
     * Sets the transaction counter (for data loading).
     * @param counter The counter value to set
     */
    public static void setTransactionCounter(int counter) {
        transactionCounter = counter;
    }

    // ==================== GETTERS ====================
    // Note: No setters for most fields - transactions are immutable once created

    /** @return The unique transaction ID */
    public String getTransactionId() { return transactionId; }

    /** @return The transaction timestamp */
    public String getTimestamp() { return timestamp; }

    /** @return The transaction type */
    public TransactionType getType() { return type; }

    /** @return The source account number */
    public String getSourceAccount() { return sourceAccount; }

    /** @return The destination account number */
    public String getDestinationAccount() { return destinationAccount; }

    /** @return The transaction amount */
    public double getAmount() { return amount; }

    /** @return The transaction currency */
    public String getCurrency() { return currency; }

    /** @return The current transaction status */
    public TransactionStatus getStatus() { return status; }

    /** @return The transaction description */
    public String getDescription() { return description; }

    /** @return The balance after this transaction */
    public double getBalanceAfter() { return balanceAfter; }

    /** @param balanceAfter The balance after to set */
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }
}

