package com.rajarata.bank.services;

import com.rajarata.bank.models.notification.AlertType;
import com.rajarata.bank.models.transaction.Transaction;
import com.rajarata.bank.models.transaction.TransactionType;
import com.rajarata.bank.models.account.Account;
import com.rajarata.bank.utils.*;

import java.util.*;


/**
 * Service class for detecting and managing potential fraud cases.
 * Monitors transactions for suspicious patterns including:
 * - Large transaction amounts (>$10,000)
 * - Rapid successive withdrawals (velocity check)
 * - Unusual transaction patterns (multiple withdrawals in short timeframe)
 * - Repeated failed login attempts (triggered by AuthenticationService)
 * 
 * OOP Concept: Encapsulation - All fraud detection logic is encapsulated here.
 * Other services interact through the public API for flagging and monitoring.
 *
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class FraudDetectionService {

    private final Map<String, String[]> fraudCases;
    private NotificationService notificationService;
    private final AccountService accountService;
    private final CurrencyService currencyService;

    /** Threshold for single-transaction fraud alert (LKR 1M ≈ $3000) */
    private static final double LARGE_TRANSACTION_THRESHOLD = 1000000.0;
    /** Maximum number of withdrawals allowed in a short timeframe before flagging */
    private static final int RAPID_WITHDRAWAL_LIMIT = 3;
    /** Timeframe (in minutes) for velocity checking */
    private static final int VELOCITY_WINDOW_MINUTES = 10;
    /** Cumulative withdrawal threshold in velocity window (LKR 3M ≈ $10k) */
    private static final double VELOCITY_AMOUNT_THRESHOLD = 3000000.0;

    private static int caseCounter = 0;

    /**
     * Tracks recent withdrawal timestamps per account for velocity detection.
     * Key = account number, Value = list of [timestamp, amount] pairs.
     */
    private final Map<String, List<double[]>> recentWithdrawals;

    public FraudDetectionService(AccountService accountService, CurrencyService currencyService) {
        this.fraudCases = new HashMap<>();
        this.accountService = accountService;
        this.currencyService = currencyService;
        this.recentWithdrawals = new HashMap<>();
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Checks a transaction for suspicious patterns.
     * Called after every transaction is processed.
     * 
     * Checks performed:
     * 1. Large single-transaction amount (>$10,000)
     * 2. Rapid successive withdrawals (velocity check: 3+ withdrawals in 10 minutes)
     * 3. Cumulative withdrawal amount exceeding threshold within velocity window
     * 
     * @param transaction The transaction to check
     * @param account The account involved
     * @return true if suspicious activity was detected
     */
    public boolean checkTransaction(Transaction transaction, Account account) {
        boolean suspicious = false;

        // Normalize amount to LKR for threshold checks
        double amountInLKR = currencyService.convert(transaction.getAmount(), transaction.getCurrency(), "LKR");

        // 1. Large single-transaction check
        if (amountInLKR >= LARGE_TRANSACTION_THRESHOLD) {
            flagSuspiciousActivity(account.getCustomerId(), account.getAccountNumber(),
                    "Large transaction: " + transaction.getCurrency() + " " + ValidationUtil.formatAmount(transaction.getAmount()),
                    "LARGE_TRANSACTION");
            suspicious = true;
        }

        // 2. Velocity check — rapid successive withdrawals
        if (transaction.getType() == TransactionType.WITHDRAWAL ||
            transaction.getType() == TransactionType.TRANSFER) {
            suspicious = checkWithdrawalVelocity(account, transaction.getAmount(), transaction.getCurrency()) || suspicious;
        }

        return suspicious;
    }

    /**
     * Checks for rapid successive withdrawals on an account (velocity detection).
     * Flags if 3+ withdrawals occur within a 10-minute window, or if the
     * cumulative amount within that window exceeds $15,000.
     * 
     * OOP Concept: Encapsulation - Internal fraud-detection heuristic hidden
     * behind a simple boolean interface.
     * 
     * @param account The account being monitored
     * @param amount The current withdrawal amount
     * @return true if velocity limit was triggered
     */
    private boolean checkWithdrawalVelocity(Account account, double amount, String currency) {
        String accNum = account.getAccountNumber();
        long currentTimeMillis = System.currentTimeMillis();

        // Normalize amount to LKR for threshold checks
        double amountInLKR = currencyService.convert(amount, currency, "LKR");

        // Initialize tracking list if needed
        recentWithdrawals.computeIfAbsent(accNum, k -> new ArrayList<>());
        List<double[]> history = recentWithdrawals.get(accNum);

        // Add current withdrawal
        history.add(new double[]{ currentTimeMillis, amountInLKR });

        // Remove entries outside the velocity window
        long windowStart = currentTimeMillis - (VELOCITY_WINDOW_MINUTES * 60 * 1000L);
        history.removeIf(entry -> entry[0] < windowStart);

        boolean triggered = false;

        // Check count-based velocity (3+ withdrawals in window)
        if (history.size() >= RAPID_WITHDRAWAL_LIMIT) {
            flagSuspiciousActivity(account.getCustomerId(), accNum,
                    "Rapid successive withdrawals: " + history.size() +
                    " withdrawals within " + VELOCITY_WINDOW_MINUTES + " minutes",
                    "RAPID_WITHDRAWALS");
            triggered = true;
        }

        // Check amount-based velocity (cumulative > threshold in window)
        double cumulativeAmountLKR = 0;
        for (double[] entry : history) {
            cumulativeAmountLKR += entry[1];
        }
        if (cumulativeAmountLKR > VELOCITY_AMOUNT_THRESHOLD) {
            flagSuspiciousActivity(account.getCustomerId(), accNum,
                    "High cumulative withdrawals: LKR " + ValidationUtil.formatAmount(cumulativeAmountLKR) +
                    " within " + VELOCITY_WINDOW_MINUTES + " minutes",
                    "HIGH_CUMULATIVE_WITHDRAWAL");
            triggered = true;
        }

        return triggered;
    }

    /**
     * Flags a suspicious activity and creates a fraud case.
     * Notifies all staff/admin users and logs to the audit trail.
     * 
     * @param customerId The customer involved
     * @param accountNumber The account involved
     * @param description Description of the suspicious activity
     * @param pattern The detected pattern type (e.g., LARGE_TRANSACTION, RAPID_WITHDRAWALS, REPEATED_LOGIN_FAILURE)
     */
    public void flagSuspiciousActivity(String customerId, String accountNumber,
                                       String description, String pattern) {
        caseCounter++;
        String caseId = String.format("FRAUD-%06d", caseCounter);
        String[] caseData = {
            caseId, customerId, accountNumber, description, pattern,
            "Under Investigation", DateUtil.getCurrentDateTime(), ""
        };
        fraudCases.put(caseId, caseData);
        FileHandler.appendLine(FileHandler.FRAUD_CASES_FILE, String.join("|", caseData));

        if (notificationService != null) {
            notificationService.broadcastToStaff(AlertType.FRAUD_ALERT,
                    "Suspicious Activity Detected",
                    "Case " + caseId + ": " + description + " on account " + accountNumber);
        }
        FileHandler.logAudit("FRAUD_DETECTED",
                "Case " + caseId + ": " + description + " | Customer: " + customerId +
                " | Pattern: " + pattern);
    }

    /**
     * Updates the status and notes of an existing fraud case.
     * 
     * @param caseId The fraud case ID
     * @param status New status (Under Investigation, False Positive, Confirmed Fraud)
     * @param notes Investigation notes
     * @return true if case was found and updated
     */
    public boolean updateCase(String caseId, String status, String notes) {
        String[] caseData = fraudCases.get(caseId);
        if (caseData != null) {
            caseData[5] = status;
            caseData[7] = notes;
            saveFraudCases();
            FileHandler.logAudit("FRAUD_CASE_UPDATE", "Case " + caseId + " updated to: " + status);
            return true;
        }
        return false;
    }

    /**
     * Gets a formatted display of all fraud cases for staff/admin viewing.
     * @return Formatted string listing all fraud cases
     */
    public String getFraudCaseDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔════════════════════════════════════════════════════════════╗\n");
        sb.append("║                    FRAUD CASES                            ║\n");
        sb.append("╠════════════════════════════════════════════════════════════╣\n");
        if (fraudCases.isEmpty()) {
            sb.append("║   No fraud cases recorded.                                ║\n");
        } else {
            sb.append(String.format("║ %-12s %-10s %-14s %-20s ║\n",
                    "Case ID", "Customer", "Status", "Pattern"));
            sb.append("║────────────────────────────────────────────────────────────║\n");
            for (String[] c : fraudCases.values()) {
                sb.append(String.format("║ %-12s %-10s %-14s %-20s ║\n",
                        c[0], c[1], c[5], c[4]));
            }
        }
        sb.append("╚════════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /** @return Copy of all fraud cases */
    public Map<String, String[]> getAllCases() { return new HashMap<>(fraudCases); }

    private void saveFraudCases() {
        List<String> lines = new ArrayList<>();
        for (String[] c : fraudCases.values()) {
            lines.add(String.join("|", c));
        }
        FileHandler.writeAllLines(FileHandler.FRAUD_CASES_FILE, lines);
    }

    /**
     * Loads fraud cases from file at application startup.
     */
    public void loadFraudCases() {
        List<String> lines = FileHandler.readAllLines(FileHandler.FRAUD_CASES_FILE);
        int maxCounter = 0;
        for (String line : lines) {
            String[] parts = line.split(FileHandler.DELIMITER_REGEX, -1);
            if (parts.length >= 8) {
                fraudCases.put(parts[0], parts);
                try {
                    int num = Integer.parseInt(parts[0].replace("FRAUD-", ""));
                    maxCounter = Math.max(maxCounter, num);
                } catch (NumberFormatException e) { /* ignore */ }
            }
        }
        caseCounter = maxCounter;
    }
}
