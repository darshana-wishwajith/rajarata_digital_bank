package com.rajarata.bank.models.loan;

/**
 * Enumeration of supported loan types offered by Rajarata Digital Bank.
 * 
 * Each loan type has a descriptive name and maximum allowed term in months.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public enum LoanType {
    /** Personal loan for general purposes */
    PERSONAL("Personal Loan", 60),
    /** Auto loan for vehicle purchase */
    AUTO("Auto Loan", 84),
    /** Home loan / mortgage */
    HOME("Home Loan", 360),
    /** Education loan for academic expenses */
    EDUCATION("Education Loan", 120);

    /** Human-readable display name */
    private final String displayName;
    /** Maximum loan term in months */
    private final int maxTermMonths;

    /**
     * Constructor for LoanType enum.
     * @param displayName The user-friendly name
     * @param maxTermMonths Maximum allowed term in months
     */
    LoanType(String displayName, int maxTermMonths) {
        this.displayName = displayName;
        this.maxTermMonths = maxTermMonths;
    }

    /**
     * Gets the display name of this loan type.
     * @return Human-readable loan type name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the maximum allowed term for this loan type.
     * @return Maximum term in months
     */
    public int getMaxTermMonths() {
        return maxTermMonths;
    }
}
