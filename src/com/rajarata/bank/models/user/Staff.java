package com.rajarata.bank.models.user;

/**
 * Represents a bank staff member who can approve loans,
 * monitor transactions, and manage customer support operations.
 * 
 * OOP Concept: Inheritance - Extends abstract User class, providing
 * staff-specific implementations of abstract methods.
 * 
 * OOP Concept: Polymorphism - Overrides getRole() and getDashboardMenu()
 * to provide staff-specific behavior.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class Staff extends User {

    // ==================== PRIVATE FIELDS ====================

    /** Unique staff employee ID */
    private String employeeId;
    /** Department the staff belongs to */
    private String department;
    /** Job position/title */
    private String position;

    /** Static counter for generating unique staff IDs */
    private static int staffCounter = 100;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor.
     */
    public Staff() {
        super();
    }

    /**
     * Parameterized constructor for creating a new staff member.
     * 
     * @param fullName Staff member's full name
     * @param email Staff member's email
     * @param phone Phone number
     * @param address Physical address
     * @param dateOfBirth Date of birth
     * @param governmentId Government ID
     * @param password Plain text password
     * @param department Department name
     * @param position Job position
     */
    public Staff(String fullName, String email, String phone, String address,
                 String dateOfBirth, String governmentId, String password,
                 String department, String position) {
        super(generateStaffId(), fullName, email, phone, address, dateOfBirth, governmentId, password);
        this.employeeId = getUserId();
        this.department = department;
        this.position = position;
    }

    // ==================== ABSTRACT METHOD IMPLEMENTATIONS ====================

    /**
     * {@inheritDoc}
     * OOP Concept: Polymorphism - Staff-specific role identification.
     */
    @Override
    public String getRole() {
        return "Staff";
    }

    /**
     * {@inheritDoc}
     * OOP Concept: Polymorphism - Staff-specific dashboard menu.
     */
    @Override
    public String getDashboardMenu() {
        int unread = getUnreadNotificationCount();
        String notifBadge = unread > 0 ? " (" + unread + " new)" : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append("║         STAFF DASHBOARD                  ║\n");
        sb.append("║         Welcome, ").append(String.format("%-22s", getFullName())).append(" ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append("║  1. Review Loan Applications             ║\n");
        sb.append("║  2. View All Customers                   ║\n");
        sb.append("║  3. Search Customer                      ║\n");
        sb.append("║  4. View Customer Accounts               ║\n");
        sb.append("║  5. Monitor Transactions                 ║\n");
        sb.append("║  6. Flag Suspicious Activity             ║\n");
        sb.append("║  7. View Fraud Cases                     ║\n");
        sb.append("║  8. Transaction Reports                  ║\n");
        sb.append("║  9. Customer Activity Reports            ║\n");
        sb.append("║ 10. Notifications").append(String.format("%-22s", notifBadge)).append(" ║\n");
        sb.append("║ 11. My Profile                           ║\n");
        sb.append("║ 12. Change Password                      ║\n");
        sb.append("║  0. Logout                               ║\n");
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Serializes the staff to a delimited string.
     * Overrides User.toFileString() to add staff-specific fields.
     */
    @Override
    public String toFileString() {
        return super.toFileString() + "|" + 
               (department != null ? department : "") + "|" +
               (position != null ? position : "");
    }

    // ==================== STATIC METHODS ====================

    /**
     * Generates a unique staff ID with format STAFF-XXX.
     * @return A unique staff ID string
     */
    private static synchronized String generateStaffId() {
        staffCounter++;
        return String.format("STAFF-%03d", staffCounter);
    }

    /**
     * Sets the staff counter for data loading.
     * @param counter The counter value to set
     */
    public static void setStaffCounter(int counter) {
        staffCounter = counter;
    }

    // ==================== GETTERS AND SETTERS ====================

    /** @return The staff employee ID */
    public String getEmployeeId() { return employeeId; }
    /** @param employeeId The employee ID to set */
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    /** @return The department name */
    public String getDepartment() { return department; }
    /** @param department The department to set */
    public void setDepartment(String department) { this.department = department; }

    /** @return The job position */
    public String getPosition() { return position; }
    /** @param position The position to set */
    public void setPosition(String position) { this.position = position; }
}

