package com.rajarata.bank.services;

import com.rajarata.bank.models.account.*;
import com.rajarata.bank.models.user.Customer;
import com.rajarata.bank.models.transaction.*;
import com.rajarata.bank.exceptions.*;
import com.rajarata.bank.factory.AccountFactory;
import com.rajarata.bank.utils.*;

import java.util.*;

/**
 * Service class for account management operations.
 * Handles account creation, closure, status management, and data persistence.
 * 
 * OOP Concept: Encapsulation - All account management business logic is
 * encapsulated here, providing a clean service API.
 * 
 * OOP Concept: Polymorphism - Works with Account base type, processing
 * different account subtypes uniformly.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class AccountService {

    /** Map of all accounts by account number */
    private final Map<String, Account> accountsByNumber;
    /** Reference to authentication service for customer lookup */
    private final AuthenticationService authService;

    /**
     * Constructor.
     * @param authService Reference to the authentication service
     */
    public AccountService(AuthenticationService authService) {
        this.accountsByNumber = new HashMap<>();
        this.authService = authService;
    }

    /**
     * Opens a new bank account for a customer.
     * Uses the Factory pattern to create the appropriate account type.
     * 
     * OOP Concept: Factory Pattern - Account creation is delegated to AccountFactory,
     * which returns the correct subclass based on the type string.
     * 
     * @param customerId The customer's ID
     * @param accountType Type of account to create
     * @param initialDeposit Initial deposit amount
     * @param currency Account currency
     * @param lockInMonths FD lock-in period (0 for other types)
     * @return The newly created Account
     * @throws InvalidAccountException if validation fails
     * @throws InvalidInputException if customer not found
     */
    public Account openAccount(String customerId, String accountType, double initialDeposit,
                                String currency, int lockInMonths) 
            throws InvalidAccountException, InvalidInputException {

        // Validate customer exists
        Customer customer = authService.getCustomer(customerId);
        if (customer == null) {
            throw new InvalidInputException("Customer not found: " + customerId, "customerId");
        }

        // Validate minimum deposit
        double minDeposit = AccountFactory.getMinimumDeposit(accountType);
        if (initialDeposit < minDeposit) {
            throw new InvalidAccountException(
                "Minimum initial deposit for " + accountType + " account is $" + 
                ValidationUtil.formatAmount(minDeposit));
        }

        // Create account using Factory pattern
        Account account = AccountFactory.createAccount(
                accountType, customerId, initialDeposit, currency,
                customer.getDateOfBirth(), lockInMonths);

        // Link account to customer (Composition)
        customer.addAccount(account);
        accountsByNumber.put(account.getAccountNumber(), account);

        // Record initial deposit transaction
        Transaction depositTxn = new Transaction(
                TransactionType.DEPOSIT, account.getAccountNumber(), null,
                initialDeposit, account.getCurrency(), "Initial deposit - Account opened");
        depositTxn.complete(account.getBalance());
        account.addTransaction(depositTxn);

        // Save data
        saveAllAccounts();
        FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, depositTxn.toFileString());
        FileHandler.logAudit("ACCOUNT_OPENED",
                "Account " + account.getAccountNumber() + " (" + accountType + ") opened for " + customerId +
                " with initial deposit $" + ValidationUtil.formatAmount(initialDeposit));

        return account;
    }

    /**
     * Closes a bank account with balance settlement.
     * 
     * @param accountNumber The account to close
     * @param customerId The owning customer's ID (for verification)
     * @return The remaining balance that was settled
     * @throws InvalidAccountException if account not found or not owned by customer
     */
    public double closeAccount(String accountNumber, String customerId) 
            throws InvalidAccountException {

        Account account = accountsByNumber.get(accountNumber);
        if (account == null) {
            throw new InvalidAccountException("Account not found", accountNumber);
        }
        if (!account.getCustomerId().equals(customerId)) {
            throw new InvalidAccountException("Account does not belong to this customer", accountNumber);
        }
        if ("Closed".equals(account.getStatus())) {
            throw new InvalidAccountException("Account is already closed", accountNumber);
        }

        double remainingBalance = account.getBalance();
        account.setBalance(0);
        account.setStatus("Closed");

        // Record closing transaction
        if (remainingBalance > 0) {
            Transaction closeTxn = new Transaction(
                    TransactionType.WITHDRAWAL, accountNumber, null,
                    remainingBalance, account.getCurrency(), "Account closure - Balance settlement");
            closeTxn.complete(0);
            account.addTransaction(closeTxn);
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, closeTxn.toFileString());
        }

        saveAllAccounts();
        FileHandler.logAudit("ACCOUNT_CLOSED",
                "Account " + accountNumber + " closed. Balance settled: $" + 
                ValidationUtil.formatAmount(remainingBalance));

        return remainingBalance;
    }

    /**
     * Suspends an account (admin/staff function).
     * @param accountNumber The account to suspend
     */
    public void suspendAccount(String accountNumber) throws InvalidAccountException {
        Account account = getAccount(accountNumber);
        account.setStatus("Suspended");
        saveAllAccounts();
        FileHandler.logAudit("ACCOUNT_SUSPENDED", "Account suspended: " + accountNumber);
    }

    /**
     * Reactivates a suspended account.
     * @param accountNumber The account to reactivate
     */
    public void reactivateAccount(String accountNumber) throws InvalidAccountException {
        Account account = getAccount(accountNumber);
        if (!"Suspended".equals(account.getStatus())) {
            throw new InvalidAccountException("Account is not suspended", accountNumber);
        }
        account.setStatus("Active");
        saveAllAccounts();
        FileHandler.logAudit("ACCOUNT_REACTIVATED", "Account reactivated: " + accountNumber);
    }

    /**
     * Gets an account by account number.
     * @param accountNumber The account number to look up
     * @return The Account object
     * @throws InvalidAccountException if not found
     */
    public Account getAccount(String accountNumber) throws InvalidAccountException {
        Account account = accountsByNumber.get(accountNumber);
        if (account == null) {
            throw new InvalidAccountException("Account not found: " + accountNumber, accountNumber);
        }
        return account;
    }

    /**
     * Gets an account without throwing exception (returns null if not found).
     * @param accountNumber The account number
     * @return The Account or null
     */
    public Account getAccountOrNull(String accountNumber) {
        return accountsByNumber.get(accountNumber);
    }

    /**
     * Gets all accounts for a specific customer.
     * @param customerId The customer ID
     * @return List of accounts
     */
    public List<Account> getCustomerAccounts(String customerId) {
        List<Account> result = new ArrayList<>();
        for (Account account : accountsByNumber.values()) {
            if (account.getCustomerId().equals(customerId)) {
                result.add(account);
            }
        }
        return result;
    }

    /**
     * Gets all accounts in the system (admin function).
     * @return Map of all accounts
     */
    public Map<String, Account> getAllAccounts() {
        return new HashMap<>(accountsByNumber);
    }

    /**
     * Gets the total number of accounts in the system.
     * @return Account count
     */
    public int getAccountCount() {
        return accountsByNumber.size();
    }

    // ==================== DATA PERSISTENCE ====================

    /**
     * Loads all account data from file.
     */
    public void loadAccounts() {
        accountsByNumber.clear();
        // Clear accounts from customers to avoid duplicates during reload
        for (com.rajarata.bank.models.user.User user : authService.getAllUsersById().values()) {
            if (user instanceof com.rajarata.bank.models.user.Customer) {
                ((com.rajarata.bank.models.user.Customer) user).getAccounts().clear();
            }
        }
        List<String> lines = FileHandler.readAllLines(FileHandler.ACCOUNTS_FILE);
        int maxSeq = 0;

        for (String line : lines) {
            try {
                String[] parts = line.split(FileHandler.DELIMITER_REGEX, -1);
                if (parts.length < 10) continue;

                String accountType = parts[2];
                Account account = AccountFactory.createEmptyAccount(accountType);

                account.setAccountNumber(parts[0]);
                account.setCustomerId(parts[1]);
                account.setBalance(Double.parseDouble(parts[3]));
                account.setOpenDate(parts[4]);
                account.setStatus(parts[5]);
                account.setCurrency(parts[6]);
                account.setMonthlyWithdrawalCount(Integer.parseInt(parts[7]));
                if (!parts[8].isEmpty()) account.setLastInterestDate(parts[8]);
                account.setTotalInterestEarned(Double.parseDouble(parts[9]));

                // Load extra fields for specific types
                if (account instanceof FixedDepositAccount && parts.length > 13) {
                    FixedDepositAccount fd = (FixedDepositAccount) account;
                    fd.setPrincipalAmount(Double.parseDouble(parts[10]));
                    fd.setLockInMonths(Integer.parseInt(parts[11]));
                    fd.setMaturityDate(parts[12]);
                    fd.setMatured(Boolean.parseBoolean(parts[13]));
                } else if (account instanceof CheckingAccount && parts.length > 10) {
                    CheckingAccount ca = (CheckingAccount) account;
                    if (!parts[10].isEmpty()) {
                        ca.setOverdraftUsed(Double.parseDouble(parts[10]));
                    }
                } else if (account instanceof StudentAccount && parts.length > 11) {
                    StudentAccount sa = (StudentAccount) account;
                    if (!parts[10].isEmpty()) sa.setInstitution(parts[10]);
                    if (!parts[11].isEmpty()) sa.setStudentId(parts[11]);
                }

                accountsByNumber.put(account.getAccountNumber(), account);

                // Link to customer
                Customer customer = authService.getCustomer(account.getCustomerId());
                if (customer != null) {
                    customer.addAccount(account);
                }

                // Track sequence
                try {
                    String seqStr = account.getAccountNumber().split("-")[1];
                    int seq = Integer.parseInt(seqStr);
                    maxSeq = Math.max(maxSeq, seq);
                } catch (Exception e) { /* ignore */ }

            } catch (Exception e) {
                System.err.println("Warning: Failed to load account record: " + e.getMessage());
            }
        }

        Account.setAccountSequence(maxSeq);

        // Load transactions and link to accounts
        loadTransactions();
    }

    /**
     * Loads transactions from file and links them to accounts.
     */
    private void loadTransactions() {
        List<String> lines = FileHandler.readAllLines(FileHandler.TRANSACTIONS_FILE);
        int maxTxnCounter = 0;

        for (String line : lines) {
            try {
                String[] parts = line.split(FileHandler.DELIMITER_REGEX, -1);
                if (parts.length < 10) continue;

                Transaction txn = new Transaction(
                        parts[0], // transactionId
                        parts[1], // timestamp
                        TransactionType.valueOf(parts[2]), // type
                        parts[3].isEmpty() ? null : parts[3], // sourceAccount
                        parts[4].isEmpty() ? null : parts[4], // destinationAccount
                        Double.parseDouble(parts[5]), // amount
                        parts[6], // currency
                        TransactionStatus.valueOf(parts[7]), // status
                        parts[8], // description
                        Double.parseDouble(parts[9]) // balanceAfter
                );

                // Link to account history - ONLY if transaction was COMPLETED
                // Failed/Pending transactions stay in logs but don't show in account history
                if (txn.getSourceAccount() != null && txn.getStatus() == TransactionStatus.COMPLETED) {
                    Account acc = accountsByNumber.get(txn.getSourceAccount());
                    if (acc != null) {
                        acc.addTransaction(txn);
                    }
                }

                // Track counter for ID generation
                try {
                    String numStr = parts[0].substring(parts[0].lastIndexOf('-') + 1);
                    int num = Integer.parseInt(numStr);
                    maxTxnCounter = Math.max(maxTxnCounter, num);
                } catch (Exception e) { /* ignore */ }

            } catch (Exception e) {
                System.err.println("Warning: Failed to load transaction: " + e.getMessage());
            }
        }

        Transaction.setTransactionCounter(maxTxnCounter);
    }

    /**
     * Saves all account data to file.
     */
    public void saveAllAccounts() {
        List<String> lines = new ArrayList<>();
        for (Account account : accountsByNumber.values()) {
            lines.add(account.toFileString());
        }
        FileHandler.writeAllLines(FileHandler.ACCOUNTS_FILE, lines);
    }
}

