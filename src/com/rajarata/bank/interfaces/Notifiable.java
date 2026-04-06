package com.rajarata.bank.interfaces;

import com.rajarata.bank.models.notification.Notification;
import java.util.List;

/**
 * Interface for the Observer pattern in the notification system.
 * Entities implementing this interface can receive and manage notifications.
 * 
 * OOP Concept: Abstraction + Observer Pattern - Defines the observer contract
 * for the notification system. Accounts and users can subscribe to notifications.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public interface Notifiable {

    /**
     * Receives a notification and adds it to the entity's notification queue.
     * 
     * @param notification The notification to receive
     */
    void receiveNotification(Notification notification);

    /**
     * Gets all unread notifications for this entity.
     * 
     * @return List of unread notifications
     */
    List<Notification> getUnreadNotifications();

    /**
     * Gets all notifications (read and unread) for this entity.
     * 
     * @return List of all notifications
     */
    List<Notification> getAllNotifications();

    /**
     * Marks a notification as read.
     * 
     * @param notificationId The ID of the notification to mark as read
     */
    void markNotificationAsRead(String notificationId);
}

