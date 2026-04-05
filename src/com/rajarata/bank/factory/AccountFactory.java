package com.rajarata.bank.factory;

import com.rajarata.bank.models.account.*;
import com.rajarata.bank.exceptions.InvalidAccountException;

/**
 * Factory class responsible for creating different types of bank accounts.
 * 
 * OOP Concept: Factory Design Pattern - Centralizes account creation logic,
 * allowing the system to create the appropriate account subclass based on
 * a type string without the caller needing to know about specific classes.
 * 
 * OOP Concept: Polymorphism - Returns Account base type, letting the system
 * work with accounts polymorphically regardless of their concrete type.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class AccountFactory {

    /**
     * Private constructor to prevent instantiation (static factory pattern).
     */
    private AccountFactory() {
        throw new UnsupportedOperationException("Factory class - use static methods");
    }

    /**
     * Creates a bank account of the specified type.
     * Uses the Factory pattern to instantiate the correct subclass.
     * 
     * OOP Concept: Factory Pattern - Client code requests an account by type name,
     * and the factory returns the correct subclass instance. This decouples
     * account creation from usage, making it easy to add new account types.
     * 
     * OOP Concept: Polymorphism - Return type is the abstract Account class,
     * but actual object is a specific subclass.
     * 
     * @param accountType Type of account ("Savings", "Checking", "Student", "Fixed Deposit")
     * @param customerId The owning customer's ID
     * @param initialDeposit The initial deposit amount
     * @param currency The account currency
     * @param dateOfBirth Customer's DOB (needed for Student account age validation)
     * @param lockInMonths Lock-in period for Fixed Deposit (0 for other types)
     * @return A new Account instance of the specified type
     * @throws InvalidAccountException if type is invalid or validation fails
     */
    public static Account createAccount(String accountType, String customerId,
                                         double initialDeposit, String currency,
                                         String dateOfBirth, int lockInMonths) 
            throws InvalidAccountException {

        switch (accountType.toLowerCase()) {
            case "savings":
                return new SavingsAccount(customerId, initialDeposit, currency);

            case "checking":
                return new CheckingAccount(customerId, initialDeposit, currency);

            case "student":
                return new StudentAccount(customerId, initialDeposit, currency, dateOfBirth);

            case "fixed deposit":
            case "fixeddeposit":
            case "fd":
                return new FixedDepositAccount(customerId, initialDeposit, currency, lockInMonths);

            default:
                throw new InvalidAccountException(
                    "Unknown account type: " + accountType + 
                    ". Valid types: Savings, Checking, Student, Fixed Deposit");
        }
    }

    /**
     * Simplified factory method for non-FD, non-student accounts.
     * 
     * OOP Concept: Method Overloading - Simplified version of createAccount
     * for cases where dateOfBirth and lockInMonths are not needed.
     * 
     * @param accountType Type of account
     * @param customerId Customer ID
     * @param initialDeposit Initial deposit amount
     * @param currency Account currency
     * @return A new Account instance
     * @throws InvalidAccountException if type is invalid or validation fails
     */
    public static Account createAccount(String accountType, String customerId,
                                         double initialDeposit, String currency) 
            throws InvalidAccountException {
        return createAccount(accountType, customerId, initialDeposit, currency, null, 0);
    }

    /**
     * Creates an account from loaded file data (reconstruction).
     * 
     * @param accountType The account type string
     * @return An empty account instance of the correct type for data loading
     */
    public static Account createEmptyAccount(String accountType) {
        switch (accountType.toLowerCase()) {
            case "savings":
                return new SavingsAccount();
            case "checking":
                return new CheckingAccount();
            case "student":
                return new StudentAccount();
            case "fixed deposit":
                return new FixedDepositAccount();
            default:
                return new SavingsAccount(); // Default fallback
        }
    }

    /**
     * Gets the minimum initial deposit for a given account type.
     * 
     * @param accountType The account type
     * @return Minimum initial deposit amount
     */
    public static double getMinimumDeposit(String accountType) {
        switch (accountType.toLowerCase()) {
            case "savings":
                return 500.0;
            case "checking":
                return 100.0;
            case "student":
                return 100.0;
            case "fixed deposit":
            case "fd":
                return 5000.0;
            default:
                return 100.0;
        }
    }

    /**
     * Gets a description of all available account types.
     * @return Formatted string describing all account types
     */
    public static String getAccountTypeDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║              AVAILABLE ACCOUNT TYPES                    ║\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║  1. Savings Account                                    ║\n");
        sb.append("║     • Interest: 3.5% p.a.  • Min Balance: 500 Units    ║\n");
        sb.append("║     • 5 withdrawals/month  • No overdraft             ║\n");
        sb.append("║                                                        ║\n");
        sb.append("║  2. Checking Account                                   ║\n");
        sb.append("║     • Interest: 0.5% p.a.  • No min balance           ║\n");
        sb.append("║     • Unlimited withdrawals • 100,000 Units overdraft  ║\n");
        sb.append("║                                                        ║\n");
        sb.append("║  3. Student Account                                    ║\n");
        sb.append("║     • Interest: 2.0% p.a.  • No min balance           ║\n");
        sb.append("║     • 10 withdrawals/month • Ages 18-25 only          ║\n");
        sb.append("║                                                        ║\n");
        sb.append("║  4. Fixed Deposit Account                              ║\n");
        sb.append("║     • Interest: 5.5% p.a.  • Min deposit: 5,000 Units  ║\n");
        sb.append("║     • Lock-in: 6/12/24/36 months                      ║\n");
        sb.append("║     • Early withdrawal: 2% penalty                    ║\n");
        sb.append("╚══════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }
}
