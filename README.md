# Rajarata Digital Bank (OOP Mini Project)

A professional-grade digital banking application developed in **Pure Java** and **JavaFX**. This project demonstrates advanced **Object-Oriented Programming (OOP)** principles while providing a secure, feature-rich financial management system.

---

## 🔐 Default Administrator Credentials

Use the following hardcoded accounts to access the Administrator Dashboards. Note that these accounts exist only in the code for security and are not stored in the data files.

| Role | Email / Username | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **Super Administrator** | `superadmin@rajarata.com` | `Super@123` | Level 3 (Full Control) |
| **Full Administrator** | `admin@rajarata.com` | `Admin@123` | Level 2 (User Management) |
| **Basic Administrator** | `basicadmin@rajarata.com` | `Basic@123` | Level 1 (Reports Only) |

---

## 🚀 Key Features

*   **Multi-Role Access**: Dashboards for Customers, Staff, and 3 levels of Administrators.
*   **Dynamic Account Management**: Create Savings, Checking, and Student accounts.
*   **Secure Transactions**: Deposit, withdraw, and transfer funds with conversion fees.
*   **Automated Fraud Detection**: Intelligent heuristics and security lockout system.
*   **Financial Reporting**: Professional ASCII statements and audit logs.
*   **Notifications**: Real-time alerts using the Observer-style notification engine.
*   **Data Persistence**: Flat-file database using specialized pipe-delimited storage.

---

## 💻 How to Run

1.  **GUI Version**: Run `run-gui.bat` to launch the JavaFX application.
2.  **Console Version**: Run `run-console.bat` for the lightweight CLI interface.
3.  **Standalone Installer**: Run `build-exe.bat` to generate a Windows `.exe` setup file in the `dist` folder.

---

## 🛠️ Technical Architecture

*   **Language**: Java 20+
*   **UI Framework**: JavaFX 24 SDK (Included in `lib/`)
*   **Design Patterns**: Singleton, Factory, and Observer logic.
*   **Security**: SHA-256 password hashing and encrypted security recovery.
