package com.rajarata.bank.models.account;

import com.rajarata.bank.exceptions.InsufficientFundsException;
import com.rajarata.bank.exceptions.InvalidAccountException;
import com.rajarata.bank.utils.DateUtil;

/**
 * Fixed Deposit Account implementation with specific rules:
 * - Interest Rate: 5.5% per annum (locked for minimum duration)
 * - Minimum Deposit: $5000
 * - Lock-in Period: 6, 12, 24, or 36 months
 * - Early Withdrawal Penalty: 2% of principal amount
 * - No regular withdrawals until maturity
 * 
 * OOP Concept: Inheritance - Extends Account abstract class.
 * 
 * OOP Concept: Polymorphism - Overrides withdraw() to prevent access before
 * maturity, and calculateInterest() for compound interest.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class FixedDepositAccount extends Account {

    // ==================== CONSTANTS ====================

    /** Annual interest rate for fixed deposits */
    private static final double ANNUAL_INTEREST_RATE = 0.055;       // 5.5%
    /** Minimum deposit amount */
    private static final double MIN_DEPOSIT_AMOUNT = 5000.0;
    /** Early withdrawal penalty (2% of principal) */
    private static final double EARLY_WITHDRAWAL_PENALTY_RATE = 0.02;
    /** Valid lock-in periods in months */
    public static final int[] VALID_LOCK_PERIODS = {6, 12, 24, 36};

    // ==================== PRIVATE FIELDS ====================

    /** Original principal amount deposited */
    private double principalAmount;
    /** Lock-in period in months */
    private int lockInMonths;
    /** Maturity date of the fixed deposit */
    private String maturityDate;
    /** Whether the fixed deposit has matured */
    private boolean matured;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor.
     */
    public FixedDepositAccount() {
        super();
        this.matured = false;
    }

    /**
     * Creates a new Fixed Deposit Account.
     * 
     * @param customerId The owning customer's ID
     * @param depositAmount The deposit amount (must be >= $5000)
     * @param currency The account currency
     * @param lockInMonths Lock-in period (6, 12, 24, or 36 months)
     * @throws InvalidAccountException if deposit or lock-in period is invalid
     */
    public FixedDepositAccount(String customerId, double depositAmount, String currency,
                                int lockInMonths) throws InvalidAccountException {
        super(customerId, depositAmount, currency);

        // Validate minimum deposit
        if (depositAmount < MIN_DEPOSIT_AMOUNT) {
            throw new InvalidAccountException(
                "Fixed deposit requires minimum $" + MIN_DEPOSIT_AMOUNT, getAccountNumber());
        }

        // Validate lock-in period
        if (!isValidLockPeriod(lockInMonths)) {
            throw new InvalidAccountException(
                "Invalid lock-in period. Valid options: 6, 12, 24, or 36 months", getAccountNumber());
        }

        this.principalAmount = depositAmount;
        this.lockInMonths = lockInMonths;
        this.maturityDate = DateUtil.addMonths(getOpenDate(), lockInMonths);
        this.matured = false;
    }

    // ==================== ABSTRACT METHOD IMPLEMENTATIONS ====================

    /** {@inheritDoc} */
    @Override
    public String getAccountType() {
        return "Fixed Deposit";
    }

    /**
     * {@inheritDoc}
     * @return The principal amount as minimum (cannot be partially withdrawn)
     */
    @Override
    public double getMinimumBalance() {
        return principalAmount;
    }

    /**
     * {@inheritDoc}
     * @return 0 - No regular withdrawals from fixed deposit
     */
    @Override
    public int getMonthlyWithdrawalLimit() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * @return 0 - No overdraft for fixed deposits
     */
    @Override
    public double getOverdraftLimit() {
        return 0.0;
    }

    // ==================== INTEREST BEARING IMPLEMENTATION ====================

    /**
     * Calculates compound interest for fixed deposit.
     * Formula: A = P(1 + r/n)^(nt) - P
     * Where: P = principal, r = annual rate, n = 12 (monthly), t = years
     * 
     * OOP Concept: Polymorphism - Fixed deposit uses compound interest,
     * unlike savings/checking which use simple monthly interest.
     * 
     * @return Total compound interest amount
     */
    @Override
    public double calculateInterest() {
        // Monthly compound interest calculation
        double monthlyRate = ANNUAL_INTEREST_RATE / 12;
        double months = lockInMonths;
        double compoundAmount = principalAmount * Math.pow(1 + monthlyRate, months);
        return compoundAmount - principalAmount;
    }

    /** {@inheritDoc} */
    @Override
    public double getInterestRate() {
        return ANNUAL_INTEREST_RATE;
    }

    /**
     * {@inheritDoc}
     * For FD, monthly interest is accrued but only posted at maturity.
     */
    @Override
    public void applyInterest() {
        // Check if maturity date has been reached
        checkMaturity();
        if (matured) {
            double interest = calculateInterest();
            creditInterest(interest);
        }
    }

    // ==================== WITHDRAWAL OVERRIDE ====================
    // OOP Concept: Polymorphism - FD has completely different withdrawal rules

    /**
     * {@inheritDoc}
     * Fixed deposits cannot be withdrawn before maturity without penalty.
     * Early withdrawal incurs a 2% penalty on principal.
     */
    @Override
    public boolean withdraw(double amount) throws InsufficientFundsException, InvalidAccountException {
        if (!"Active".equals(getStatus())) {
            throw new InvalidAccountException("Cannot withdraw from " + getStatus() + " account", getAccountNumber());
        }

        if (!matured) {
            // Early withdrawal - apply penalty
            double penalty = principalAmount * EARLY_WITHDRAWAL_PENALTY_RATE;
            double totalAvailable = getBalance() - penalty;

            if (amount > totalAvailable) {
                throw new InsufficientFundsException(
                    "Insufficient funds after early withdrawal penalty ($" + 
                    String.format("%.2f", penalty) + ")",
                    getAccountNumber(), amount, totalAvailable);
            }

            // Deduct penalty then amount
            setBalance(getBalance() - penalty - amount);
            setStatus("Closed"); // FD is closed after early withdrawal
            return true;
        } else {
            // Normal withdrawal after maturity
            if (amount > getBalance()) {
                throw new InsufficientFundsException(
                    "Insufficient funds", getAccountNumber(), amount, getBalance());
            }
            setBalance(getBalance() - amount);
            if (getBalance() == 0) {
                setStatus("Closed");
            }
            return true;
        }
    }

    /**
     * {@inheritDoc}
     * FD can only be fully withdrawn (early with penalty, or at maturity).
     */
    @Override
    public boolean canWithdraw(double amount) {
        if (matured) {
            return amount > 0 && amount <= getBalance();
        }
        // Early withdrawal possible but with penalty
        double penalty = principalAmount * EARLY_WITHDRAWAL_PENALTY_RATE;
        return amount > 0 && amount <= (getBalance() - penalty);
    }

    /** {@inheritDoc} */
    @Override
    public double getAvailableBalance() {
        if (matured) {
            return getBalance();
        }
        // Available after penalty deduction
        double penalty = principalAmount * EARLY_WITHDRAWAL_PENALTY_RATE;
        return Math.max(0, getBalance() - penalty);
    }

    // ==================== FD-SPECIFIC METHODS ====================

    /**
     * Checks if the fixed deposit has reached its maturity date.
     */
    public void checkMaturity() {
        if (!matured && maturityDate != null) {
            // Compare maturity date with current date
            if (!DateUtil.isInFuture(maturityDate)) {
                matured = true;
            }
        }
    }

    /**
     * Gets a summary of the fixed deposit details.
     * @return Formatted FD summary string
     */
    public String getFDSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n═══ Fixed Deposit Details ═══\n");
        sb.append(String.format("Principal      : $%,.2f\n", principalAmount));
        sb.append(String.format("Interest Rate  : %.1f%% p.a.\n", ANNUAL_INTEREST_RATE * 100));
        sb.append(String.format("Lock-in Period : %d months\n", lockInMonths));
        sb.append(String.format("Maturity Date  : %s\n", maturityDate));
        sb.append(String.format("Matured        : %s\n", matured ? "Yes" : "No"));
        sb.append(String.format("Expected Interest: $%,.2f\n", calculateInterest()));
        sb.append(String.format("Expected Total   : $%,.2f\n", principalAmount + calculateInterest()));
        if (!matured) {
            sb.append(String.format("Early Withdrawal Penalty: $%,.2f\n", 
                principalAmount * EARLY_WITHDRAWAL_PENALTY_RATE));
        }
        sb.append("═════════════════════════════\n");
        return sb.toString();
    }

    /**
     * Validates if a lock-in period is one of the allowed values.
     * @param months The period to validate
     * @return true if valid
     */
    public static boolean isValidLockPeriod(int months) {
        for (int period : VALID_LOCK_PERIODS) {
            if (period == months) return true;
        }
        return false;
    }

    // ==================== GETTERS AND SETTERS ====================

    /** @return The original principal amount */
    public double getPrincipalAmount() { return principalAmount; }
    /** @param principalAmount The principal to set */
    public void setPrincipalAmount(double principalAmount) { this.principalAmount = principalAmount; }

    /** @return Lock-in period in months */
    public int getLockInMonths() { return lockInMonths; }
    /** @param lockInMonths The lock-in period to set */
    public void setLockInMonths(int lockInMonths) { this.lockInMonths = lockInMonths; }

    /** @return The maturity date */
    public String getMaturityDate() { return maturityDate; }
    /** @param maturityDate The maturity date to set */
    public void setMaturityDate(String maturityDate) { this.maturityDate = maturityDate; }

    /** @return Whether the deposit has matured */
    public boolean isMatured() { return matured; }
    /** @param matured The maturity status to set */
    public void setMatured(boolean matured) { this.matured = matured; }

    /** {@inheritDoc} */
    @Override
    protected String getExtraFieldsForFile() {
        return String.join("|",
                String.valueOf(principalAmount),
                String.valueOf(lockInMonths),
                maturityDate != null ? maturityDate : "",
                String.valueOf(matured));
    }
}
