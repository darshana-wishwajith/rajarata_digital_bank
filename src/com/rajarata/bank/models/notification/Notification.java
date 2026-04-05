package com.rajarata.bank.models.notification;

import com.rajarata.bank.utils.DateUtil;

/**
 * Represents an in-app notification or alert sent to a user.
 * Notifications track read/unread status and support various alert types.
 * 
 * OOP Concept: Encapsulation - All fields are private with controlled access.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class Notification {

    // ==================== PRIVATE FIELDS ====================

    /** Unique notification ID */
    private final String notificationId;
    /** ID of the user this notification belongs to */
    private final String userId;
    /** Type of alert */
    private final AlertType alertType;
    /** Notification title/subject */
    private final String title;
    /** Notification message body */
    private final String message;
    /** When the notification was created */
    private final String timestamp;
    /** Whether the notification has been read */
    private boolean read;

    /** Static counter for ID generation */
    private static int notificationCounter = 0;

    // ==================== CONSTRUCTORS ====================

    /**
     * Creates a new notification.
     * 
     * @param userId The recipient user's ID
     * @param alertType The type of alert
     * @param title Notification title
     * @param message Notification message body
     */
    public Notification(String userId, AlertType alertType, String title, String message) {
        this.notificationId = generateNotificationId();
        this.userId = userId;
        this.alertType = alertType;
        this.title = title;
        this.message = message;
        this.timestamp = DateUtil.getCurrentDateTime();
        this.read = false;
    }

    /**
     * Constructor for loading from file.
     * 
     * @param notificationId Existing notification ID
     * @param userId User ID
     * @param alertType Alert type
     * @param title Title
     * @param message Message
     * @param timestamp Timestamp
     * @param read Read status
     */
    public Notification(String notificationId, String userId, AlertType alertType,
                        String title, String message, String timestamp, boolean read) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.alertType = alertType;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    // ==================== METHODS ====================

    /**
     * Marks this notification as read.
     */
    public void markAsRead() {
        this.read = true;
    }

    /**
     * Gets a formatted display string for this notification.
     * @return Formatted notification string
     */
    public String getDisplayString() {
        String readIndicator = read ? "  " : "●";
        return String.format("%s [%s] %s - %s\n   %s\n   %s",
                readIndicator,
                alertType.getDisplayName(),
                title,
                timestamp.substring(0, 10),
                message,
                "─".repeat(40));
    }

    /**
     * Serializes to file format.
     * @return Pipe-delimited string
     */
    public String toFileString() {
        return String.join("|",
                notificationId, userId, alertType.name(),
                title, message, timestamp, String.valueOf(read));
    }

    // ==================== STATIC METHODS ====================

    private static synchronized String generateNotificationId() {
        notificationCounter++;
        return String.format("NOTIF-%06d", notificationCounter);
    }

    public static void setNotificationCounter(int counter) {
        notificationCounter = counter;
    }

    // ==================== GETTERS ====================

    /** @return The notification ID */
    public String getNotificationId() { return notificationId; }
    /** @return The user ID */
    public String getUserId() { return userId; }
    /** @return The alert type */
    public AlertType getAlertType() { return alertType; }
    /** @return The notification title */
    public String getTitle() { return title; }
    /** @return The notification message */
    public String getMessage() { return message; }
    /** @return The timestamp */
    public String getTimestamp() { return timestamp; }
    /** @return Whether this notification has been read */
    public boolean isRead() { return read; }
}
