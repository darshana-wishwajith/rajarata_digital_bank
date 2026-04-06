package com.rajarata.bank.exceptions;

/**
 * Custom exception thrown when an invalid account operation is attempted,
 * such as accessing a non-existent, closed, or suspended account.
 * 
 * OOP Concept: Exception Handling - Domain-specific exception for account validation errors.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class InvalidAccountException extends Exception {

    /** The invalid account number that triggered this exception */
    private final String accountNumber;

    /**
     * Constructs a new InvalidAccountException.
     * @param message Human-readable error message
     * @param accountNumber The account number that is invalid
     */
    public InvalidAccountException(String message, String accountNumber) {
        super(message);
        this.accountNumber = accountNumber;
    }

    /**
     * Simple constructor with just a message.
     * @param message Human-readable error message
     */
    public InvalidAccountException(String message) {
        super(message);
        this.accountNumber = "Unknown";
    }

    /** @return The invalid account number */
    public String getAccountNumber() { return accountNumber; }
}

