package com.rajarata.bank.models.transaction;

/**
 * Enumeration of all supported transaction types in the banking system.
 * 
 * OOP Concept: Abstraction - Using enums to represent a fixed set of transaction
 * categories, providing type safety and preventing invalid transaction types.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public enum TransactionType {
    /** Deposit funds into an account */
    DEPOSIT("Deposit"),
    /** Withdraw funds from an account */
    WITHDRAWAL("Withdrawal"),
    /** Transfer funds between accounts */
    TRANSFER("Transfer"),
    /** Balance inquiry operation */
    BALANCE_INQUIRY("Balance Inquiry"),
    /** Bill payment transaction */
    BILL_PAYMENT("Bill Payment"),
    /** Loan disbursement to customer account */
    LOAN_DISBURSEMENT("Loan Disbursement"),
    /** Loan repayment from customer */
    LOAN_REPAYMENT("Loan Repayment"),
    /** Interest credit to account */
    INTEREST_CREDIT("Interest Credit"),
    /** Penalty charge on account */
    PENALTY_CHARGE("Penalty Charge"),
    /** Currency exchange operation */
    CURRENCY_EXCHANGE("Currency Exchange");

    /** Human-readable display name for the transaction type */
    private final String displayName;

    /**
     * Constructor for TransactionType enum.
     * @param displayName The user-friendly name of this transaction type
     */
    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the display name of this transaction type.
     * @return Human-readable transaction type name
     */
    public String getDisplayName() {
        return displayName;
    }
}
