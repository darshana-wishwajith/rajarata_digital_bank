package com.rajarata.bank.models.user;

/**
 * Represents a system administrator with full system oversight,
 * user management, and compliance reporting capabilities.
 * 
 * OOP Concept: Inheritance - Extends abstract User class.
 * Multi-level inheritance: User -> Administrator
 * 
 * OOP Concept: Polymorphism - Overrides getRole() and getDashboardMenu()
 * to provide admin-specific behavior with the most privileged access.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class Administrator extends User {

    // ==================== PRIVATE FIELDS ====================

    /** Admin's access level (1=Basic, 2=Full, 3=Super Admin) */
    private int accessLevel;
    /** Whether this admin can manage other admins */
    private boolean canManageAdmins;

    /** Static counter for generating unique admin IDs */
    private static int adminCounter = 10;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor.
     */
    public Administrator() {
        super();
        this.accessLevel = 3;
        this.canManageAdmins = false;
    }

    /**
     * Parameterized constructor for creating a new administrator.
     * 
     * @param fullName Admin's full name
     * @param email Admin's email
     * @param phone Phone number
     * @param address Physical address
     * @param dateOfBirth Date of birth
     * @param governmentId Government ID
     * @param password Plain text password
     * @param accessLevel Admin access level (1-3)
     */
    public Administrator(String fullName, String email, String phone, String address,
                         String dateOfBirth, String governmentId, String password,
                         int accessLevel) {
        super(generateAdminId(), fullName, email, phone, address, dateOfBirth, governmentId, password);
        this.accessLevel = accessLevel;
        this.canManageAdmins = accessLevel >= 3;
    }

    // ==================== ABSTRACT METHOD IMPLEMENTATIONS ====================

    /**
     * {@inheritDoc}
     * OOP Concept: Polymorphism - Admin-specific role identification.
     */
    @Override
    public String getRole() {
        return "Administrator";
    }

    /**
     * {@inheritDoc}
     * OOP Concept: Polymorphism - Admin-specific dashboard with full system access.
     */
    @Override
    public String getDashboardMenu() {
        int unread = getUnreadNotificationCount();
        String notifBadge = unread > 0 ? " (" + unread + " new)" : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append("║       ADMINISTRATOR DASHBOARD            ║\n");
        sb.append("║       Welcome, ").append(String.format("%-24s", getFullName())).append(" ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append("║  --- User Management ---                 ║\n");
        sb.append("║  1. View All Users                       ║\n");
        sb.append("║  2. Create Staff Account                 ║\n");
        sb.append("║  3. Lock/Unlock User Account             ║\n");
        sb.append("║  4. Reset User Password                  ║\n");
        sb.append("║  --- System Management ---               ║\n");
        sb.append("║  5. View System Statistics               ║\n");
        sb.append("║  6. Manage Exchange Rates                ║\n");
        sb.append("║  7. View/Manage Fraud Cases              ║\n");
        sb.append("║  8. Freeze/Unfreeze Account              ║\n");
        sb.append("║  --- Reports ---                         ║\n");
        sb.append("║  9. Customer Activity Report             ║\n");
        sb.append("║ 10. Transaction Analysis Report          ║\n");
        sb.append("║ 11. Loan Performance Report              ║\n");
        sb.append("║ 12. Compliance & Audit Report            ║\n");
        sb.append("║  --- Other ---                           ║\n");
        sb.append("║ 13. Notifications").append(String.format("%-22s", notifBadge)).append(" ║\n");
        sb.append("║ 14. My Profile                           ║\n");
        sb.append("║ 15. Change Password                      ║\n");
        sb.append("║  0. Logout                               ║\n");
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Serializes the administrator to a delimited string.
     * Overrides User.toFileString() to add admin-specific fields.
     */
    @Override
    public String toFileString() {
        return super.toFileString() + "|" + accessLevel;
    }

    // ==================== STATIC METHODS ====================

    /**
     * Generates a unique admin ID with format ADMIN-XX.
     * @return A unique admin ID string
     */
    private static synchronized String generateAdminId() {
        adminCounter++;
        return String.format("ADMIN-%02d", adminCounter);
    }

    /**
     * Sets the admin counter for data loading.
     * @param counter The counter value to set
     */
    public static void setAdminCounter(int counter) {
        adminCounter = counter;
    }

    // ==================== GETTERS AND SETTERS ====================

    /** @return The admin's access level */
    public int getAccessLevel() { return accessLevel; }
    /** @param accessLevel The access level to set */
    public void setAccessLevel(int accessLevel) { this.accessLevel = accessLevel; }

    /** @return Whether this admin can manage other admins */
    public boolean isCanManageAdmins() { return canManageAdmins; }
    /** @param canManageAdmins The admin management flag to set */
    public void setCanManageAdmins(boolean canManageAdmins) { this.canManageAdmins = canManageAdmins; }
}

