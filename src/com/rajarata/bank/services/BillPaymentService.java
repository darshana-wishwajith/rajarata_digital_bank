package com.rajarata.bank.services;

import com.rajarata.bank.models.account.Account;
import com.rajarata.bank.models.transaction.*;
import com.rajarata.bank.exceptions.*;
import com.rajarata.bank.utils.*;

import java.util.*;

/**
 * Service class for bill payment operations.
 * Supports electricity, water, internet, and phone bill payments.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class BillPaymentService {

    /** Storage for saved payees: customerId -> list of payee records */
    private final Map<String, List<String[]>> savedPayees;
    /** Reference to transaction service */
    private final TransactionService transactionService;
    /** Reference to account service */
    private final AccountService accountService;

    /** Supported bill providers */
    public static final String[][] PROVIDERS = {
        {"1", "Electricity", "National Power Company", "ELC-XXXXXXXX"},
        {"2", "Water", "Municipal Water Board", "WTR-XXXXXXXX"},
        {"3", "Internet", "FastNet Communications", "INT-XXXXXXXX"},
        {"4", "Phone", "TelecomPlus", "TEL-XXXXXXXX"}
    };

    public BillPaymentService(TransactionService transactionService, AccountService accountService) {
        this.savedPayees = new HashMap<>();
        this.transactionService = transactionService;
        this.accountService = accountService;
    }


    /**
     * Adds a payee for a customer.
     * 
     * @param customerId Customer ID
     * @param providerType Provider category (Electricity, Water, etc.)
     * @param accountNumber Bill account number
     * @param nickname Friendly name for the payee
     * @throws InvalidInputException if validation fails
     */
    public void addPayee(String customerId, String providerType, String accountNumber, String nickname) 
            throws InvalidInputException {

        if (!ValidationUtil.isValidBillAccountNumber(providerType, accountNumber)) {
            // Get expected format
            String format = getExpectedFormat(providerType);
            throw new InvalidInputException(
                "Invalid account number format for " + providerType + ". Expected: " + format,
                "accountNumber");
        }

        savedPayees.computeIfAbsent(customerId, k -> new ArrayList<>());
        savedPayees.get(customerId).add(new String[]{providerType, accountNumber, nickname});

        // Save to file
        FileHandler.appendLine(FileHandler.PAYEES_FILE,
                String.join("|", customerId, providerType, accountNumber, nickname));

        FileHandler.logAudit("PAYEE_ADDED",
                "Payee added for " + customerId + ": " + nickname + " (" + providerType + ")");
    }

    /**
     * Gets the expected account number format for a provider.
     */
    private String getExpectedFormat(String providerType) {
        for (String[] provider : PROVIDERS) {
            if (provider[1].equalsIgnoreCase(providerType)) {
                return provider[3];
            }
        }
        return "Unknown format";
    }

    /**
     * Processes a bill payment.
     * 
     * @param sourceAccount Account to pay from
     * @param providerType Bill provider type
     * @param billAccountNumber Bill account number
     * @param amount Payment amount
     * @param nickname Payee nickname
     * @return The payment Transaction
     */
    public Transaction payBill(String sourceAccount, String providerType,
                                String billAccountNumber, double amount, String nickname) 
            throws InsufficientFundsException, InvalidAccountException, InvalidInputException {

        if (amount <= 0) {
            throw new InvalidInputException("Payment amount must be positive", "amount");
        }

        // Find provider name
        String providerName = "";
        for (String[] provider : PROVIDERS) {
            if (provider[1].equalsIgnoreCase(providerType)) {
                providerName = provider[2];
                break;
            }
        }

        String description = "Bill Payment - " + providerName + " (" + nickname + ") Ref: " + billAccountNumber;

        // Process as withdrawal using transactionService to ensure consistent fraud checks and logging
        Account account = accountService.getAccount(sourceAccount);
        Transaction txn = new Transaction(TransactionType.BILL_PAYMENT, sourceAccount,
                amount, description);

        try {
            transactionService.processWithdrawal(account, txn, amount, true);
        } catch (InsufficientFundsException | InvalidAccountException e) {
            // Already handled by processWithdrawal but we rethrow for the UI
            throw e;
        }

        return txn;
    }

    /**
     * Generates a payment receipt.
     * @param txn The payment transaction
     * @return Formatted receipt string
     */
    public String generateReceipt(Transaction txn) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append("║         PAYMENT RECEIPT                  ║\n");
        sb.append("║      Rajarata Digital Bank               ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append(String.format("║ Receipt #   : %-26s ║\n", txn.getTransactionId()));
        sb.append(String.format("║ Date        : %-26s ║\n", txn.getTimestamp()));
        sb.append(String.format("║ Amount      : $%-25s ║\n", ValidationUtil.formatAmount(txn.getAmount())));
        sb.append(String.format("║ Status      : %-26s ║\n", txn.getStatus().getDisplayName()));
        sb.append(String.format("║ Account     : %-26s ║\n", txn.getSourceAccount()));
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append(String.format("║ %-40s ║\n", txn.getDescription()));
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append("║    Thank you for using Rajarata Bank!    ║\n");
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Gets saved payees for a customer.
     * @param customerId The customer ID
     * @return List of payee arrays [providerType, accountNumber, nickname]
     */
    public List<String[]> getSavedPayees(String customerId) {
        return savedPayees.getOrDefault(customerId, new ArrayList<>());
    }

    /**
     * Gets provider display list.
     * @return Formatted string of available providers
     */
    public String getProviderList() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  Available Bill Providers:\n");
        sb.append("  ─────────────────────────────────────────\n");
        for (String[] provider : PROVIDERS) {
            sb.append(String.format("  %s. %-12s - %-25s\n", provider[0], provider[1], provider[2]));
            sb.append(String.format("     Account format: %s\n", provider[3]));
        }
        return sb.toString();
    }

    /**
     * Loads saved payees from file.
     */
    public void loadPayees() {
        savedPayees.clear();
        List<String> lines = FileHandler.readAllLines(FileHandler.PAYEES_FILE);
        for (String line : lines) {
            try {
                String[] parts = line.split(FileHandler.DELIMITER_REGEX, -1);
                if (parts.length >= 4) {
                    savedPayees.computeIfAbsent(parts[0], k -> new ArrayList<>());
                    savedPayees.get(parts[0]).add(new String[]{parts[1], parts[2], parts[3]});
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to load payee: " + e.getMessage());
            }
        }
    }
}

