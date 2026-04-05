package com.rajarata.bank.services;

import com.rajarata.bank.models.account.Account;
import com.rajarata.bank.models.account.CheckingAccount;
import com.rajarata.bank.models.transaction.*;
import com.rajarata.bank.models.notification.AlertType;
import com.rajarata.bank.models.notification.Notification;
import com.rajarata.bank.models.user.Customer;
import com.rajarata.bank.exceptions.*;
import com.rajarata.bank.utils.*;
import com.rajarata.bank.services.CurrencyService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for processing financial transactions.
 * Handles deposits, withdrawals, transfers, and transaction history.
 * 
 * OOP Concept: Polymorphism - Processes transactions uniformly through the
 * Account and Transactable interfaces, regardless of concrete account type.
 * The same processTransaction method works for all account subtypes.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class TransactionService {

    /** Reference to account service */
    private final AccountService accountService;
    /** Reference to authentication service */
    private final AuthenticationService authService;
    /** Reference to notification service */
    private NotificationService notificationService;
    /** Reference to currency service for cross-currency transfers */
    private CurrencyService currencyService;
    /** Daily transaction limit per account */
    private static final double DAILY_TRANSACTION_LIMIT = 50000.0;
    /** Conversion fee rate for cross-currency transfers (0.5%) */
    private static final double CURRENCY_CONVERSION_FEE_RATE = 0.005;

    /**
     * Constructor.
     * @param accountService Reference to account service
     * @param authService Reference to authentication service
     */
    public TransactionService(AccountService accountService, AuthenticationService authService) {
        this.accountService = accountService;
        this.authService = authService;
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
     * Processes a deposit transaction.
     * 
     * OOP Concept: Polymorphism - Calls account.deposit() which behaves
     * differently based on the concrete account type.
     * 
     * @param accountNumber The account to deposit into
     * @param amount The deposit amount
     * @param description Transaction description
     * @return The completed Transaction object
     * @throws InvalidAccountException if account validation fails
     * @throws InvalidInputException if amount validation fails
     */
    public Transaction deposit(String accountNumber, double amount, String description) 
            throws InvalidAccountException, InvalidInputException {

        // Validate amount
        if (!ValidationUtil.isValidDeposit(amount)) {
            throw new InvalidInputException(
                "Invalid deposit amount. Minimum is $" + ValidationUtil.formatAmount(Account.MIN_DEPOSIT),
                "amount");
        }

        Account account = accountService.getAccount(accountNumber);
        Transaction txn = new Transaction(TransactionType.DEPOSIT, accountNumber, amount,
                description != null ? description : "Cash deposit");

        try {
            // OOP Concept: Polymorphism - deposit() may behave differently per account type
            account.deposit(amount);
            txn.complete(account.getBalance());
            account.addTransaction(txn);

            // Save data
            accountService.saveAllAccounts();
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, txn.toFileString());
            FileHandler.logAudit("DEPOSIT",
                    "Deposit $" + ValidationUtil.formatAmount(amount) + " to " + accountNumber);

            // Notifications — success notification and large-transaction check
            sendNotification(account.getCustomerId(),
                    AlertType.TRANSACTION_SUCCESS,
                    "Deposit Successful",
                    "$" + ValidationUtil.formatAmount(amount) + " deposited to " +
                    accountNumber + ". New balance: $" + ValidationUtil.formatAmount(account.getBalance()));
            checkLargeTransaction(account, amount);

        } catch (InvalidAccountException e) {
            txn.fail(e.getMessage());
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, txn.toFileString());
            FileHandler.logAudit("DEPOSIT_FAILED",
                    "Failed deposit to " + accountNumber + ": " + e.getMessage());

            // Notification — failed transaction alert
            sendNotification(account.getCustomerId(),
                    AlertType.TRANSACTION_FAILED,
                    "Deposit Failed",
                    "Failed to deposit $" + ValidationUtil.formatAmount(amount) +
                    " to " + accountNumber + ". Reason: " + e.getMessage());
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

        if (amount <= 0) {
            throw new InvalidAccountException("Withdrawal amount must be positive", accountNumber);
        }

        Account account = accountService.getAccount(accountNumber);
        Transaction txn = new Transaction(TransactionType.WITHDRAWAL, accountNumber, amount,
                description != null ? description : "Cash withdrawal");

        try {
            // OOP Concept: Polymorphism - withdraw() has different behavior per account type
            // SavingsAccount: checks withdrawal limit, no overdraft
            // CheckingAccount: allows overdraft up to $1000
            // StudentAccount: checks withdrawal limit, no overdraft
            // FixedDepositAccount: applies early withdrawal penalty
            account.withdraw(amount);
            txn.complete(account.getBalance());
            account.addTransaction(txn);

            accountService.saveAllAccounts();
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, txn.toFileString());
            FileHandler.logAudit("WITHDRAWAL",
                    "Withdrawal $" + ValidationUtil.formatAmount(amount) + " from " + accountNumber);

            // Success notification
            sendNotification(account.getCustomerId(),
                    AlertType.TRANSACTION_SUCCESS,
                    "Withdrawal Successful",
                    "$" + ValidationUtil.formatAmount(amount) + " withdrawn from " +
                    accountNumber + ". New balance: $" + ValidationUtil.formatAmount(account.getBalance()));

            // Check for overdraft usage on CheckingAccount — audit log + alert
            if (account instanceof CheckingAccount && account.getBalance() < 0) {
                double overdraftUsed = Math.abs(account.getBalance());
                FileHandler.logAudit("OVERDRAFT_USED",
                        "Account " + accountNumber + " is now in overdraft. Amount: $" +
                        ValidationUtil.formatAmount(overdraftUsed));
                sendNotification(account.getCustomerId(),
                        AlertType.OVERDRAFT_ALERT,
                        "Overdraft Facility Used",
                        "Your checking account " + accountNumber + " is now in overdraft by $" +
                        ValidationUtil.formatAmount(overdraftUsed) +
                        ". Overdraft interest (15% p.a.) will be charged.");
            }

            // Check for low balance and large transaction alerts
            checkLowBalance(account);
            checkLargeTransaction(account, amount);

        } catch (InsufficientFundsException | InvalidAccountException e) {
            txn.fail(e.getMessage());
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, txn.toFileString());
            FileHandler.logAudit("WITHDRAWAL_FAILED",
                    "Failed withdrawal from " + accountNumber + ": " + e.getMessage());

            // Failure notification
            sendNotification(account.getCustomerId(),
                    AlertType.TRANSACTION_FAILED,
                    "Withdrawal Failed",
                    "Failed to withdraw $" + ValidationUtil.formatAmount(amount) +
                    " from " + accountNumber + ". Reason: " + e.getMessage());
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
            // Withdraw from source (in source currency)
            sourceAccount.withdraw(amount);
            sourceTxn.complete(sourceAccount.getBalance());
            sourceAccount.addTransaction(sourceTxn);

            // Deposit to destination (converted amount in dest currency)
            destAccount.deposit(depositAmount);
            destTxn.complete(destAccount.getBalance());
            destAccount.addTransaction(destTxn);

            // Save
            accountService.saveAllAccounts();
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, sourceTxn.toFileString());
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, destTxn.toFileString());

            // Build audit message
            String auditMsg = "Transfer $" + ValidationUtil.formatAmount(amount) + " " +
                    sourceCurrency + " from " + sourceAccountNumber + " to " + destAccountNumber;
            if (isCrossCurrency) {
                auditMsg += " (converted to " + ValidationUtil.formatAmount(depositAmount) +
                        " " + destCurrency + ", fee: " + ValidationUtil.formatAmount(conversionFee) +
                        " " + destCurrency + ")";
            }
            FileHandler.logAudit("TRANSFER", auditMsg);

            // Success notifications — notify both sender and receiver
            String senderMsg = "$" + ValidationUtil.formatAmount(amount) + " " + sourceCurrency +
                    " transferred from " + sourceAccountNumber + " to " + destAccountNumber;
            if (isCrossCurrency) {
                senderMsg += " (" + ValidationUtil.formatAmount(depositAmount) + " " + destCurrency +
                        " received, fee: " + ValidationUtil.formatAmount(conversionFee) + " " + destCurrency + ")";
            }
            senderMsg += ". New balance: $" + ValidationUtil.formatAmount(sourceAccount.getBalance());
            sendNotification(sourceAccount.getCustomerId(),
                    AlertType.TRANSFER_SUCCESS, "Transfer Sent", senderMsg);

            String receiverMsg = (isCrossCurrency
                    ? ValidationUtil.formatAmount(depositAmount) + " " + destCurrency
                    : "$" + ValidationUtil.formatAmount(amount))
                    + " received from " + sourceAccountNumber +
                    ". New balance: $" + ValidationUtil.formatAmount(destAccount.getBalance());
            sendNotification(destAccount.getCustomerId(),
                    AlertType.TRANSFER_SUCCESS, "Transfer Received", receiverMsg);

            // Check overdraft on source if CheckingAccount
            if (sourceAccount instanceof CheckingAccount && sourceAccount.getBalance() < 0) {
                double overdraftUsed = Math.abs(sourceAccount.getBalance());
                FileHandler.logAudit("OVERDRAFT_USED",
                        "Account " + sourceAccountNumber + " is now in overdraft. Amount: $" +
                        ValidationUtil.formatAmount(overdraftUsed));
                sendNotification(sourceAccount.getCustomerId(),
                        AlertType.OVERDRAFT_ALERT,
                        "Overdraft Facility Used",
                        "Your checking account " + sourceAccountNumber + " is now in overdraft by $" +
                        ValidationUtil.formatAmount(overdraftUsed) +
                        ". Overdraft interest (15% p.a.) will be charged.");
            }

            // Check alerts
            checkLowBalance(sourceAccount);
            checkLargeTransaction(sourceAccount, amount);

        } catch (InsufficientFundsException | InvalidAccountException e) {
            sourceTxn.fail(e.getMessage());
            destTxn.fail("Source transaction failed");
            FileHandler.appendLine(FileHandler.TRANSACTIONS_FILE, sourceTxn.toFileString());
            FileHandler.logAudit("TRANSFER_FAILED",
                    "Failed transfer from " + sourceAccountNumber + ": " + e.getMessage());

            // Failure notification
            sendNotification(sourceAccount.getCustomerId(),
                    AlertType.TRANSACTION_FAILED,
                    "Transfer Failed",
                    "Failed to transfer $" + ValidationUtil.formatAmount(amount) +
                    " from " + sourceAccountNumber + ". Reason: " + e.getMessage());
            throw e;
        }

        return sourceTxn;
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
                        "Account " + account.getAccountNumber() + " balance is $" +
                        ValidationUtil.formatAmount(account.getBalance()) +
                        ". Minimum balance: $" + ValidationUtil.formatAmount(minBalance));
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
                    "A transaction of $" + ValidationUtil.formatAmount(amount) +
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
