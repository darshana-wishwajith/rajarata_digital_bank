package com.rajarata.bank.ui;

import java.util.Scanner;

/**
 * Utility class for console-based user interface operations.
 * Provides standardized input/output methods for the banking application.
 * 
 * OOP Concept: Encapsulation - All console I/O logic is encapsulated here,
 * providing a consistent UI experience across all menus.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class ConsoleUI {

    /** Shared Scanner for console input */
    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleUI() {}

    /**
     * Reads a string input with a prompt.
     * @param prompt The prompt to display
     * @return The trimmed user input
     */
    public static String readString(String prompt) {
        System.out.print("  " + prompt + ": ");
        return scanner.nextLine().trim();
    }

    /**
     * Reads an integer input with validation and retry.
     * @param prompt The prompt to display
     * @return The validated integer
     */
    public static int readInt(String prompt) {
        while (true) {
            System.out.print("  " + prompt + ": ");
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                return value;
            } catch (NumberFormatException e) {
                printError("Please enter a valid number.");
            }
        }
    }

    /**
     * Reads a double input with validation.
     * @param prompt The prompt to display
     * @return The validated double
     */
    public static double readDouble(String prompt) {
        while (true) {
            System.out.print("  " + prompt + ": ");
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (value < 0) {
                    printError("Please enter a positive number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                printError("Please enter a valid number.");
            }
        }
    }

    /**
     * Reads a yes/no confirmation.
     * @param prompt The prompt to display
     * @return true if user confirmed (Y/yes)
     */
    public static boolean confirm(String prompt) {
        System.out.print("  " + prompt + " (Y/N): ");
        String input = scanner.nextLine().trim().toUpperCase();
        return "Y".equals(input) || "YES".equals(input);
    }

    /**
     * Reads a password input (displayed with asterisks if possible).
     * @param prompt The prompt
     * @return The password string
     */
    public static String readPassword(String prompt) {
        // Console-based - password will be visible (Java limitation in IDE)
        System.out.print("  " + prompt + ": ");
        return scanner.nextLine().trim();
    }

    /**
     * Prints a success message with formatting.
     * @param message The success message
     */
    public static void printSuccess(String message) {
        System.out.println("\n  ✓ SUCCESS: " + message + "\n");
    }

    /**
     * Prints an error message with formatting.
     * @param message The error message
     */
    public static void printError(String message) {
        System.out.println("\n  ✗ ERROR: " + message + "\n");
    }

    /**
     * Prints a warning message with formatting.
     * @param message The warning message
     */
    public static void printWarning(String message) {
        System.out.println("\n  ⚠ WARNING: " + message + "\n");
    }

    /**
     * Prints an info message with formatting.
     * @param message The info message
     */
    public static void printInfo(String message) {
        System.out.println("\n  ℹ " + message + "\n");
    }

    /**
     * Prints a divider line.
     */
    public static void printDivider() {
        System.out.println("  " + "─".repeat(50));
    }

    /**
     * Prints a header.
     * @param title The header title
     */
    public static void printHeader(String title) {
        System.out.println("\n  ═══ " + title + " ═══\n");
    }

    /**
     * Pauses execution until user presses Enter.
     */
    public static void pressEnterToContinue() {
        System.out.print("\n  Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Clears the console (best effort).
     */
    public static void clearScreen() {
        System.out.println("\n".repeat(3));
    }
}
