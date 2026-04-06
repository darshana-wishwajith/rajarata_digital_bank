package com.rajarata.bank.services;

import com.rajarata.bank.models.account.Account;
import com.rajarata.bank.models.account.CheckingAccount;
import com.rajarata.bank.models.transaction.*;
import com.rajarata.bank.models.notification.AlertType;
import com.rajarata.bank.exceptions.*;
import com.rajarata.bank.utils.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This service handles all money movements in the bank. 
 * It manages deposits, withdrawals, and transfers between accounts.
 * 
 * OOP Concepts applied:
 * 1. Encapsulation - This class hides all transaction logic like validation, 
 *    fee calculation, and audit logging.
 * 2. Dependency Injection - It receives other required services through its 
 *    constructor or setter methods.
 * 3. Polymorphism - It works with abstract 'Account' objects to perform 
 *    core banking actions.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class TransactionService {

    private final AccountService accountService;
    /** Reference to notification service */
    private NotificationService notificationService;
    /** Reference to currency service for cross-currency transfers */
    private CurrencyService currencyService;
    /** Conversion fee rate for cross-currency transfers (0.5%) */
    private static final double CURRENCY_CONVERSION_FEE_RATE = 0.005;

    private final FraudDetectionService fraudDetectionService;

    /**
     * Constructor with dependencies.
     */
    public TransactionService(AccountService accountService, AuthenticationService authService,
                              FraudDetectionService fraudDetectionService) {
        this.accountService = accountService;
        this.fraudDetectionService = fraudDetectionService;
    }

    /**
     * Sets the notification service reference (avoids circular dependency).
     * @param notificationService The notification service
     */
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Sets the currency service reference (for cross-currency transfers).
     * @param currencyService The currency service
     */
    public void setCurrencyService(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    /**
     * Deposits funds into an account.
     * Performs validations and logs a success/failure transaction record.
     * 
     * @param accountNumber The account number
     * @param amount The amount to deposit
     * @param description Transaction description
     * @return The completed Transaction object
     * @throws InvalidAccountException if account validation fails
     */
    public Transaction deposit(String accountNumber, double amount, String description) 
            throws InvalidAccountException {
        Account account = accountService.getAccount(accountNumber);
        Transaction txn = new Transaction(TransactionType.DEPOSIT, accountNumber, null, 
                amount, account.getCurrency(), description != null ? description : "Cash deposit");
        return processDeposit(account, txn, amount, true);
    }

    /**
     * Internal helper to process a deposit. Can be used by other services.
     */
    public Transaction processDeposit(Account account, Transaction txn, double amount, boolean notify) 
            throws InvalidAccountException {
        
        if (amount <= 0) {
            throw new InvalidAccountException("Deposit amount must be positive", account.getAccountNumber());
        }

        try {
            account.deposit(amount);
            txn.complete(account.getBalance());
            account.addTransaction(txn);

            // Security check
            if (fraudDetectionService != null) {
                fraudDetectionService.checkTransaction(txn, account);
            }

            // Persistence
            accountService.saveAllAccounts();
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, txn.toFileString());
            FileHandler.logAudit(txn.getType().name(),
                    "Amount: " + account.getCurrency() + " " + ValidationUtil.formatAmount(amount) + 
                    " | Account: " + account.getAccountNumber());

            // Notify user
            if (notify) {
                sendNotification(account.getCustomerId(),
                        AlertType.TRANSACTION_SUCCESS,
                        "Funds Deposited",
                        account.getCurrency() + " " + ValidationUtil.formatAmount(amount) + " deposited to " +
                        account.getAccountNumber() + ". New balance: " + account.getCurrency() + " " + ValidationUtil.formatAmount(account.getBalance()));
            }

        } catch (InvalidAccountException e) {
            txn.fail(e.getMessage());
            FileHandler.logAudit(txn.getType().name() + "_FAILED", 
                    "Failed deposit to " + account.getAccountNumber() + ": " + e.getMessage());
            
            if (notify) {
                sendNotification(account.getCustomerId(),
                        AlertType.TRANSACTION_FAILED,
                        "Deposit Failed",
                        "Failed to deposit " + account.getCurrency() + " " + ValidationUtil.formatAmount(amount) +
                        ". Reason: " + e.getMessage());
            }
            throw e;
        }

        return txn;
    }

    /**
     * Processes a withdrawal transaction.
     * 
     * @param accountNumber The account to withdraw from
     * @param amount The withdrawal amount
     * @param description Transaction description
     * @return The completed Transaction object
     * @throws InsufficientFundsException if balance is insufficient
     * @throws InvalidAccountException if account validation fails
     */
    public Transaction withdraw(String accountNumber, double amount, String description) 
            throws InsufficientFundsException, InvalidAccountException {
        Account account = accountService.getAccount(accountNumber);
        Transaction txn = new Transaction(TransactionType.WITHDRAWAL, accountNumber, amount, 
                account.getCurrency(), description != null ? description : "Cash withdrawal");
        return processWithdrawal(account, txn, amount, true);
    }

    /**
     * Internal helper to process a withdrawal. Can be used by other services.
     */
    public Transaction processWithdrawal(Account account, Transaction txn, double amount, boolean notify) 
            throws InsufficientFundsException, InvalidAccountException {

        if (amount <= 0) {
            throw new InvalidAccountException("Withdrawal amount must be positive", account.getAccountNumber());
        }

        try {
            account.withdraw(amount);
            txn.complete(account.getBalance());
            account.addTransaction(txn);

            // Security check
            if (fraudDetectionService != null) {
                fraudDetectionService.checkTransaction(txn, account);
            }

            accountService.saveAllAccounts();
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, txn.toFileString());
            FileHandler.logAudit(txn.getType().name(),
                    "Amount: " + account.getCurrency() + " " + ValidationUtil.formatAmount(amount) + 
                    " | Account: " + account.getAccountNumber());

            // Success notification
            if (notify) {
                sendNotification(account.getCustomerId(),
                        AlertType.TRANSACTION_SUCCESS,
                        "Funds Withdrawn",
                        account.getCurrency() + " " + ValidationUtil.formatAmount(amount) + " withdrawn from " +
                        account.getAccountNumber() + ". New balance: " + account.getCurrency() + " " + ValidationUtil.formatAmount(account.getBalance()));
            }

            // Check for overdraft usage
            checkOverdraftUsage(account);

            // Other alerts
            checkLowBalance(account);
            checkLargeTransaction(account, amount);

        } catch (InsufficientFundsException | InvalidAccountException e) {
            txn.fail(e.getMessage());
            FileHandler.logAudit(txn.getType().name() + "_FAILED",
                    "Failed from " + account.getAccountNumber() + ": " + e.getMessage());

            if (notify) {
                sendNotification(account.getCustomerId(),
                        AlertType.TRANSACTION_FAILED,
                        "Transaction Failed",
                        "Failed to process " + account.getCurrency() + " " + ValidationUtil.formatAmount(amount) +
                        ". Reason: " + e.getMessage());
            }
            throw e;
        }

        return txn;
    }

    /**
     * Processes a transfer between two accounts.
     * 
     * @param sourceAccountNumber Source account
     * @param destAccountNumber Destination account
     * @param amount Transfer amount
     * @param description Transfer description
     * @return The completed Transaction for the source account
     * @throws InsufficientFundsException if source has insufficient funds
     * @throws InvalidAccountException if either account is invalid
     */
    public Transaction transfer(String sourceAccountNumber, String destAccountNumber,
                                 double amount, String description) 
            throws InsufficientFundsException, InvalidAccountException {

        if (amount <= 0) {
            throw new InvalidAccountException("Transfer amount must be positive", sourceAccountNumber);
        }
        if (sourceAccountNumber.equals(destAccountNumber)) {
            throw new InvalidAccountException("Cannot transfer to the same account", sourceAccountNumber);
        }

        Account sourceAccount = accountService.getAccount(sourceAccountNumber);
        Account destAccount = accountService.getAccount(destAccountNumber);

        String sourceCurrency = sourceAccount.getCurrency();
        String destCurrency = destAccount.getCurrency();
        boolean isCrossCurrency = !sourceCurrency.equals(destCurrency) && currencyService != null;

        // Calculate converted amount for cross-currency transfers
        double depositAmount = amount; // amount in destination currency
        double conversionFee = 0;
        double exchangeRate = 1.0;

        if (isCrossCurrency) {
            exchangeRate = currencyService.getExchangeRate(sourceCurrency, destCurrency);
            double convertedAmount = currencyService.convert(amount, sourceCurrency, destCurrency);
            conversionFee = convertedAmount * CURRENCY_CONVERSION_FEE_RATE;
            depositAmount = convertedAmount - conversionFee;
        }

        String desc = description != null ? description : "Transfer to " + destAccountNumber;
        if (isCrossCurrency) {
            desc += String.format(" [%s→%s @%.4f, fee: %s %s]",
                    sourceCurrency, destCurrency, exchangeRate,
                    ValidationUtil.formatAmount(conversionFee), destCurrency);
        }

        // Create transactions for both sides
        Transaction sourceTxn = new Transaction(TransactionType.TRANSFER, sourceAccountNumber,
                destAccountNumber, amount, sourceCurrency, desc);
        Transaction destTxn = new Transaction(TransactionType.TRANSFER, destAccountNumber,
                sourceAccountNumber, depositAmount, destCurrency,
                "Transfer from " + sourceAccountNumber +
                (isCrossCurrency ? " [" + sourceCurrency + "→" + destCurrency + "]" : ""));

        // Set currency exchange details on source transaction for record-keeping
        if (isCrossCurrency) {
            sourceTxn.setCurrencyExchangeDetails(amount, depositAmount,
                    sourceCurrency, destCurrency);
            destTxn.setCurrencyExchangeDetails(amount, depositAmount,
                    sourceCurrency, destCurrency);
        }

        try {
            // Withdrawal from source account (base currency)
            sourceAccount.withdraw(amount);
            sourceTxn.complete(sourceAccount.getBalance());
            
            try {
                // Deposit to destination account (converted if necessary)
                destAccount.deposit(depositAmount);
                destTxn.complete(destAccount.getBalance());
                
                // Add to history only after both operations succeed
                sourceAccount.addTransaction(sourceTxn);
                destAccount.addTransaction(destTxn);
                
                // Save both accounts and log transactions
                accountService.saveAllAccounts();
                FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, sourceTxn.toFileString());
                FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, destTxn.toFileString());
                
                // Final audit and notifications
                String auditMsg = "Transfer " + sourceCurrency + " " + ValidationUtil.formatAmount(amount) +
                        " from " + sourceAccountNumber + " to " + destAccountNumber;
                if (isCrossCurrency) {
                    auditMsg += " (converted to " + ValidationUtil.formatAmount(depositAmount) +
                            " " + destCurrency + ", fee: " + ValidationUtil.formatAmount(conversionFee) +
                            " " + destCurrency + ")";
                }
                FileHandler.logAudit("TRANSFER", auditMsg);
                
                // Process notifications
                sendTransferNotifications(sourceAccount, destAccount, amount, depositAmount, 
                        sourceCurrency, destCurrency, isCrossCurrency, conversionFee);
                
                // Check overdraft and thresholds
                checkOverdraftUsage(sourceAccount);
                checkLowBalance(sourceAccount);
                checkLargeTransaction(sourceAccount, amount);
                
            } catch (Exception e) {
                // ROLLBACK: If deposit fails, refund the source account
                FileHandler.logAudit("TRANSFER_ROLLBACK", 
                    "Reversing withdrawal from " + sourceAccountNumber + " due to destination error: " + e.getMessage());
                sourceAccount.deposit(amount); 
                sourceTxn.fail("Transfer failed: " + e.getMessage());
                throw e;
            }
            
        } catch (InsufficientFundsException | InvalidAccountException e) {
            sourceTxn.fail(e.getMessage());
            FileHandler.logAudit("TRANSFER_FAILED",
                    "Failed transfer from " + sourceAccountNumber + ": " + e.getMessage());
            
            // Failure notification to sender only
            sendNotification(sourceAccount.getCustomerId(),
                    AlertType.TRANSACTION_FAILED,
                    "Transfer Failed",
                    "Transfer of " + sourceCurrency + " " + ValidationUtil.formatAmount(amount) +
                    " failed. Reason: " + e.getMessage());
            throw e;
        }

        return sourceTxn;
    }

    /**
     * Helper to send notifications for successful transfers.
     */
    private void sendTransferNotifications(Account source, Account dest, double srcAmount, double destAmount,
                                         String srcCur, String destCur, boolean crossCur, double fee) {
        String senderMsg = ValidationUtil.formatAmount(srcAmount) + " " + srcCur +
                " transferred to " + dest.getAccountNumber();
        if (crossCur) {
            senderMsg += " (" + ValidationUtil.formatAmount(destAmount) + " " + destCur +
                    " received, fee: " + ValidationUtil.formatAmount(fee) + " " + destCur + ")";
        }
        senderMsg += ". New balance: " + srcCur + " " + ValidationUtil.formatAmount(source.getBalance());
        sendNotification(source.getCustomerId(), AlertType.TRANSFER_SUCCESS, "Transfer Sent", senderMsg);

        String receiverMsg = (crossCur ? ValidationUtil.formatAmount(destAmount) + " " + destCur
                                      : ValidationUtil.formatAmount(srcAmount) + " " + srcCur)
                + " received from " + source.getAccountNumber() +
                ". New balance: " + destCur + " " + ValidationUtil.formatAmount(dest.getBalance());
        sendNotification(dest.getCustomerId(), AlertType.TRANSFER_SUCCESS, "Transfer Received", receiverMsg);
    }

    /**
     * Checks and logs overdraft usage.
     */
    private void checkOverdraftUsage(Account account) {
        if (account instanceof CheckingAccount && account.getBalance() < 0) {
            double overdraftUsed = Math.abs(account.getBalance());
            FileHandler.logAudit("OVERDRAFT_USED",
                    "Account " + account.getAccountNumber() + " is now in overdraft. Amount: " +
                    account.getCurrency() + " " + ValidationUtil.formatAmount(overdraftUsed));
            sendNotification(account.getCustomerId(),
                    AlertType.OVERDRAFT_ALERT,
                    "Overdraft Facility Used",
                    "Your checking account " + account.getAccountNumber() + " is now in overdraft by " +
                    account.getCurrency() + " " + ValidationUtil.formatAmount(overdraftUsed) +
                    ". Interest will be charged.");
        }
    }
    /**
     * Gets the transaction history with pagination.
     * 
     * @param accountNumber The account number
     * @param page Page number (1-based)
     * @param pageSize Number of transactions per page
     * @return List of transactions for the requested page
     */
    public List<Transaction> getTransactionHistory(String accountNumber, int page, int pageSize) 
            throws InvalidAccountException {
        Account account = accountService.getAccount(accountNumber);
        List<Transaction> allTxns = account.getTransactionHistory();

        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allTxns.size());

        if (startIndex >= allTxns.size()) {
            return new ArrayList<>();
        }

        return allTxns.subList(startIndex, endIndex);
    }

    /**
     * Gets the total number of pages for transaction history.
     */
    public int getTransactionPageCount(String accountNumber, int pageSize) throws InvalidAccountException {
        Account account = accountService.getAccount(accountNumber);
        int totalTxns = account.getTransactionHistory().size();
        return (int) Math.ceil((double) totalTxns / pageSize);
    }

    /**
     * Searches transactions by various criteria.
     * 
     * @param accountNumber Account to search in
     * @param startDate Start date filter (nullable)
     * @param endDate End date filter (nullable)
     * @param type Transaction type filter (nullable)
     * @param minAmount Minimum amount filter (0 for no filter)
     * @param maxAmount Maximum amount filter (0 for no filter)
     * @return Filtered list of transactions
     */
    public List<Transaction> searchTransactions(String accountNumber, String startDate,
                                                 String endDate, TransactionType type,
                                                 double minAmount, double maxAmount) 
            throws InvalidAccountException {
        Account account = accountService.getAccount(accountNumber);
        List<Transaction> results = new ArrayList<>(account.getTransactionHistory());

        // Filter by date range
        if (startDate != null && endDate != null) {
            results = results.stream().filter(t -> {
                String txnDate = t.getTimestamp().substring(0, 10);
                return DateUtil.isWithinRange(txnDate, startDate, endDate);
            }).collect(Collectors.toList());
        }

        // Filter by type
        if (type != null) {
            results = results.stream()
                    .filter(t -> t.getType() == type)
                    .collect(Collectors.toList());
        }

        // Filter by amount range
        if (minAmount > 0) {
            results = results.stream()
                    .filter(t -> t.getAmount() >= minAmount)
                    .collect(Collectors.toList());
        }
        if (maxAmount > 0) {
            results = results.stream()
                    .filter(t -> t.getAmount() <= maxAmount)
                    .collect(Collectors.toList());
        }

        return results;
    }

    /**
     * Exports transaction history to CSV file.
     * 
     * @param accountNumber The account number
     * @param outputPath Output file path
     */
    public void exportTransactionHistory(String accountNumber, String outputPath) 
            throws InvalidAccountException {
        Account account = accountService.getAccount(accountNumber);
        List<Transaction> txns = account.getTransactionHistory();

        String headers = "Transaction ID,Date,Type,Amount,Currency,Status,Description,Balance After";
        List<String> rows = new ArrayList<>();
        for (Transaction txn : txns) {
            rows.add(txn.toFileString());
        }

        FileHandler.exportToCsv(outputPath, headers, rows);
        FileHandler.logAudit("EXPORT", "Transaction history exported for " + accountNumber);
    }

    // ==================== ALERT HELPERS ====================

    /**
     * Checks if account balance is below the low-balance threshold.
     */
    private void checkLowBalance(Account account) {
        double minBalance = account.getMinimumBalance();
        if (minBalance > 0) {
            double threshold = minBalance * 0.1; // 10% of minimum balance
            if (account.getBalance() <= threshold) {
                sendNotification(account.getCustomerId(),
                        AlertType.LOW_BALANCE,
                        "Low Balance Warning",
                        "Account " + account.getAccountNumber() + " balance is " +
                        account.getCurrency() + " " + ValidationUtil.formatAmount(account.getBalance()) +
                        ". Minimum balance: " + account.getCurrency() + " " + ValidationUtil.formatAmount(minBalance));
            }
        }
    }

    /**
     * Checks if a transaction amount triggers a large transaction alert.
     */
    private void checkLargeTransaction(Account account, double amount) {
        if (amount > ValidationUtil.LARGE_TRANSACTION_THRESHOLD) {
            sendNotification(account.getCustomerId(),
                    AlertType.LARGE_TRANSACTION,
                    "Large Transaction Alert",
                    "A transaction of " + account.getCurrency() + " " + ValidationUtil.formatAmount(amount) +
                    " was processed on account " + account.getAccountNumber());
        }
    }

    /**
     * Sends a notification to a customer.
     */
    private void sendNotification(String customerId, AlertType type, String title, String message) {
        if (notificationService != null) {
            notificationService.sendNotification(customerId, type, title, message);
        }
    }
}

