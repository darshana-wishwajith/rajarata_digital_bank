package com.rajarata.bank.services;

import com.rajarata.bank.models.user.*;
import com.rajarata.bank.exceptions.*;
import com.rajarata.bank.utils.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service class for user authentication, registration, and session management.
 * Handles login, logout, account locking, and password recovery.
 * 
 * OOP Concept: Encapsulation - All authentication logic is encapsulated in this
 * service. Other classes interact through the public API without needing to know
 * about password hashing, lock mechanisms, or session tracking.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class AuthenticationService {

    /** Map of all users by email (login username) */
    private final Map<String, User> usersByEmail;
    /** Map of all users by user ID */
    private final Map<String, User> usersById;
    /** Currently logged-in user (null if no active session) */
    private User currentUser;
    /** IDs of hardcoded admin accounts (not saved to file) */
    private final Set<String> hardcodedUserIds;
    /** Reference to notification service (set after construction to avoid circular dependency) */
    private NotificationService notificationService;
    /** Reference to fraud detection service (set after construction to avoid circular dependency) */
    private FraudDetectionService fraudDetectionService;

    /**
     * Constructor initializes the user maps.
     */
    public AuthenticationService() {
        this.usersByEmail = new HashMap<>();
        this.usersById = new HashMap<>();
        this.currentUser = null;
        this.hardcodedUserIds = new HashSet<>();
    }

    /**
     * Sets the notification service reference (avoids circular dependency).
     * @param notificationService The notification service
     */
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Sets the fraud detection service reference (avoids circular dependency).
     * @param fraudDetectionService The fraud detection service
     */
    public void setFraudDetectionService(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    // ==================== REGISTRATION ====================

    /**
     * Registers a new customer in the system.
     * Validates all inputs and checks for duplicate emails/government IDs.
     * 
     * @param fullName Customer's full name
     * @param email Email address (used as login)
     * @param phone Phone number
     * @param address Physical address
     * @param dateOfBirth Date of birth (yyyy-MM-dd)
     * @param governmentId Government-issued ID
     * @param password Plain text password
     * @param securityQuestion Security question for recovery
     * @param securityAnswer Answer to the security question
     * @return The newly created Customer object
     * @throws InvalidInputException if any validation fails
     */
    public Customer registerCustomer(String fullName, String email, String phone,
                                      String address, String dateOfBirth, String governmentId,
                                      String password, String securityQuestion, String securityAnswer) 
            throws InvalidInputException {

        // Validate all inputs
        if (!ValidationUtil.isValidName(fullName)) {
            throw new InvalidInputException("Invalid name format. Use letters and spaces only.", "fullName");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new InvalidInputException("Invalid email format.", "email");
        }
        if (!ValidationUtil.isValidPhone(phone)) {
            throw new InvalidInputException("Invalid phone number. Use 10-15 digits.", "phone");
        }
        if (!ValidationUtil.isValidAddress(address)) {
            throw new InvalidInputException("Address must be at least 10 characters.", "address");
        }
        if (!ValidationUtil.isValidDateOfBirth(dateOfBirth)) {
            throw new InvalidInputException("Invalid date of birth. Must be 18+ years old. Format: yyyy-MM-dd", "dateOfBirth");
        }
        if (!ValidationUtil.isValidGovtId(governmentId)) {
            throw new InvalidInputException("Invalid government ID. Use 5-20 alphanumeric characters.", "governmentId");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new InvalidInputException(ValidationUtil.getPasswordRequirements(), "password");
        }

        // Check for duplicate email
        if (usersByEmail.containsKey(email.toLowerCase())) {
            throw new InvalidInputException("An account with this email already exists.", "email");
        }

        // Check for duplicate government ID
        for (User user : usersById.values()) {
            if (user.getGovernmentId() != null && user.getGovernmentId().equals(governmentId)) {
                throw new InvalidInputException("An account with this government ID already exists.", "governmentId");
            }
        }

        // Create new customer
        Customer customer = new Customer(fullName, email, phone, address, dateOfBirth, governmentId, password);
        customer.setSecurityQuestion(securityQuestion, securityAnswer);

        // Add to maps
        usersByEmail.put(email.toLowerCase(), customer);
        usersById.put(customer.getUserId(), customer);

        // Save to file
        FileHandler.appendLine(FileHandler.CUSTOMERS_FILE, customer.toFileString());
        FileHandler.logAudit("REGISTRATION", "New customer registered: " + customer.getUserId() + " (" + email + ")");

        return customer;
    }

    /**
     * Creates a new staff account (administrator function).
     * 
     * @param fullName Staff name
     * @param email Staff email
     * @param phone Phone number
     * @param address Address
     * @param dateOfBirth Date of birth
     * @param governmentId Government ID
     * @param password Password
     * @param department Department name
     * @param position Job position
     * @return The newly created Staff object
     * @throws InvalidInputException if validation fails
     * @throws UnauthorizedAccessException if current user is not admin
     */
    public Staff createStaffAccount(String fullName, String email, String phone,
                                     String address, String dateOfBirth, String governmentId,
                                     String password, String department, String position) 
            throws InvalidInputException, UnauthorizedAccessException {

        // Verify admin access
        if (currentUser == null || !"Administrator".equals(currentUser.getRole())) {
            throw new UnauthorizedAccessException("Only administrators can create staff accounts");
        }

        // Basic validation
        if (!ValidationUtil.isValidEmail(email)) {
            throw new InvalidInputException("Invalid email format.", "email");
        }
        if (usersByEmail.containsKey(email.toLowerCase())) {
            throw new InvalidInputException("An account with this email already exists.", "email");
        }

        Staff staff = new Staff(fullName, email, phone, address, dateOfBirth, governmentId,
                               password, department, position);
        staff.setSecurityQuestion("What is your employee ID?", staff.getUserId());
        staff.verifyEmail();

        usersByEmail.put(email.toLowerCase(), staff);
        usersById.put(staff.getUserId(), staff);

        FileHandler.appendLine(FileHandler.CUSTOMERS_FILE, staff.toFileString());
        FileHandler.logAudit("STAFF_CREATION", "New staff created: " + staff.getUserId() + " by " + currentUser.getUserId());

        return staff;
    }

    // ==================== AUTHENTICATION ====================

    /**
     * Authenticates a user with email and password.
     * Tracks failed attempts and locks accounts after 3 failures.
     * 
     * @param email The user's email (login username)
     * @param password The user's password
     * @return The authenticated User object
     * @throws AccountLockedException if the account is locked
     * @throws InvalidInputException if credentials are invalid
     */
    public User login(String email, String password) 
            throws AccountLockedException, InvalidInputException {

        User user = usersByEmail.get(email.toLowerCase());

        if (user == null) {
            FileHandler.logAudit("LOGIN_FAILED", "Unknown email: " + email);
            throw new InvalidInputException("Invalid email or password.", "credentials");
        }

        if (user.isAccountLocked()) {
            FileHandler.logAudit("LOGIN_LOCKED", "Attempt on locked account: " + email);
            throw new AccountLockedException(
                "Account is locked due to multiple failed login attempts. Contact administrator.",
                email);
        }

        if (!user.validatePassword(password)) {
            int remaining = User.MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
            FileHandler.logAudit("LOGIN_FAILED", "Invalid password for: " + email + 
                " (Attempts remaining: " + remaining + ")");

            if (user.isAccountLocked()) {
                // Save updated data
                saveAllUsers();

                // Trigger fraud alert on account lockout
                FileHandler.logAudit("SECURITY_LOCKOUT",
                        "Account locked after " + User.MAX_FAILED_ATTEMPTS + " failed attempts: " + email);
                if (fraudDetectionService != null) {
                    fraudDetectionService.flagSuspiciousActivity(
                            user.getUserId(), "N/A",
                            "Account locked after " + User.MAX_FAILED_ATTEMPTS +
                            " consecutive failed login attempts from email: " + email,
                            "REPEATED_LOGIN_FAILURE");
                }
                if (notificationService != null) {
                    notificationService.sendNotification(user.getUserId(),
                            com.rajarata.bank.models.notification.AlertType.SECURITY_ALERT,
                            "Account Locked — Security Alert",
                            "Your account has been locked after " + User.MAX_FAILED_ATTEMPTS +
                            " failed login attempts. If this was not you, contact the bank immediately.");
                    notificationService.broadcastToStaff(
                            com.rajarata.bank.models.notification.AlertType.FRAUD_ALERT,
                            "Account Lockout Alert",
                            "User " + user.getUserId() + " (" + email +
                            ") account locked after " + User.MAX_FAILED_ATTEMPTS + " failed login attempts.");
                }

                throw new AccountLockedException(
                    "Account has been locked after " + User.MAX_FAILED_ATTEMPTS + " failed attempts.",
                    email);
            }

            throw new InvalidInputException(
                "Invalid email or password. " + remaining + " attempt(s) remaining.", "credentials");
        }

        // Successful login
        currentUser = user;
        FileHandler.logAudit("LOGIN_SUCCESS", "User logged in: " + user.getUserId() + " (" + email + ") Role: " + user.getRole());
        saveAllUsers();

        return user;
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        if (currentUser != null) {
            FileHandler.logAudit("LOGOUT", "User logged out: " + currentUser.getUserId());
            currentUser.logout();
            currentUser = null;
        }
    }

    // ==================== PASSWORD MANAGEMENT ====================

    /**
     * Changes the password for the current user.
     * 
     * @param oldPassword The current password
     * @param newPassword The new password
     * @return true if password was changed successfully
     * @throws InvalidInputException if validation fails
     */
    public boolean changePassword(String oldPassword, String newPassword) throws InvalidInputException {
        if (currentUser == null) {
            throw new InvalidInputException("No user is currently logged in.", "session");
        }
        if (!ValidationUtil.isValidPassword(newPassword)) {
            throw new InvalidInputException(ValidationUtil.getPasswordRequirements(), "newPassword");
        }

        boolean changed = currentUser.changePassword(oldPassword, newPassword);
        if (changed) {
            saveAllUsers();
            FileHandler.logAudit("PASSWORD_CHANGE", "Password changed for: " + currentUser.getUserId());
        }
        return changed;
    }

    /**
     * Recovers a user's account using security question.
     * 
     * @param email The user's email
     * @param securityAnswer Answer to the security question
     * @param newPassword New password to set
     * @return true if recovery was successful
     * @throws InvalidInputException if validation fails
     */
    public boolean recoverPassword(String email, String securityAnswer, String newPassword) 
            throws InvalidInputException {
        User user = usersByEmail.get(email.toLowerCase());
        if (user == null) {
            throw new InvalidInputException("No account found for this email.", "email");
        }
        if (!ValidationUtil.isValidPassword(newPassword)) {
            throw new InvalidInputException(ValidationUtil.getPasswordRequirements(), "newPassword");
        }

        boolean recovered = user.recoverPassword(securityAnswer, newPassword);
        if (recovered) {
            saveAllUsers();
            FileHandler.logAudit("PASSWORD_RECOVERY", "Password recovered for: " + user.getUserId());
        }
        return recovered;
    }

    /**
     * Gets the security question for an email (for password recovery).
     * @param email The user's email
     * @return The security question, or null if not found
     */
    public String getSecurityQuestion(String email) {
        User user = usersByEmail.get(email.toLowerCase());
        return user != null ? user.getSecurityQuestion() : null;
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * Locks a user account (admin function).
     * @param userId The user ID to lock
     * @return true if lock was successful
     */
    public boolean lockUser(String userId) throws UnauthorizedAccessException {
        if (currentUser == null || !"Administrator".equals(currentUser.getRole())) {
            FileHandler.logAudit("UNAUTHORIZED_ACCESS",
                    "Non-admin attempted to lock account " + userId +
                    " (by: " + (currentUser != null ? currentUser.getUserId() : "unauthenticated") + ")");
            throw new UnauthorizedAccessException("Only administrators can lock accounts");
        }
        User user = usersById.get(userId);
        if (user != null) {
            user.setAccountLocked(true);
            saveAllUsers();
            FileHandler.logAudit("ACCOUNT_LOCK", "Account locked: " + userId + " by " + currentUser.getUserId());
            return true;
        }
        return false;
    }

    /**
     * Unlocks a user account (admin function).
     * @param userId The user ID to unlock
     * @return true if unlock was successful
     */
    public boolean unlockUser(String userId) throws UnauthorizedAccessException {
        if (currentUser == null || !"Administrator".equals(currentUser.getRole())) {
            FileHandler.logAudit("UNAUTHORIZED_ACCESS",
                    "Non-admin attempted to unlock account " + userId +
                    " (by: " + (currentUser != null ? currentUser.getUserId() : "unauthenticated") + ")");
            throw new UnauthorizedAccessException("Only administrators can unlock accounts");
        }
        User user = usersById.get(userId);
        if (user != null) {
            user.unlockAccount();
            saveAllUsers();
            FileHandler.logAudit("ACCOUNT_UNLOCK", "Account unlocked: " + userId + " by " + currentUser.getUserId());
            return true;
        }
        return false;
    }

    /**
     * Resets a user's password (admin function).
     * @param userId The user ID
     * @param newPassword The new password to set
     * @return true if reset was successful
     */
    public boolean resetUserPassword(String userId, String newPassword) throws UnauthorizedAccessException {
        if (currentUser == null || !"Administrator".equals(currentUser.getRole())) {
            FileHandler.logAudit("UNAUTHORIZED_ACCESS",
                    "Non-admin attempted to reset password for " + userId +
                    " (by: " + (currentUser != null ? currentUser.getUserId() : "unauthenticated") + ")");
            throw new UnauthorizedAccessException("Only administrators can reset passwords");
        }
        User user = usersById.get(userId);
        if (user != null) {
            user.setPasswordHash(EncryptionUtil.hashPassword(newPassword));
            user.unlockAccount();
            saveAllUsers();
            FileHandler.logAudit("PASSWORD_RESET", "Password reset for: " + userId + " by " + currentUser.getUserId());
            return true;
        }
        return false;
    }

    // ==================== DATA LOADING ====================

    /**
     * Loads all user data from file into memory.
     * Called at application startup.
     */
    public void loadUsers() {
        List<String> lines = FileHandler.readAllLines(FileHandler.CUSTOMERS_FILE);
        int maxCustId = 1000, maxStaffId = 100, maxAdminId = 10;

        for (String line : lines) {
            try {
                String[] parts = line.split(FileHandler.DELIMITER_REGEX, -1);
                if (parts.length < 16) continue;

                String userId = parts[0];
                String role = parts[15];
                User user;

                switch (role) {
                    case "Customer":
                        Customer customer = new Customer();
                        customer.setUserId(userId);
                        customer.setCustomerId(userId);
                        user = customer;
                        try {
                            int num = Integer.parseInt(userId.replace("CUST-", ""));
                            maxCustId = Math.max(maxCustId, num);
                        } catch (NumberFormatException e) { /* ignore */ }
                        break;

                    case "Staff":
                        Staff staff = new Staff();
                        staff.setUserId(userId);
                        staff.setEmployeeId(userId);
                        user = staff;
                        try {
                            int num = Integer.parseInt(userId.replace("STAFF-", ""));
                            maxStaffId = Math.max(maxStaffId, num);
                        } catch (NumberFormatException e) { /* ignore */ }
                        break;

                    case "Administrator":
                        Administrator admin = new Administrator();
                        admin.setUserId(userId);
                        user = admin;
                        try {
                            int num = Integer.parseInt(userId.replace("ADMIN-", ""));
                            maxAdminId = Math.max(maxAdminId, num);
                        } catch (NumberFormatException e) { /* ignore */ }
                        break;

                    default:
                        continue;
                }

                // Set common fields
                user.setFullName(parts[1]);
                user.setEmail(parts[2]);
                user.setPhone(parts[3]);
                user.setAddress(parts[4]);
                user.setDateOfBirth(parts[5]);
                user.setGovernmentId(parts[6]);
                user.setPasswordHash(parts[7]);
                if (parts[8] != null && !parts[8].isEmpty()) user.setSecurityQuestion(parts[8]);
                if (parts[9] != null && !parts[9].isEmpty()) user.setSecurityAnswerHash(parts[9]);
                user.setEmailVerified(Boolean.parseBoolean(parts[10]));
                user.setAccountLocked(Boolean.parseBoolean(parts[11]));
                user.setFailedLoginAttempts(Integer.parseInt(parts[12]));
                if (parts[13] != null && !parts[13].isEmpty()) user.setCreationDate(parts[13]);
                if (parts[14] != null && !parts[14].isEmpty()) user.setLastLoginDate(parts[14]);

                usersByEmail.put(user.getEmail().toLowerCase(), user);
                usersById.put(user.getUserId(), user);

            } catch (Exception e) {
                System.err.println("Warning: Failed to load user record: " + e.getMessage());
            }
        }

        // Update counters
        Customer.setCustomerCounter(maxCustId);
        Staff.setStaffCounter(maxStaffId);
        Administrator.setAdminCounter(maxAdminId);
    }

    /**
     * Saves all user data to file.
     * Hardcoded admin accounts are excluded — they only exist in code.
     */
    public void saveAllUsers() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (User user : usersById.values()) {
            // Skip hardcoded admins — they are never persisted
            if (hardcodedUserIds.contains(user.getUserId())) continue;
            lines.add(user.toFileString());
        }
        FileHandler.writeAllLines(FileHandler.CUSTOMERS_FILE, lines);
    }

    /**
     * Creates the hardcoded administrator accounts.
     * These accounts exist ONLY in code and are never saved to the data file.
     * Three levels of admin access are provided:
     *   Level 1 (Basic)  — limited admin, can view reports
     *   Level 2 (Full)   — full admin, can manage users and system
     *   Level 3 (Super)  — super admin, can manage other admins
     */
    public void createDefaultAdmin() {
        // ========== LEVEL 3 — SUPER ADMIN ==========
        // Has full control including managing other admins
        if (!usersByEmail.containsKey("superadmin@rajarata.com")) {
            Administrator superAdmin = new Administrator(
                "Super Administrator", "superadmin@rajarata.com", "+94771000001",
                "Rajarata Digital Bank HQ, Colombo, Sri Lanka",
                "1980-01-01", "SADMIN00001", "Super@123", 3);
            superAdmin.verifyEmail();
            superAdmin.setSecurityQuestion("What is the bank name?", "Rajarata");
            usersByEmail.put("superadmin@rajarata.com", superAdmin);
            usersById.put(superAdmin.getUserId(), superAdmin);
            hardcodedUserIds.add(superAdmin.getUserId());
        }

        // ========== LEVEL 2 — FULL ADMIN ==========
        // Can manage users, system settings, but cannot manage other admins
        if (!usersByEmail.containsKey("admin@rajarata.com")) {
            Administrator fullAdmin = new Administrator(
                "Full Administrator", "admin@rajarata.com", "+94771000002",
                "Rajarata Digital Bank HQ, Colombo, Sri Lanka",
                "1982-06-15", "FADMIN00002", "Admin@123", 2);
            fullAdmin.verifyEmail();
            fullAdmin.setSecurityQuestion("What is the bank name?", "Rajarata");
            usersByEmail.put("admin@rajarata.com", fullAdmin);
            usersById.put(fullAdmin.getUserId(), fullAdmin);
            hardcodedUserIds.add(fullAdmin.getUserId());
        }

        // ========== LEVEL 1 — BASIC ADMIN ==========
        // Can view reports and basic system monitoring only
        if (!usersByEmail.containsKey("basicadmin@rajarata.com")) {
            Administrator basicAdmin = new Administrator(
                "Basic Administrator", "basicadmin@rajarata.com", "+94771000003",
                "Rajarata Digital Bank HQ, Colombo, Sri Lanka",
                "1985-03-20", "BADMIN00003", "Basic@123", 1);
            basicAdmin.verifyEmail();
            basicAdmin.setSecurityQuestion("What is the bank name?", "Rajarata");
            usersByEmail.put("basicadmin@rajarata.com", basicAdmin);
            usersById.put(basicAdmin.getUserId(), basicAdmin);
            hardcodedUserIds.add(basicAdmin.getUserId());
        }

        System.out.println("  Admin accounts loaded (hardcoded):");
        System.out.println("    Level 3 (Super):  superadmin@rajarata.com / Super@123");
        System.out.println("    Level 2 (Full):   admin@rajarata.com     / Admin@123");
        System.out.println("    Level 1 (Basic):  basicadmin@rajarata.com / Basic@123");
    }

    // ==================== GETTERS ====================

    /** @return The currently logged-in user */
    public User getCurrentUser() { return currentUser; }

    /** @return User by user ID */
    public User getUserById(String userId) { return usersById.get(userId); }

    /** @return User by email */
    public User getUserByEmail(String email) { return usersByEmail.get(email.toLowerCase()); }

    /** @return Map of all users by ID */
    public Map<String, User> getAllUsersById() { return new HashMap<>(usersById); }

    /** @return Map of all users by email */
    public Map<String, User> getAllUsersByEmail() { return new HashMap<>(usersByEmail); }

    /** @return Count of all registered users */
    public int getUserCount() { return usersById.size(); }

    /**
     * Gets a customer by ID (cast from User).
     * @param customerId The customer ID
     * @return Customer object, or null if not found or not a customer
     */
    public Customer getCustomer(String customerId) {
        User user = usersById.get(customerId);
        if (user instanceof Customer) {
            return (Customer) user;
        }
        return null;
    }
}
