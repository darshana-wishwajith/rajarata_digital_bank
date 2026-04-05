package com.rajarata.bank.services;

import com.rajarata.bank.models.notification.*;
import com.rajarata.bank.models.user.*;
import com.rajarata.bank.utils.FileHandler;

import java.util.List;
import java.util.Map;

/**
 * Service class for managing notifications and alerts.
 * Implements the Observer pattern - observers (users) receive notifications
 * when events occur in the system.
 * 
 * OOP Concept: Observer Pattern - The notification service acts as the subject,
 * pushing notifications to subscribed users (observers) when events occur.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class NotificationService {

    /** Reference to authentication service for user lookup */
    private final AuthenticationService authService;

    public NotificationService(AuthenticationService authService) {
        this.authService = authService;
    }

    /**
     * Sends a notification to a specific user.
     * OOP Concept: Observer Pattern - Notifying the user (observer).
     * 
     * @param userId The recipient user's ID
     * @param alertType Type of alert
     * @param title Notification title
     * @param message Notification message
     */
    public void sendNotification(String userId, AlertType alertType, String title, String message) {
        User user = authService.getUserById(userId);
        if (user != null) {
            Notification notification = new Notification(userId, alertType, title, message);
            user.receiveNotification(notification);
            FileHandler.appendLine(FileHandler.NOTIFICATIONS_FILE, notification.toFileString());
        }
    }

    /**
     * Broadcasts a notification to all staff members.
     * 
     * @param alertType Type of alert
     * @param title Notification title
     * @param message Notification message
     */
    public void broadcastToStaff(AlertType alertType, String title, String message) {
        Map<String, User> allUsers = authService.getAllUsersById();
        for (User user : allUsers.values()) {
            if ("Staff".equals(user.getRole()) || "Administrator".equals(user.getRole())) {
                Notification notification = new Notification(user.getUserId(), alertType, title, message);
                user.receiveNotification(notification);
                FileHandler.appendLine(FileHandler.NOTIFICATIONS_FILE, notification.toFileString());
            }
        }
    }

    /**
     * Loads notifications from file and assigns to users.
     */
    public void loadNotifications() {
        List<String> lines = FileHandler.readAllLines(FileHandler.NOTIFICATIONS_FILE);
        int maxCounter = 0;

        for (String line : lines) {
            try {
                String[] parts = line.split(FileHandler.DELIMITER_REGEX, -1);
                if (parts.length < 7) continue;

                Notification notification = new Notification(
                        parts[0], parts[1], AlertType.valueOf(parts[2]),
                        parts[3], parts[4], parts[5], Boolean.parseBoolean(parts[6]));

                User user = authService.getUserById(parts[1]);
                if (user != null) {
                    user.receiveNotification(notification);
                }

                try {
                    int num = Integer.parseInt(parts[0].replace("NOTIF-", ""));
                    maxCounter = Math.max(maxCounter, num);
                } catch (NumberFormatException e) { /* ignore */ }

            } catch (Exception e) {
                System.err.println("Warning: Failed to load notification: " + e.getMessage());
            }
        }

        Notification.setNotificationCounter(maxCounter);
    }

    /**
     * Gets formatted notification display for a user.
     * @param userId The user's ID
     * @return Formatted notification list
     */
    public String getNotificationDisplay(String userId) {
        User user = authService.getUserById(userId);
        if (user == null) return "User not found.";

        List<Notification> notifications = user.getAllNotifications();
        if (notifications.isEmpty()) {
            return "\n  No notifications.\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append("║          NOTIFICATIONS                   ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");

        int unread = user.getUnreadNotificationCount();
        sb.append(String.format("║ Total: %d  |  Unread: %d                 ║\n",
                notifications.size(), unread));
        sb.append("╚══════════════════════════════════════════╝\n\n");

        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification n = notifications.get(i);
            sb.append(String.format(" %d. %s\n", notifications.size() - i, n.getDisplayString()));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Marks a notification as read by its list index.
     * @param userId User ID
     * @param index 1-based index in notification list
     * @return true if marked successfully
     */
    public boolean markAsReadByIndex(String userId, int index) {
        User user = authService.getUserById(userId);
        if (user == null) return false;

        List<Notification> notifications = user.getAllNotifications();
        int reverseIndex = notifications.size() - index;
        if (reverseIndex >= 0 && reverseIndex < notifications.size()) {
            notifications.get(reverseIndex).markAsRead();
            return true;
        }
        return false;
    }

    /**
     * Marks all notifications as read for a user.
     * @param userId User ID
     */
    public void markAllAsRead(String userId) {
        User user = authService.getUserById(userId);
        if (user != null) {
            for (Notification n : user.getAllNotifications()) {
                n.markAsRead();
            }
        }
    }
}
