package com.rajarata.bank.models.account;

import com.rajarata.bank.exceptions.InsufficientFundsException;
import com.rajarata.bank.exceptions.InvalidAccountException;

/**
 * Checking Account implementation with specific rules:
 * - Interest Rate: 0.5% per annum
 * - No minimum balance requirement
 * - Unlimited withdrawals
 * - Overdraft protection: Up to 100,000 LKR with 15% interest on overdraft amount
 * 
 * OOP Concept: Inheritance - Extends abstract Account class.
 * 
 * OOP Concept: Polymorphism - Overrides canWithdraw() to implement overdraft
 * logic, and calculateInterest() for checking-specific interest rate.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class CheckingAccount extends Account {

    // ==================== CONSTANTS ====================
    // OOP Concept: Final Keyword

    /** Annual interest rate for checking accounts */
    private static final double ANNUAL_INTEREST_RATE = 0.005;       // 0.5%
    /** Maximum overdraft amount allowed (100,000 LKR) */
    private static final double OVERDRAFT_LIMIT = 100000.0;
    /** Interest rate charged on overdraft amount */
    private static final double OVERDRAFT_INTEREST_RATE = 0.15;     // 15%

    // ==================== PRIVATE FIELDS ====================

    /** Current overdraft amount (if balance is negative) */
    private double overdraftUsed;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor.
     */
    public CheckingAccount() {
        super();
        this.overdraftUsed = 0.0;
    }

    /**
     * Creates a new Checking Account with an initial deposit.
     * No minimum deposit requirement.
     * 
     * @param customerId The owning customer's ID
     * @param initialDeposit The initial deposit amount 
     * @param currency The account currency
     */
    public CheckingAccount(String customerId, double initialDeposit, String currency) 
            throws InvalidAccountException {
        super(customerId, initialDeposit, currency);
        this.overdraftUsed = 0.0;
    }

    // ==================== ABSTRACT METHOD IMPLEMENTATIONS ====================

    /** {@inheritDoc} */
    @Override
    public String getAccountType() {
        return "Checking";
    }

    /**
     * {@inheritDoc}
     * @return 0 - No minimum balance for checking accounts
     */
    @Override
    public double getMinimumBalance() {
        return 0.0;
    }

    /**
     * {@inheritDoc}
     * @return -1 (unlimited withdrawals)
     */
    @Override
    public int getMonthlyWithdrawalLimit() {
        return -1; // Unlimited
    }

    /**
     * @return 100,000 LKR overdraft limit
     */
    @Override
    public double getOverdraftLimit() {
        return OVERDRAFT_LIMIT;
    }

    // ==================== INTEREST BEARING IMPLEMENTATION ====================

    /**
     * Calculates monthly interest for checking account.
     * If in overdraft, calculates overdraft interest (charge) instead.
     * 
     * OOP Concept: Polymorphism - Different interest logic than savings account.
     * Checking accounts also charge interest on overdraft amounts.
     * 
     * @return Interest amount (positive = credit, negative = overdraft charge)
     */
    @Override
    public double calculateInterest() {
        if (getBalance() >= 0) {
            // Regular interest on positive balance
            return getBalance() * (ANNUAL_INTEREST_RATE / 12);
        } else {
            // Charge overdraft interest on negative balance
            return getBalance() * (OVERDRAFT_INTEREST_RATE / 12); // This will be negative
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getInterestRate() {
        return ANNUAL_INTEREST_RATE;
    }

    /**
     * {@inheritDoc}
     * Applies interest credit or overdraft charge.
     */
    @Override
    public void applyInterest() {
        double interest = calculateInterest();
        creditInterest(interest);
        // Update overdraft tracking
        if (getBalance() < 0) {
            overdraftUsed = Math.abs(getBalance());
        } else {
            overdraftUsed = 0;
        }
    }

    // ==================== OVERDRAFT MANAGEMENT ====================

    /**
     * {@inheritDoc}
     * Checking account allows withdrawal up to balance + 100,000 LKR overdraft limit.
     * 
     * OOP Concept: Polymorphism - Overrides base class to add overdraft logic.
     */
    @Override
    public boolean canWithdraw(double amount) {
        return amount > 0 && amount <= (getBalance() + OVERDRAFT_LIMIT);
    }

    /**
     * {@inheritDoc}
     * Available balance includes overdraft limit.
     */
    @Override
    public double getAvailableBalance() {
        return getBalance() + OVERDRAFT_LIMIT;
    }

    /**
     * {@inheritDoc}
     * Tracks overdraft usage after withdrawal.
     */
    @Override
    public boolean withdraw(double amount) throws InsufficientFundsException, InvalidAccountException {
        boolean result = super.withdraw(amount);
        if (result && getBalance() < 0) {
            overdraftUsed = Math.abs(getBalance());
        }
        return result;
    }

    /** @return Current overdraft amount being used */
    public double getOverdraftUsed() { return overdraftUsed; }

    /** @return Remaining overdraft available */
    public double getOverdraftAvailable() {
        return OVERDRAFT_LIMIT - overdraftUsed;
    }

    /** @return The overdraft interest rate */
    public double getOverdraftInterestRate() { return OVERDRAFT_INTEREST_RATE; }

    /** {@inheritDoc} */
    @Override
    protected String getExtraFieldsForFile() {
        return String.valueOf(overdraftUsed);
    }

    /** @param overdraftUsed The overdraft used amount to set */
    public void setOverdraftUsed(double overdraftUsed) { this.overdraftUsed = overdraftUsed; }
}

