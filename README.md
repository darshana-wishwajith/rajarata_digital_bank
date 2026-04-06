# Rajarata Digital Bank (OOP Mini Project)

A comprehensive, professional-grade digital banking application developed in **Pure Java** and **JavaFX**. This project was engineered to demonstrate a deep mastery of **Object-Oriented Programming (OOP)** principles while solving real-world challenges in secure financial management.

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

*   **Multi-Role Access**: Specialized dashboards for Customers, Staff, and three levels of Administrators.
*   **Dynamic Account Management**: Create and manage Savings, Checking, and Student accounts via a centralized **Account Factory**.
*   **Secure Transactions**: Deposit, withdraw, and transfer funds with real-time balance updates and currency conversion fees.
*   **Automated Fraud Detection**: Intelligent heuristics that monitor for large transactions and security lockouts.
*   **Financial Reporting**: Generate monthly statements and audit logs in professional ASCII-table format.
*   **Notifications & Alerts**: A real-time notification engine using the **Observer Pattern** logic.
*   **Robust Data Persistence**: Complete file-based database using a specialized **FileHandler** with pipe-delimited storage.

---

## 🛠️ Technical Architecture & OOP Concepts

This project serves as a showcase for the 7 core pillars of Object-Oriented Programming:

1.  **Encapsulation**: All sensitive data is private, exposed only through strictly controlled services.
2.  **Inheritance**: Specialized user and account hierarchies derived from robust base classes.
3.  **Polymorphism**: Cross-account transaction processing and generic reporting interfaces.
4.  **Abstraction**: Abstract classes and interfaces define strict contracts for core banking logic.
5.  **Composition**: A modular service-based architecture managed by a central **Bank** singleton.
6.  **Exception Handling**: Custom error types (e.g., `InsufficientFundsException`) for graceful fail-safes.
7.  **File I/O & Persistence**: Direct flat-file management for long-term data storage without external DBs.

---

## 💻 How to Run & Install

### **Method 1: Running from Scripts**
*   **GUI Version**: Run `run-gui.bat` to compile and launch the **JavaFX** interface.
*   **Console Version**: Run `run-console.bat` to launch the lightweight **Terminal-based** interface.

### **Method 2: Standalone Installation**
*   **Build Your Own**: Run `build-exe.bat` to generate a professional Windows installer (`.exe`) in the `dist` folder. *(Requires WiX Toolset v3.11+)*.
*   **Ready-to-Use**: The generated `.exe` in the `dist` folder is a self-contained setup. End-users do not need Java or WiX installed to run the application.

---

## 👨‍💻 Authors

**Rajarata University - Faculty of Technology - Department of ICT**
*   Developed as a Mini Project for the Object-Oriented Programming Module.
