package com.rajarata.bank.exceptions;

/**
 * Custom exception thrown when user input fails validation.
 * Covers invalid formats, out-of-range values, and malformed data.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class InvalidInputException extends Exception {

    /** The field name that has invalid input */
    private final String fieldName;

    /**
     * Constructs a new InvalidInputException.
     * @param message Human-readable error message
     * @param fieldName The name of the field with invalid input
     */
    public InvalidInputException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

    /**
     * Simple constructor with just a message.
     * @param message Human-readable error message
     */
    public InvalidInputException(String message) {
        super(message);
        this.fieldName = "Unknown";
    }

    /** @return The field name with invalid input */
    public String getFieldName() { return fieldName; }
}
