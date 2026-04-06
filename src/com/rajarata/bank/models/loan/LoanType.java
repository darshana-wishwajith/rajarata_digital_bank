package com.rajarata.bank.models.loan;

/**
 * Enumeration of supported loan types offered by Rajarata Digital Bank.
 * 
 * Each loan type has a descriptive name, maximum allowed term, base interest rate,
 * and associated financial rules for transparency.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public enum LoanType {
    /** Personal loan for general purposes */
    PERSONAL("Personal Loan", 60, 0.08, 
             "Base 8% APR calculated monthly on reducing balance.",
             "Standard EMI via disbursement account. Limit: 50,000 to 2,000,000.",
             "5% late payment penalty applied after a 5-day grace period."),
             
    /** Auto loan for vehicle purchase */
    AUTO("Auto Loan", 84, 0.065,
             "Base 6.5% APR. Rate may vary according to vehicle age.",
             "Mandatory insurance coverage required. Limit: 100,000 to 10,000,000.",
             "Significant default may lead to vehicle repossession risks."),
             
    /** Home loan / mortgage */
    HOME("Home Loan", 360, 0.045,
             "Base 4.5% APR. Competitive fixed and variable options available.",
             "Mortgage on property title required. Limit: 500,000 to 50,000,000.",
             "Property auction rights reserved after 6 months of non-payment."),
             
    /** Education loan for academic expenses */
    EDUCATION("Education Loan", 120, 0.035,
             "Base 3.5% APR. Preferred rate for registered university students.",
             "Repayment grace period granted until 6 months after graduation.",
             "Guarantor required for applicants without active income.");

    private final String displayName;
    private final int maxTermMonths;
    private final double baseInterestRate;
    private final String interestRules;
    private final String repaymentRules;
    private final String penaltyRules;

    /**
     * Constructor for LoanType enum.
     * @param displayName The user-friendly name
     * @param maxTermMonths Maximum allowed term in months
     * @param baseInterestRate Base annual interest rate (e.g. 0.08 for 8%)
     * @param interestRules Interest calculation method description
     * @param repaymentRules Repayment conditions and limits
     * @param penaltyRules Penalty and default policies
     */
    LoanType(String displayName, int maxTermMonths, double baseInterestRate, 
             String interestRules, String repaymentRules, String penaltyRules) {
        this.displayName = displayName;
        this.maxTermMonths = maxTermMonths;
        this.baseInterestRate = baseInterestRate;
        this.interestRules = interestRules;
        this.repaymentRules = repaymentRules;
        this.penaltyRules = penaltyRules;
    }

    /** @return Human-readable loan type name */
    public String getDisplayName() { return displayName; }
    /** @return Maximum term in months */
    public int getMaxTermMonths() { return maxTermMonths; }
    /** @return Base APR as decimal */
    public double getBaseInterestRate() { return baseInterestRate; }
    /** @return Description of interest rules */
    public String getInterestRules() { return interestRules; }
    /** @return Description of repayment rules */
    public String getRepaymentRules() { return repaymentRules; }
    /** @return Description of penalty rules */
    public String getPenaltyRules() { return penaltyRules; }
}
