package com.rajarata.bank.exceptions;

/**
 * Custom exception thrown when an account is locked due to
 * excessive failed login attempts or administrative action.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class AccountLockedException extends Exception {

    /** The username of the locked account */
    private final String username;

    /**
     * Constructs a new AccountLockedException.
     * @param message Human-readable error message
     * @param username The locked account's username
     */
    public AccountLockedException(String message, String username) {
        super(message);
        this.username = username;
    }

    /**
     * Simple constructor with just a message.
     * @param message Human-readable error message
     */
    public AccountLockedException(String message) {
        super(message);
        this.username = "Unknown";
    }

    /** @return The username of the locked account */
    public String getUsername() { return username; }
}

