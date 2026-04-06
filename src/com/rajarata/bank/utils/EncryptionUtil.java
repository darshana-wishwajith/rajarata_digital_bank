package com.rajarata.bank.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for encryption and hashing operations.
 * Provides password hashing using SHA-256 and Base64 encoding/decoding.
 * 
 * OOP Concept: Static Members - All methods are static utility methods,
 * following the utility class pattern. The class cannot be instantiated.
 * 
 * OOP Concept: Encapsulation - Password hashing logic is encapsulated in
 * this single class, hiding cryptographic implementation details from callers.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public final class EncryptionUtil {

    /** Salt value used in password hashing to prevent rainbow table attacks */
    private static final String SALT = "RajarataBankSalt2024";

    /**
     * Private constructor to prevent instantiation of this utility class.
     * OOP Concept: Encapsulation - Preventing instantiation of utility classes.
     */
    private EncryptionUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Hashes a password using SHA-256 algorithm with salt.
     * The password is never stored in plain text - only the hash is stored.
     * 
     * Algorithm: SHA-256(salt + password) -> Base64 encoded string
     * 
     * @param password The plain text password to hash
     * @return Base64-encoded SHA-256 hash of the salted password
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = SALT + password;
            byte[] hashBytes = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to Base64 encoding if SHA-256 is unavailable (extremely unlikely)
            System.err.println("WARNING: SHA-256 not available, using Base64 fallback");
            return Base64.getEncoder().encodeToString((SALT + password).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Verifies a plain text password against a stored hash.
     * 
     * @param plainPassword The plain text password to verify
     * @param storedHash The stored hash to compare against
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        String hashedInput = hashPassword(plainPassword);
        return hashedInput.equals(storedHash);
    }

    /**
     * Encodes a string to Base64 format.
     * Used for encoding non-sensitive data for storage.
     * 
     * @param data The plain text string to encode
     * @return Base64-encoded string
     */
    public static String encodeBase64(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a Base64-encoded string back to plain text.
     * 
     * @param encodedData The Base64-encoded string to decode
     * @return The decoded plain text string
     */
    public static String decodeBase64(String encodedData) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Masks sensitive data for display purposes.
     * Shows only the last 4 characters, replacing the rest with asterisks.
     * Example: "1234567890" -> "******7890"
     * 
     * @param data The sensitive data to mask
     * @return Masked string with only last 4 characters visible
     */
    public static String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        int visibleChars = 4;
        String masked = "*".repeat(data.length() - visibleChars);
        return masked + data.substring(data.length() - visibleChars);
    }
}

