package com.rajarata.bank.models.loan;

/**
 * Enumeration representing the lifecycle status of a loan.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public enum LoanStatus {
    /** Loan application is pending staff review */
    PENDING("Pending Review"),
    /** Loan has been approved and funds disbursed */
    ACTIVE("Active"),
    /** Loan has been fully repaid */
    PAID("Fully Paid"),
    /** Loan application was rejected */
    REJECTED("Rejected"),
    /** Loan is in default (missed payments) */
    DEFAULTED("Defaulted");

    /** Human-readable display name */
    private final String displayName;

    /**
     * Constructor for LoanStatus enum.
     * @param displayName The user-friendly name
     */
    LoanStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the display name of this loan status.
     * @return Human-readable status name
     */
    public String getDisplayName() {
        return displayName;
    }
}

