package com.rajarata.bank.utils;

import java.util.regex.Pattern;

/**
 * Utility class for input validation across the banking system.
 * Provides validation methods for emails, phone numbers, passwords,
 * monetary amounts, and other data formats.
 * 
 * OOP Concept: Static Members - All validation methods are static,
 * providing stateless validation logic accessible from anywhere.
 * 
 * OOP Concept: Final Keyword - Regex patterns are stored as final constants,
 * compiled once for performance.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public final class ValidationUtil {

    /** Regex pattern for valid email addresses */
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Regex pattern for valid phone numbers (10-15 digits, optional + prefix) */
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\+?[0-9]{10,15}$");

    /** 
     * Regex pattern for strong passwords:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    /** Regex pattern for government ID (alphanumeric, 5-20 characters) */
    private static final Pattern GOVT_ID_PATTERN = 
        Pattern.compile("^[A-Za-z0-9]{5,20}$");

    /** Minimum deposit amount in dollars */
    public static final double MIN_DEPOSIT_AMOUNT = 10.0;

    /** Maximum transaction amount for fraud detection threshold */
    public static final double LARGE_TRANSACTION_THRESHOLD = 5000.0;

    /** Maximum single withdrawal amount for fraud detection */
    public static final double FRAUD_WITHDRAWAL_THRESHOLD = 10000.0;

    /**
     * Private constructor to prevent instantiation.
     */
    private ValidationUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates an email address format.
     * @param email The email address to validate
     * @return true if the email format is valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates a phone number format.
     * Accepts 10-15 digits with optional leading '+'.
     * @param phone The phone number to validate
     * @return true if the phone format is valid
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim().replaceAll("[\\s-]", "")).matches();
    }

    /**
     * Validates password strength requirements.
     * Must have 8+ chars, uppercase, lowercase, digit, and special character.
     * @param password The password to validate
     * @return true if the password meets strength requirements
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Returns a description of password requirements for user feedback.
     * @return String describing password requirements
     */
    public static String getPasswordRequirements() {
        return "Password must be at least 8 characters with uppercase, lowercase, digit, and special character (@$!%*?&)";
    }

    /**
     * Validates a government ID format.
     * @param govtId The government ID to validate
     * @return true if the ID format is valid
     */
    public static boolean isValidGovtId(String govtId) {
        if (govtId == null || govtId.trim().isEmpty()) {
            return false;
        }
        return GOVT_ID_PATTERN.matcher(govtId.trim()).matches();
    }

    /**
     * Validates a monetary amount.
     * Must be positive and have at most 2 decimal places.
     * @param amount The amount to validate
     * @return true if the amount is valid
     */
    public static boolean isValidAmount(double amount) {
        if (amount <= 0) {
            return false;
        }
        // Check for maximum 2 decimal places
        String amountStr = String.valueOf(amount);
        int decimalIndex = amountStr.indexOf('.');
        if (decimalIndex != -1) {
            int decimalPlaces = amountStr.length() - decimalIndex - 1;
            return decimalPlaces <= 2;
        }
        return true;
    }

    /**
     * Validates that an amount meets the minimum deposit requirement.
     * @param amount The deposit amount
     * @return true if the amount >= minimum deposit
     */
    public static boolean isValidDeposit(double amount) {
        return isValidAmount(amount) && amount >= MIN_DEPOSIT_AMOUNT;
    }

    /**
     * Validates that a string is not null or empty.
     * @param value The string to validate
     * @return true if the string is not null or empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Validates a full name (at least 2 characters, letters and spaces only).
     * @param name The name to validate
     * @return true if the name is valid
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().length() < 2) {
            return false;
        }
        return name.trim().matches("^[A-Za-z\\s.'-]+$");
    }

    /**
     * Validates an address (at least 10 characters).
     * @param address The address to validate
     * @return true if the address is valid
     */
    public static boolean isValidAddress(String address) {
        return address != null && address.trim().length() >= 10;
    }

    /**
     * Validates that a date of birth represents an age within a range.
     * @param dateOfBirth The date of birth in yyyy-MM-dd format
     * @param minAge Minimum age (inclusive)
     * @param maxAge Maximum age (inclusive)
     * @return true if the age is within the specified range
     */
    public static boolean isAgeInRange(String dateOfBirth, int minAge, int maxAge) {
        int age = DateUtil.calculateAge(dateOfBirth);
        return age >= minAge && age <= maxAge;
    }

    /**
     * Validates a date of birth (must be in past, person must be at least 18).
     * @param dateOfBirth The date of birth in yyyy-MM-dd format
     * @return true if the date of birth is valid
     */
    public static boolean isValidDateOfBirth(String dateOfBirth) {
        if (!DateUtil.isValidDate(dateOfBirth)) {
            return false;
        }
        if (!DateUtil.isInPast(dateOfBirth)) {
            return false;
        }
        int age = DateUtil.calculateAge(dateOfBirth);
        return age >= 18 && age <= 120;
    }

    /**
     * Validates a bill account number based on provider format.
     * @param provider The bill provider name
     * @param accountNumber The bill account number
     * @return true if the account number format is valid for the provider
     */
    public static boolean isValidBillAccountNumber(String provider, String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return false;
        }
        String trimmed = accountNumber.trim();
        switch (provider.toUpperCase()) {
            case "ELECTRICITY":
                return trimmed.matches("^ELC-\\d{8}$"); // ELC-12345678
            case "WATER":
                return trimmed.matches("^WTR-\\d{8}$"); // WTR-12345678
            case "INTERNET":
                return trimmed.matches("^INT-\\d{8}$"); // INT-12345678
            case "PHONE":
                return trimmed.matches("^TEL-\\d{8}$"); // TEL-12345678
            default:
                return trimmed.length() >= 5;
        }
    }

    /**
     * Sanitizes a string input by removing potentially dangerous characters.
     * Prevents injection attacks in text inputs.
     * @param input The input string to sanitize
     * @return Sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Remove HTML tags and special characters that could be used for injection
        return input.replaceAll("[<>\"';\\\\]", "").trim();
    }

    /**
     * Formats an amount to 2 decimal places for display.
     * @param amount The monetary amount
     * @return Formatted amount string (e.g., "1,234.56")
     */
    public static String formatAmount(double amount) {
        return String.format("%,.2f", amount);
    }
}
