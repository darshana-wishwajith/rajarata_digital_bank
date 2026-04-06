package com.rajarata.bank.models.transaction;

/**
 * Enumeration representing the lifecycle status of a transaction.
 * 
 * OOP Concept: Abstraction - Encapsulating transaction states as type-safe enum values
 * rather than using raw strings or integers, preventing invalid state assignments.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public enum TransactionStatus {
    /** Transaction is awaiting processing */
    PENDING("Pending"),
    /** Transaction has been successfully completed */
    COMPLETED("Completed"),
    /** Transaction has failed due to validation or processing error */
    FAILED("Failed"),
    /** Transaction has been reversed/cancelled */
    REVERSED("Reversed");

    /** Human-readable display name for the status */
    private final String displayName;

    /**
     * Constructor for TransactionStatus enum.
     * @param displayName The user-friendly name of this status
     */
    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the display name of this transaction status.
     * @return Human-readable status name
     */
    public String getDisplayName() {
        return displayName;
    }
}

