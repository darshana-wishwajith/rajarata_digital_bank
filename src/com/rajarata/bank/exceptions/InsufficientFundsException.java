package com.rajarata.bank.exceptions;

/**
 * Custom exception thrown when an account has insufficient funds
 * for a requested withdrawal, transfer, or payment operation.
 * 
 * OOP Concept: Exception Handling - Custom exception class extending Exception
 * to provide domain-specific error handling. This allows calling code to catch
 * and handle insufficient funds scenarios differently from other errors.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class InsufficientFundsException extends Exception {

    /** The account number that has insufficient funds */
    private final String accountNumber;
    /** The amount that was requested */
    private final double requestedAmount;
    /** The current available balance */
    private final double availableBalance;

    /**
     * Constructs a new InsufficientFundsException with detailed information.
     * 
     * @param message Human-readable error message
     * @param accountNumber The account number with insufficient funds
     * @param requestedAmount The amount that was attempted
     * @param availableBalance The actual available balance
     */
    public InsufficientFundsException(String message, String accountNumber, 
                                       double requestedAmount, double availableBalance) {
        super(message);
        this.accountNumber = accountNumber;
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }

    /**
     * Simple constructor with just a message.
     * @param message Human-readable error message
     */
    public InsufficientFundsException(String message) {
        super(message);
        this.accountNumber = "Unknown";
        this.requestedAmount = 0;
        this.availableBalance = 0;
    }

    /** @return The account number that has insufficient funds */
    public String getAccountNumber() { return accountNumber; }

    /** @return The amount that was requested */
    public double getRequestedAmount() { return requestedAmount; }

    /** @return The current available balance */
    public double getAvailableBalance() { return availableBalance; }
}
