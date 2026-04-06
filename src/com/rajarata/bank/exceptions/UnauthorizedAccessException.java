package com.rajarata.bank.exceptions;

/**
 * Custom exception thrown when a user attempts to perform an action
 * they are not authorized to execute based on their role.
 * 
 * OOP Concept: Exception Handling - Enforcing role-based access control
 * through exception handling mechanisms.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class UnauthorizedAccessException extends Exception {

    /** The username that attempted unauthorized access */
    private final String username;
    /** The action that was attempted */
    private final String attemptedAction;

    /**
     * Constructs a new UnauthorizedAccessException.
     * @param message Human-readable error message
     * @param username The user who attempted the action
     * @param attemptedAction The action that was denied
     */
    public UnauthorizedAccessException(String message, String username, String attemptedAction) {
        super(message);
        this.username = username;
        this.attemptedAction = attemptedAction;
    }

    /**
     * Simple constructor with just a message.
     * @param message Human-readable error message
     */
    public UnauthorizedAccessException(String message) {
        super(message);
        this.username = "Unknown";
        this.attemptedAction = "Unknown";
    }

    /** @return The username that attempted unauthorized access */
    public String getUsername() { return username; }

    /** @return The action that was denied */
    public String getAttemptedAction() { return attemptedAction; }
}

