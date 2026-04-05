package com.rajarata.bank;

import com.rajarata.bank.ui.MainMenu;

/**
 * Main entry point for the Rajarata Digital Banking Application.
 * 
 * This application demonstrates the following OOP concepts:
 * 
 * 1. ENCAPSULATION - All data is private with controlled access via getters/setters
 * 2. INHERITANCE - User → Customer/Staff/Administrator, Account → Savings/Checking/Student/FD
 * 3. POLYMORPHISM - Method overriding (abstract methods), method overloading (constructors)
 * 4. ABSTRACTION - Abstract classes (User, Account), Interfaces (Transactable, etc.)
 * 5. COMPOSITION - Account HAS-A List<Transaction>, Customer HAS-A List<Account>
 * 6. DESIGN PATTERNS - Singleton (Bank), Factory (AccountFactory), Observer (Notifications)
 * 7. CUSTOM EXCEPTIONS - 5 custom exception classes for domain-specific error handling
 * 8. ENUMS - 5 enums for type-safe constants (TransactionType, LoanType, etc.)
 * 9. INTERFACES - 4 interfaces defining contracts (Transactable, Reportable, etc.)
 * 10. FILE I/O - Complete file-based persistence using FileHandler utility
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class Main {

    /**
     * Application entry point.
     * Initializes the Bank singleton and starts the console UI.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            // OOP Concept: Singleton Pattern - Get the single Bank instance
            Bank bank = Bank.getInstance();

            // Initialize the bank (load data, create files, etc.)
            bank.initialize();

            // OOP Concept: Encapsulation - MainMenu encapsulates all UI logic
            MainMenu mainMenu = new MainMenu();

            // Start the application
            mainMenu.start();

        } catch (Exception e) {
            System.err.println("\n  FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            System.err.println("\n  The application has encountered a critical error and must close.");
            System.err.println("  Please contact the system administrator.\n");
        }
    }
}
