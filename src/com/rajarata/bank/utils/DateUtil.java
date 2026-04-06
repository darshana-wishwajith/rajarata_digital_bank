package com.rajarata.bank.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for date and time operations used throughout the banking system.
 * 
 * OOP Concept: Static Members - Provides static utility methods and constants
 * for date formatting, parsing, and validation. Cannot be instantiated.
 * 
 * OOP Concept: Final Keyword - Uses final constants for date format patterns,
 * ensuring consistency across the application.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public final class DateUtil {

    /** Standard date format used throughout the application */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    /** Standard date-time format for timestamps */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /** Display format for user-facing dates */
    public static final String DISPLAY_DATE_FORMAT = "dd MMM yyyy";
    /** Format for transaction IDs */
    public static final String TRANSACTION_DATE_FORMAT = "yyyyMMdd";

    /** Pre-built formatter for standard date format */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    /** Pre-built formatter for date-time format */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_FORMAT);
    /** Pre-built formatter for display date format */
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern(DISPLAY_DATE_FORMAT);
    /** Pre-built formatter for transaction date format */
    private static final DateTimeFormatter TRANSACTION_FORMATTER = DateTimeFormatter.ofPattern(TRANSACTION_DATE_FORMAT);

    /**
     * Private constructor to prevent instantiation.
     */
    private DateUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the current date as a formatted string.
     * @return Current date in "yyyy-MM-dd" format
     */
    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * Gets the current date and time as a formatted string.
     * @return Current date-time in "yyyy-MM-dd HH:mm:ss" format
     */
    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    /**
     * Gets the current date formatted for transaction ID generation.
     * @return Current date in "yyyyMMdd" format
     */
    public static String getTransactionDate() {
        return LocalDate.now().format(TRANSACTION_FORMATTER);
    }

    /**
     * Formats a date string for display purposes.
     * @param dateStr Date in "yyyy-MM-dd" format
     * @return Date in "dd MMM yyyy" format (e.g., "21 Mar 2026")
     */
    public static String formatForDisplay(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return date.format(DISPLAY_FORMATTER);
        } catch (DateTimeParseException e) {
            return dateStr; // Return original if parsing fails
        }
    }

    /**
     * Parses a date string in standard format to LocalDate.
     * @param dateStr Date in "yyyy-MM-dd" format
     * @return Parsed LocalDate object
     * @throws DateTimeParseException if the date string is invalid
     */
    public static LocalDate parseDate(String dateStr) throws DateTimeParseException {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    /**
     * Parses a date-time string to LocalDateTime.
     * @param dateTimeStr DateTime in "yyyy-MM-dd HH:mm:ss" format
     * @return Parsed LocalDateTime object
     * @throws DateTimeParseException if the string is invalid
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) throws DateTimeParseException {
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }

    /**
     * Calculates the age in years from a date of birth.
     * @param dateOfBirth Date of birth in "yyyy-MM-dd" format
     * @return Age in years
     */
    public static int calculateAge(String dateOfBirth) {
        try {
            LocalDate dob = LocalDate.parse(dateOfBirth, DATE_FORMATTER);
            return Period.between(dob, LocalDate.now()).getYears();
        } catch (DateTimeParseException e) {
            return -1; // Invalid date
        }
    }

    /**
     * Validates if a date string is in the correct format and is a valid date.
     * @param dateStr The date string to validate
     * @return true if the date is valid
     */
    public static boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Checks if a date is in the past (before today).
     * @param dateStr The date string to check
     * @return true if the date is in the past
     */
    public static boolean isInPast(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return date.isBefore(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Checks if a date is in the future (after today).
     * @param dateStr The date string to check
     * @return true if the date is in the future
     */
    public static boolean isInFuture(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return date.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Checks if a date falls within the specified range (inclusive).
     * @param dateStr The date to check
     * @param startDate Start of range
     * @param endDate End of range
     * @return true if date is within range
     */
    public static boolean isWithinRange(String dateStr, String startDate, String endDate) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
            return !date.isBefore(start) && !date.isAfter(end);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Adds months to a date and returns the result.
     * @param dateStr The starting date
     * @param months Number of months to add
     * @return The resulting date as a formatted string
     */
    public static String addMonths(String dateStr, int months) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return date.plusMonths(months).format(DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return dateStr;
        }
    }

    /**
     * Gets the number of days between two dates.
     * @param startDate The start date
     * @param endDate The end date
     * @return Number of days between the dates
     */
    public static long daysBetween(String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
            return java.time.temporal.ChronoUnit.DAYS.between(start, end);
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    /**
     * Formats a LocalDateTime to a string.
     * @param dateTime The LocalDateTime to format
     * @return Formatted date-time string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * Formats a LocalDate to a string.
     * @param date The LocalDate to format
     * @return Formatted date string
     */
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }
}

