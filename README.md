# Rajarata Digital Bank (OOP Mini Project)

A comprehensive, professional-grade digital banking application developed in **Pure Java** and **JavaFX**. This project was engineered to demonstrate a deep mastery of **Object-Oriented Programming (OOP)** principles while solving real-world challenges in secure financial management.

---

## 🚀 Key Features

- **Multi-Role Access**: Specialized dashboards for Customers, Staff, and Administrators.
- **Dynamic Account Management**: Create and manage Savings, Checking, and Student accounts via a centralized **Account Factory**.
- **Secure Transactions**: Deposit, withdraw, and transfer funds with real-time balance updates and currency conversion fees.
- **Automated Fraud Detection**: Intelligent heuristics that monitor for large transactions and rapid successive withdrawals.
- **Financial Reporting**: Generate monthly statements and audit logs in professional ASCII-table format.
- **Notifications & Alerts**: A real-time notification engine using the **Observer Pattern** logic.
- **Robust Data Persistence**: Complete file-based database using a specialized **FileHandler** with pipe-delimited storage.

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

## 💻 How to Run

### **Graphical Version (GUI)**

1. Ensure Java 20+ is installed on your machine.
2. Double-click `run-gui.bat` to compile and launch the **JavaFX** interface.

### **Console Version (CLI)**

1. Double-click `run-console.bat` to launch the **Terminal-based** interface for a lightweight experience.

### **Standalone Installation**

Method I

1. Run `build-exe.bat` to generate a professional Windows installer (`.exe`) in the `dist` folder.
2. _Note: Requires WiX Toolset to be installed._

Method II

1. **Ready-to-Use Installer**: The generated `.exe` in the `dist` folder is a self-contained setup.
2. **Zero Dependencies**: End-users do not need Java or WiX installed to run the application.

---

## 👨‍💻 Authors

**Rajarata University - Faculty of Technology - Department of ICT**

- Developed as a Mini Project for the Object-Oriented Programming Module.
