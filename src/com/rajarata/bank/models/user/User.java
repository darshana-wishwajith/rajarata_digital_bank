package com.rajarata.bank.models.user;

import com.rajarata.bank.models.notification.Notification;
import com.rajarata.bank.interfaces.Notifiable;
import com.rajarata.bank.utils.EncryptionUtil;
import com.rajarata.bank.utils.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class representing a user of the Rajarata Digital Banking system.
 * Serves as the parent class for Customer, Staff, and Administrator.
 * 
 * OOP Concept: Abstraction - Defines common attributes and behaviors shared by
 * all user types, with abstract methods that subclasses must implement for
 * role-specific functionality.
 * 
 * OOP Concept: Inheritance - Customer, Staff, and Administrator extend this class,
 * inheriting common properties (userId, name, password) and overriding abstract
 * methods (getRole(), getDashboardMenu()).
 * 
 * OOP Concept: Encapsulation - All fields are private with controlled access
 * through getters/setters. Password is stored as a hash, never in plain text.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public abstract class User implements Notifiable {

    // ==================== PRIVATE FIELDS ====================
    // OOP Concept: Encapsulation - All fields are private

    /** Unique identifier for the user */
    private String userId;
    /** Full name of the user */
    private String fullName;
    /** Email address (used as username for login) */
    private String email;
    /** Phone number */
    private String phone;
    /** Physical address */
    private String address;
    /** Date of birth in yyyy-MM-dd format */
    private String dateOfBirth;
    /** Government-issued ID number */
    private String governmentId;
    /** Hashed password - never stored in plain text */
    private String passwordHash;
    /** Security question for password recovery */
    private String securityQuestion;
    /** Hashed security answer */
    private String securityAnswerHash;
    /** Whether the email has been verified */
    private boolean emailVerified;
    /** Whether the account is locked (e.g., too many failed logins) */
    private boolean accountLocked;
    /** Number of consecutive failed login attempts */
    private int failedLoginAttempts;
    /** Date the account was created */
    private String creationDate;
    /** Date of last successful login */
    private String lastLoginDate;
    /** Whether the user is currently in an active session */
    private boolean activeSession;
    /** List of notifications for this user */
    private List<Notification> notifications;

    /** Maximum number of failed login attempts before account lock */
    public static final int MAX_FAILED_ATTEMPTS = 3;

    // ==================== CONSTRUCTORS ====================
    // OOP Concept: Constructors - Parameterized and default constructors

    /**
     * Default constructor - initializes default values.
     */
    protected User() {
        this.notifications = new ArrayList<>();
        this.emailVerified = false;
        this.accountLocked = false;
        this.failedLoginAttempts = 0;
        this.activeSession = false;
        this.creationDate = DateUtil.getCurrentDate();
    }

    /**
     * Parameterized constructor for creating a new user with all required fields.
     * 
     * @param userId Unique user ID
     * @param fullName User's full name
     * @param email User's email address
     * @param phone User's phone number
     * @param address User's physical address
     * @param dateOfBirth User's date of birth (yyyy-MM-dd)
     * @param governmentId Government-issued ID number
     * @param password Plain text password (will be hashed before storage)
     */
    protected User(String userId, String fullName, String email, String phone,
                   String address, String dateOfBirth, String governmentId, String password) {
        this(); // Call default constructor for initialization
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.governmentId = governmentId;
        this.passwordHash = EncryptionUtil.hashPassword(password);
    }

    // ==================== ABSTRACT METHODS ====================
    // OOP Concept: Abstraction - Subclasses must implement these

    /**
     * Gets the role of this user (Customer, Staff, Administrator).
     * Each subclass provides its own implementation.
     * 
     * @return The user's role as a string
     */
    public abstract String getRole();

    /**
     * Gets the dashboard menu options for this user's role.
     * Different roles see different menu options.
     * 
     * @return Formatted string of menu options
     */
    public abstract String getDashboardMenu();

    // ==================== PUBLIC METHODS ====================

    /**
     * Validates a plain text password against the stored hash.
     * Tracks failed attempts and locks account after MAX_FAILED_ATTEMPTS.
     * 
     * @param password The plain text password to verify
     * @return true if the password matches
     */
    public boolean validatePassword(String password) {
        if (accountLocked) {
            return false;
        }

        boolean valid = EncryptionUtil.verifyPassword(password, passwordHash);

        if (valid) {
            failedLoginAttempts = 0; // Reset on successful login
            lastLoginDate = DateUtil.getCurrentDateTime();
            activeSession = true;
        } else {
            failedLoginAttempts++;
            if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                accountLocked = true;
            }
        }

        return valid;
    }

    /**
     * Changes the user's password after verifying the old password.
     * 
     * @param oldPassword The current password
     * @param newPassword The new password to set
     * @return true if the password was successfully changed
     */
    public boolean changePassword(String oldPassword, String newPassword) {
        if (EncryptionUtil.verifyPassword(oldPassword, passwordHash)) {
            this.passwordHash = EncryptionUtil.hashPassword(newPassword);
            return true;
        }
        return false;
    }

    /**
     * Recovers the account using security question verification.
     * 
     * @param securityAnswer The answer to the security question
     * @param newPassword The new password to set
     * @return true if recovery was successful
     */
    public boolean recoverPassword(String securityAnswer, String newPassword) {
        if (securityAnswerHash != null && 
            EncryptionUtil.verifyPassword(securityAnswer, securityAnswerHash)) {
            this.passwordHash = EncryptionUtil.hashPassword(newPassword);
            this.accountLocked = false;
            this.failedLoginAttempts = 0;
            return true;
        }
        return false;
    }

    /**
     * Sets the security question and answer for password recovery.
     * 
     * @param question The security question
     * @param answer The answer to the security question
     */
    public void setSecurityQuestion(String question, String answer) {
        this.securityQuestion = question;
        this.securityAnswerHash = EncryptionUtil.hashPassword(answer);
    }

    /**
     * Ends the user's active session (logout).
     */
    public void logout() {
        this.activeSession = false;
    }

    /**
     * Unlocks a locked account (admin function).
     */
    public void unlockAccount() {
        this.accountLocked = false;
        this.failedLoginAttempts = 0;
    }

    /**
     * Verifies the user's email (simulation).
     */
    public void verifyEmail() {
        this.emailVerified = true;
    }

    // ==================== NOTIFIABLE INTERFACE IMPLEMENTATION ====================
    // OOP Concept: Polymorphism - Implementing Notifiable interface

    /**
     * {@inheritDoc}
     * Receives and stores a notification in this user's notification list.
     */
    @Override
    public void receiveNotification(Notification notification) {
        if (notifications == null) {
            notifications = new ArrayList<>();
        }
        notifications.add(notification);
    }

    /**
     * {@inheritDoc}
     * Returns all unread notifications for this user.
     */
    @Override
    public List<Notification> getUnreadNotifications() {
        if (notifications == null) return new ArrayList<>();
        return notifications.stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * Returns all notifications (read and unread) for this user.
     */
    @Override
    public List<Notification> getAllNotifications() {
        if (notifications == null) return new ArrayList<>();
        return new ArrayList<>(notifications);
    }

    /**
     * {@inheritDoc}
     * Marks a specific notification as read.
     */
    @Override
    public void markNotificationAsRead(String notificationId) {
        if (notifications == null) return;
        for (Notification notification : notifications) {
            if (notification.getNotificationId().equals(notificationId)) {
                notification.markAsRead();
                break;
            }
        }
    }

    /**
     * Deletes a notification by ID.
     * @param notificationId The ID of the notification to delete
     * @return true if the notification was found and deleted
     */
    public boolean deleteNotification(String notificationId) {
        if (notifications == null) return false;
        return notifications.removeIf(n -> n.getNotificationId().equals(notificationId));
    }

    /**
     * Gets the count of unread notifications.
     * @return Number of unread notifications
     */
    public int getUnreadNotificationCount() {
        if (notifications == null) return 0;
        return (int) notifications.stream().filter(n -> !n.isRead()).count();
    }

    // ==================== SERIALIZATION ====================
    // OOP Concept: File Handling - Converting object state to/from string for persistence

    /**
     * Serializes the user to a delimited string for file storage.
     * @return Pipe-delimited string representation of the user
     */
    public String toFileString() {
        return String.join("|",
                userId, fullName, email, phone, address, dateOfBirth,
                governmentId, passwordHash,
                securityQuestion != null ? securityQuestion : "",
                securityAnswerHash != null ? securityAnswerHash : "",
                String.valueOf(emailVerified),
                String.valueOf(accountLocked),
                String.valueOf(failedLoginAttempts),
                creationDate != null ? creationDate : "",
                lastLoginDate != null ? lastLoginDate : "",
                getRole()
        );
    }

    /**
     * Displays a summary of the user profile.
     * @return Formatted string of user details (sensitive data masked)
     */
    public String getProfileSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append("║           USER PROFILE                   ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append(String.format("║ User ID     : %-26s ║\n", userId));
        sb.append(String.format("║ Name        : %-26s ║\n", fullName));
        sb.append(String.format("║ Email       : %-26s ║\n", email));
        sb.append(String.format("║ Phone       : %-26s ║\n", phone));
        sb.append(String.format("║ Role        : %-26s ║\n", getRole()));
        sb.append(String.format("║ Verified    : %-26s ║\n", emailVerified ? "Yes" : "No"));
        sb.append(String.format("║ Status      : %-26s ║\n", accountLocked ? "LOCKED" : "Active"));
        sb.append(String.format("║ Member Since: %-26s ║\n", creationDate != null ? creationDate : "N/A"));
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    // ==================== GETTERS AND SETTERS ====================
    // OOP Concept: Encapsulation - Controlled access to private fields

    /** @return The user's unique ID */
    public String getUserId() { return userId; }
    /** @param userId The user ID to set */
    public void setUserId(String userId) { this.userId = userId; }

    /** @return The user's full name */
    public String getFullName() { return fullName; }
    /** @param fullName The full name to set */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /** @return The user's email address */
    public String getEmail() { return email; }
    /** @param email The email to set */
    public void setEmail(String email) { this.email = email; }

    /** @return The user's phone number */
    public String getPhone() { return phone; }
    /** @param phone The phone number to set */
    public void setPhone(String phone) { this.phone = phone; }

    /** @return The user's address */
    public String getAddress() { return address; }
    /** @param address The address to set */
    public void setAddress(String address) { this.address = address; }

    /** @return The user's date of birth */
    public String getDateOfBirth() { return dateOfBirth; }
    /** @param dateOfBirth The date of birth to set */
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    /** @return The user's government ID */
    public String getGovernmentId() { return governmentId; }
    /** @param governmentId The government ID to set */
    public void setGovernmentId(String governmentId) { this.governmentId = governmentId; }

    /** @return The hashed password (never the plain text) */
    public String getPasswordHash() { return passwordHash; }
    /** @param passwordHash The password hash to set (for data loading) */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** @return The security question */
    public String getSecurityQuestion() { return securityQuestion; }
    /** @param securityQuestion The security question to set */
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }

    /** @return The hashed security answer */
    public String getSecurityAnswerHash() { return securityAnswerHash; }
    /** @param hash The security answer hash to set */
    public void setSecurityAnswerHash(String hash) { this.securityAnswerHash = hash; }

    /** @return Whether email is verified */
    public boolean isEmailVerified() { return emailVerified; }
    /** @param emailVerified The verification status to set */
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    /** @return Whether the account is locked */
    public boolean isAccountLocked() { return accountLocked; }
    /** @param accountLocked The lock status to set */
    public void setAccountLocked(boolean accountLocked) { this.accountLocked = accountLocked; }

    /** @return Number of failed login attempts */
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    /** @param attempts The failed attempt count to set */
    public void setFailedLoginAttempts(int attempts) { this.failedLoginAttempts = attempts; }

    /** @return The account creation date */
    public String getCreationDate() { return creationDate; }
    /** @param creationDate The creation date to set */
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }

    /** @return The date of last login */
    public String getLastLoginDate() { return lastLoginDate; }
    /** @param lastLoginDate The last login date to set */
    public void setLastLoginDate(String lastLoginDate) { this.lastLoginDate = lastLoginDate; }

    /** @return Whether the user has an active session */
    public boolean isActiveSession() { return activeSession; }
    /** @param activeSession The session status to set */
    public void setActiveSession(boolean activeSession) { this.activeSession = activeSession; }

    /** @return The user's notifications list */
    public List<Notification> getNotifications() { return notifications; }
    /** @param notifications The notifications list to set */
    public void setNotifications(List<Notification> notifications) { this.notifications = notifications; }
}
