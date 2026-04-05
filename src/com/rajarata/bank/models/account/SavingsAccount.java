package com.rajarata.bank.models.account;

import com.rajarata.bank.exceptions.InsufficientFundsException;
import com.rajarata.bank.exceptions.InvalidAccountException;

/**
 * Savings Account implementation with specific rules:
 * - Interest Rate: 3.5% per annum (calculated monthly)
 * - Minimum Balance: $500 (penalty of $25 if violated)
 * - Monthly Withdrawal Limit: 5 transactions
 * - No overdraft allowed
 * 
 * OOP Concept: Inheritance - Extends abstract Account class, inheriting common
 * account properties and implementing abstract methods.
 * 
 * OOP Concept: Polymorphism - Overrides calculateInterest(), getAccountType(),
 * and other abstract methods with savings-specific implementations.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class SavingsAccount extends Account {

    // ==================== CONSTANTS ====================
    // OOP Concept: Final Keyword - Account-type-specific constants

    /** Annual interest rate for savings accounts */
    private static final double ANNUAL_INTEREST_RATE = 0.035;       // 3.5%
    /** Minimum balance requirement */
    private static final double MINIMUM_BALANCE = 500.0;
    /** Penalty for falling below minimum balance */
    private static final double BELOW_MIN_PENALTY = 25.0;
    /** Maximum withdrawals per month */
    private static final int MAX_MONTHLY_WITHDRAWALS = 5;
    /** Minimum initial deposit */
    private static final double MIN_INITIAL_DEPOSIT = 500.0;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor.
     */
    public SavingsAccount() {
        super();
    }

    /**
     * Creates a new Savings Account with an initial deposit.
     * 
     * @param customerId The owning customer's ID
     * @param initialDeposit The initial deposit amount (must be >= $500)
     * @param currency The account currency
     * @throws InvalidAccountException if initial deposit is insufficient
     */
    public SavingsAccount(String customerId, double initialDeposit, String currency) 
            throws InvalidAccountException {
        super(customerId, initialDeposit, currency);
        if (initialDeposit < MIN_INITIAL_DEPOSIT) {
            throw new InvalidAccountException(
                "Savings account requires minimum initial deposit of $" + MIN_INITIAL_DEPOSIT,
                getAccountNumber());
        }
    }

    // ==================== ABSTRACT METHOD IMPLEMENTATIONS ====================
    // OOP Concept: Polymorphism - Savings-specific behavior

    /**
     * {@inheritDoc}
     * @return "Savings" as the account type name
     */
    @Override
    public String getAccountType() {
        return "Savings";
    }

    /**
     * {@inheritDoc}
     * @return $500 minimum balance requirement
     */
    @Override
    public double getMinimumBalance() {
        return MINIMUM_BALANCE;
    }

    /**
     * {@inheritDoc}
     * @return 5 withdrawal limit per month
     */
    @Override
    public int getMonthlyWithdrawalLimit() {
        return MAX_MONTHLY_WITHDRAWALS;
    }

    /**
     * {@inheritDoc}
     * @return 0 - No overdraft allowed for savings accounts
     */
    @Override
    public double getOverdraftLimit() {
        return 0.0;
    }

    // ==================== INTEREST BEARING IMPLEMENTATION ====================
    // OOP Concept: Strategy Pattern - Savings-specific interest calculation

    /**
     * Calculates monthly interest for the savings account.
     * Uses compound interest formula: Monthly Interest = Balance × (Annual Rate / 12)
     * 
     * OOP Concept: Polymorphism - Each account type calculates interest differently.
     * SavingsAccount uses 3.5% per annum, compounded monthly.
     * 
     * @return The calculated monthly interest amount
     */
    @Override
    public double calculateInterest() {
        // Monthly interest = Balance × (Annual Rate / 12)
        return getBalance() * (ANNUAL_INTEREST_RATE / 12);
    }

    /**
     * {@inheritDoc}
     * @return 0.035 (3.5% per annum)
     */
    @Override
    public double getInterestRate() {
        return ANNUAL_INTEREST_RATE;
    }

    /**
     * {@inheritDoc}
     * Applies monthly interest and checks for minimum balance penalty.
     */
    @Override
    public void applyInterest() {
        double interest = calculateInterest();
        if (interest > 0) {
            creditInterest(interest);
        }
        // Apply minimum balance penalty if applicable
        if (isBelowMinimumBalance()) {
            setBalance(getBalance() - BELOW_MIN_PENALTY);
        }
    }

    // ==================== WITHDRAWAL OVERRIDE ====================

    /**
     * {@inheritDoc}
     * Savings account does not allow overdraft. Balance cannot go below 0.
     */
    @Override
    public boolean canWithdraw(double amount) {
        return amount > 0 && amount <= getBalance();
    }

    /**
     * {@inheritDoc}
     * Available balance equals current balance (no overdraft).
     */
    @Override
    public double getAvailableBalance() {
        return getBalance();
    }

    /**
     * Gets the minimum balance penalty amount.
     * @return Penalty amount ($25)
     */
    public double getMinimumBalancePenalty() {
        return BELOW_MIN_PENALTY;
    }

    /**
     * {@inheritDoc}
     * Adds savings-specific fields.
     */
    @Override
    protected String getExtraFieldsForFile() {
        return "";
    }
}
