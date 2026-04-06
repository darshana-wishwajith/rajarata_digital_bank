package com.rajarata.bank.interfaces;

/**
 * Strategy interface for interest calculation.
 * Different account types implement different interest calculation strategies.
 * 
 * OOP Concept: Strategy Pattern + Polymorphism - Each account type provides
 * its own interest calculation algorithm through this interface, allowing
 * the interest engine to process any account type polymorphically.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public interface InterestBearing {

    /**
     * Calculates the interest earned or charged on the account.
     * Implementation varies by account type:
     * - SavingsAccount: 3.5% p.a. compounded monthly
     * - CheckingAccount: 0.5% p.a.
     * - StudentAccount: 2.0% p.a.
     * - FixedDepositAccount: 5.5% p.a. compounded
     * 
     * @return The calculated interest amount
     */
    double calculateInterest();

    /**
     * Gets the annual interest rate for this account.
     * 
     * @return The annual interest rate as a decimal (e.g., 0.035 for 3.5%)
     */
    double getInterestRate();

    /**
     * Applies the calculated interest to the account balance.
     * This should be called during monthly processing.
     */
    void applyInterest();
}

