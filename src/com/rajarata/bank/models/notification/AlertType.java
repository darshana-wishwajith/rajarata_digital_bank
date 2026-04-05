package com.rajarata.bank.models.notification;

/**
 * Enumeration of alert/notification types used in the notification system.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public enum AlertType {
    /** Warning when balance drops below minimum threshold */
    LOW_BALANCE("Low Balance Warning"),
    /** Alert for a failed transaction attempt */
    TRANSACTION_FAILED("Transaction Failed"),
    /** Confirmation of successful transfer */
    TRANSFER_SUCCESS("Transfer Successful"),
    /** Reminder for upcoming loan installment */
    LOAN_REMINDER("Loan Installment Reminder"),
    /** Reminder for upcoming bill payment */
    BILL_REMINDER("Bill Payment Reminder"),
    /** Alert for transactions exceeding $5000 */
    LARGE_TRANSACTION("Large Transaction Alert"),
    /** Notification of account status change */
    ACCOUNT_STATUS_CHANGE("Account Status Change"),
    /** Alert for suspicious/fraudulent activity */
    FRAUD_ALERT("Fraud Alert"),
    /** Notification of loan status update */
    LOAN_STATUS_UPDATE("Loan Status Update"),
    /** Notification for a successful transaction (deposit, withdrawal) */
    TRANSACTION_SUCCESS("Transaction Successful"),
    /** Alert when overdraft facility is used on a checking account */
    OVERDRAFT_ALERT("Overdraft Used"),
    /** Security alert for repeated failed logins or unauthorized access */
    SECURITY_ALERT("Security Alert"),
    /** General system notification */
    SYSTEM_NOTIFICATION("System Notification");

    /** Human-readable display name */
    private final String displayName;

    /**
     * Constructor for AlertType enum.
     * @param displayName The user-friendly name
     */
    AlertType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the display name of this alert type.
     * @return Human-readable alert type name
     */
    public String getDisplayName() {
        return displayName;
    }
}
