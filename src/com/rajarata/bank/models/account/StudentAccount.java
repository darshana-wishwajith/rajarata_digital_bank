package com.rajarata.bank.models.account;

import com.rajarata.bank.exceptions.InvalidAccountException;
import com.rajarata.bank.utils.DateUtil;

/**
 * Student Account implementation with specific rules:
 * - Interest Rate: 2.0% per annum
 * - No minimum balance requirement
 * - Monthly Withdrawal Limit: 10 transactions
 * - No overdraft allowed
 * - Age Restriction: Customer must be 18-25 years old
 * 
 * OOP Concept: Inheritance - Extends Account abstract class.
 * 
 * OOP Concept: Polymorphism - Overrides abstract methods with student-specific
 * implementations including age validation.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class StudentAccount extends Account {

    // ==================== CONSTANTS ====================

    /** Annual interest rate for student accounts */
    private static final double ANNUAL_INTEREST_RATE = 0.02;        // 2.0%
    /** Maximum withdrawals per month */
    private static final int MAX_MONTHLY_WITHDRAWALS = 10;
    /** Minimum age for student account */
    private static final int MIN_AGE = 18;
    /** Maximum age for student account */
    private static final int MAX_AGE = 25;

    // ==================== PRIVATE FIELDS ====================

    /** Student's university or institution name */
    private String institution;
    /** Student ID at the institution */
    private String studentId;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor.
     */
    public StudentAccount() {
        super();
    }

    /**
     * Creates a new Student Account with age validation.
     * 
     * @param customerId The owning customer's ID
     * @param initialDeposit The initial deposit amount
     * @param currency The account currency
     * @param dateOfBirth Customer's date of birth for age validation
     * @throws InvalidAccountException if customer age is not 18-25
     */
    public StudentAccount(String customerId, double initialDeposit, String currency,
                          String dateOfBirth) throws InvalidAccountException {
        super(customerId, initialDeposit, currency);
        
        // Validate age restriction
        int age = DateUtil.calculateAge(dateOfBirth);
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new InvalidAccountException(
                "Student account requires age between " + MIN_AGE + " and " + MAX_AGE + 
                " (current age: " + age + ")", getAccountNumber());
        }
    }

    // ==================== ABSTRACT METHOD IMPLEMENTATIONS ====================

    /** {@inheritDoc} */
    @Override
    public String getAccountType() {
        return "Student";
    }

    /**
     * {@inheritDoc}
     * @return 0 - No minimum balance for student accounts
     */
    @Override
    public double getMinimumBalance() {
        return 0.0;
    }

    /**
     * {@inheritDoc}
     * @return 10 withdrawals per month
     */
    @Override
    public int getMonthlyWithdrawalLimit() {
        return MAX_MONTHLY_WITHDRAWALS;
    }

    /**
     * {@inheritDoc}
     * @return 0 - No overdraft for student accounts
     */
    @Override
    public double getOverdraftLimit() {
        return 0.0;
    }

    // ==================== INTEREST BEARING IMPLEMENTATION ====================

    /**
     * Calculates monthly interest for student account (2.0% p.a.).
     * 
     * OOP Concept: Polymorphism - Different rate than savings/checking accounts.
     * 
     * @return Monthly interest amount
     */
    @Override
    public double calculateInterest() {
        return getBalance() * (ANNUAL_INTEREST_RATE / 12);
    }

    /** {@inheritDoc} */
    @Override
    public double getInterestRate() {
        return ANNUAL_INTEREST_RATE;
    }

    /** {@inheritDoc} */
    @Override
    public void applyInterest() {
        double interest = calculateInterest();
        if (interest > 0) {
            creditInterest(interest);
        }
    }

    // ==================== WITHDRAWAL OVERRIDE ====================

    /** {@inheritDoc} */
    @Override
    public boolean canWithdraw(double amount) {
        return amount > 0 && amount <= getBalance();
    }

    /** {@inheritDoc} */
    @Override
    public double getAvailableBalance() {
        return getBalance();
    }

    // ==================== STATIC VALIDATION ====================

    /**
     * Checks if a customer is eligible for a student account based on age.
     * @param dateOfBirth Customer's date of birth
     * @return true if age is between 18 and 25
     */
    public static boolean isEligible(String dateOfBirth) {
        int age = DateUtil.calculateAge(dateOfBirth);
        return age >= MIN_AGE && age <= MAX_AGE;
    }

    // ==================== GETTERS AND SETTERS ====================

    /** @return The institution name */
    public String getInstitution() { return institution; }
    /** @param institution The institution name to set */
    public void setInstitution(String institution) { this.institution = institution; }

    /** @return The student ID */
    public String getStudentId() { return studentId; }
    /** @param studentId The student ID to set */
    public void setStudentId(String studentId) { this.studentId = studentId; }

    /** {@inheritDoc} */
    @Override
    protected String getExtraFieldsForFile() {
        return (institution != null ? institution : "") + "|" + (studentId != null ? studentId : "");
    }
}
