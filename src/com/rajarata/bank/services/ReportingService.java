package com.rajarata.bank.services;

import com.rajarata.bank.models.user.*;
import com.rajarata.bank.models.account.Account;
import com.rajarata.bank.models.transaction.*;
import com.rajarata.bank.models.loan.*;
import com.rajarata.bank.utils.*;

import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for generating various financial and system reports.
 * 
 * OOP Concept: Polymorphism - Report generation methods accept the base Account class,
 * allowing them to process any account subtype (Savings, Checking, etc.) uniformly.
 * 
 * OOP Concept: Dependency Injection - Required services are provided via the constructor,
 * promoting loose coupling and better testability.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class ReportingService {

    private final AccountService accountService;
    private final AuthenticationService authService;
    private final LoanService loanService;

    /**
     * Constructs a new ReportingService with required dependencies.
     * @param accountService Service for account operations
     * @param authService Service for authentication and user data
     * @param loanService Service for loan operations
     */
    public ReportingService(AccountService accountService,
                            AuthenticationService authService,
                            LoanService loanService) {
        this.accountService = accountService;
        this.authService = authService;
        this.loanService = loanService;
    }

    /**
     * Generates a monthly account statement for a customer.
     * 
     * @param accountNumber The account to generate the statement for
     * @param month The month (1-12)
     * @param year The year
     * @return Formatted string representation of the monthly statement
     */
    public String generateMonthlyStatement(String accountNumber, int month, int year) {
        try {
            Account account = accountService.getAccount(accountNumber);
            if (account == null) return "Error: Account not found (" + accountNumber + ")";

            // Define date range for the month
            LocalDate firstDay = LocalDate.of(year, month, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            String startDateStr = firstDay.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String endDateStr = lastDay.format(DateTimeFormatter.ISO_LOCAL_DATE);

            List<Transaction> txns = account.getTransactionsByDateRange(startDateStr, endDateStr);

            double totalDeposits = 0;
            double totalWithdrawals = 0;
            double periodInterestEarned = 0;
            int depositCount = 0;
            int withdrawalCount = 0;
            int interestCount = 0;

            for (Transaction txn : txns) {
                if (txn.getStatus() != TransactionStatus.COMPLETED) continue;

                if (txn.getType() == TransactionType.INTEREST_CREDIT) {
                    periodInterestEarned += txn.getAmount();
                    interestCount++;
                } else if (txn.getType() == TransactionType.DEPOSIT || 
                          txn.getType() == TransactionType.LOAN_DISBURSEMENT) {
                    totalDeposits += txn.getAmount();
                    depositCount++;
                } else {
                    totalWithdrawals += txn.getAmount();
                    withdrawalCount++;
                }
            }

            // Estimate opening balance
            double openingBalance = account.getBalance() - totalDeposits - periodInterestEarned + totalWithdrawals;
            String currCode = account.getCurrency();
            String currSym = com.rajarata.bank.utils.CurrencyUtil.getCurrencySymbol(currCode);

            StringBuilder sb = new StringBuilder();
            sb.append("\n╔══════════════════════════════════════════════════════════════════════════╗\n");
            sb.append("║                        RAJARATA DIGITAL BANK                             ║\n");
            sb.append("║                     Monthly Account Statement                            ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════════════════╣\n");
            
            User user = authService.getUserById(account.getCustomerId());
            String customerName = (user != null) ? user.getFullName() : "Unknown";

            sb.append(drawBoxRow("Customer Name", customerName, 74));
            sb.append(drawBoxRow("Account Number", accountNumber, 74));
            sb.append(drawBoxRow("Account Type", account.getAccountType(), 74));
            sb.append(drawBoxRow("Currency", currCode, 74));
            sb.append(drawBoxRow("Period", String.format("%02d/%04d", month, year), 74));
            
            sb.append("╠══════════════════════════════════════════════════════════════════════════╣\n");
            sb.append(drawBoxRow("Opening Balance", currSym + " " + ValidationUtil.formatAmount(openingBalance), 74));
            sb.append("╠══════════════════════════════════════════════════════════════════════════╣\n");
            // Standardizing table internal width to 74
            // Date(10) | Type(18) | Amount(15) | Balance(15) | Desc(12) = 10+1+18+1+15+1+15+1+12 = 74!
            sb.append("║ Date       │ Type            │ Amount       │ Balance      │ Desc        ║\n");
            sb.append("║────────────┼─────────────────┼──────────────┼──────────────┼─────────────║\n");

            double runningBalance = openingBalance;
            for (Transaction txn : txns) {
                if (txn.getStatus() != TransactionStatus.COMPLETED) continue;
                
                boolean isCredit = (txn.getType() == TransactionType.DEPOSIT || 
                                   txn.getType() == TransactionType.LOAN_DISBURSEMENT || 
                                   txn.getType() == TransactionType.INTEREST_CREDIT);
                
                if (isCredit) runningBalance += txn.getAmount();
                else runningBalance -= txn.getAmount();

                String desc = txn.getDescription() != null ? txn.getDescription() : "";

                sb.append(drawBoxTableLine(
                        txn.getTimestamp().substring(0, 10),
                        txn.getType().getDisplayName(),
                        ValidationUtil.formatAmount(txn.getAmount()),
                        ValidationUtil.formatAmount(runningBalance),
                        desc,
                        74));
            }

            sb.append("╠══════════════════════════════════════════════════════════════════════════╣\n");
            sb.append(drawBoxRow("Closing Balance", currSym + " " + ValidationUtil.formatAmount(account.getBalance()), 74));
            sb.append("╠══════════════════════════════════════════════════════════════════════════╣\n");
            sb.append(drawBoxLine("TRANSACTION SUMMARY", 74));
            sb.append("╠══════════════════════════════════════════════════════════════════════════╣\n");
            sb.append(drawBoxRow("Total Deposits", currSym + " " + ValidationUtil.formatAmount(totalDeposits), 74));
            sb.append(drawBoxRow("  # of Deposits", String.valueOf(depositCount), 74));
            sb.append(drawBoxRow("Total Withdrawals", currSym + " " + ValidationUtil.formatAmount(totalWithdrawals), 74));
            sb.append(drawBoxRow("  # of Withdrawals", String.valueOf(withdrawalCount), 74));
            sb.append(drawBoxRow("Total Interest", currSym + " " + ValidationUtil.formatAmount(periodInterestEarned), 74));
            sb.append(drawBoxRow("  # of Credits", String.valueOf(interestCount), 74));
            sb.append("╠══════════════════════════════════════════════════════════════════════════╣\n");
            sb.append(drawBoxLine("For queries, contact: support@rajarata.com", 74));
            sb.append("╚══════════════════════════════════════════════════════════════════════════╝\n");

            return sb.toString();

        } catch (Exception e) {
            return "Error generating statement: " + e.getMessage();
        }
    }

    /**
     * Generates a system statistics summary report.
     * @return Formatted system statistics report
     */
    public String generateSystemStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append(drawBoxLine("SYSTEM STATISTICS", 42));
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append(drawBoxRow("Total Users", String.valueOf(authService.getUserCount()), 42));
        sb.append(drawBoxRow("Total Accounts", String.valueOf(accountService.getAccountCount()), 42));
        sb.append(drawBoxRow("Total Loans", String.valueOf(loanService.getAllLoans().size()), 42));

        // Calculate total balances per currency
        java.util.Map<String, Double> currencyTotals = new java.util.TreeMap<>();
        for (Account acc : accountService.getAllAccounts().values()) {
            currencyTotals.merge(acc.getCurrency(), acc.getBalance(), Double::sum);
        }
        for (java.util.Map.Entry<String, Double> entry : currencyTotals.entrySet()) {
            sb.append(drawBoxRow("Total " + entry.getKey(), ValidationUtil.formatAmount(entry.getValue()), 42));
        }

        // Active loans
        long activeLoans = loanService.getAllLoans().values().stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE).count();
        sb.append(drawBoxRow("Active Loans", String.valueOf(activeLoans), 42));
        sb.append(drawBoxRow("Report Date", DateUtil.getCurrentDate(), 42));
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Generates a loan performance report.
     * @return Formatted loan performance report
     */
    public String generateLoanPerformanceReport() {
        Map<String, Loan> allLoans = loanService.getAllLoans();

        int total = allLoans.size(), active = 0, paid = 0, defaulted = 0, rejected = 0, pending = 0;
        double totalDisbursed = 0, totalOutstanding = 0;

        for (Loan loan : allLoans.values()) {
            switch (loan.getStatus()) {
                case ACTIVE: active++; totalOutstanding += loan.getRemainingBalance(); totalDisbursed += loan.getLoanAmount(); break;
                case PAID: paid++; totalDisbursed += loan.getLoanAmount(); break;
                case DEFAULTED: defaulted++; totalOutstanding += loan.getRemainingBalance(); break;
                case REJECTED: rejected++; break;
                case PENDING: pending++; break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append(drawBoxLine("LOAN PERFORMANCE REPORT", 42));
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append(drawBoxRow("Total Loans", String.valueOf(total), 42));
        sb.append(drawBoxRow("Active", String.valueOf(active), 42));
        sb.append(drawBoxRow("Fully Paid", String.valueOf(paid), 42));
        sb.append(drawBoxRow("Defaulted", String.valueOf(defaulted), 42));
        sb.append(drawBoxRow("Rejected", String.valueOf(rejected), 42));
        sb.append(drawBoxRow("Pending", String.valueOf(pending), 42));
        sb.append("╠══════════════════════════════════════════╣\n");
        String sym = CurrencyUtil.getCurrencySymbol(null);
        sb.append(drawBoxRow("Total Disbursed", sym + ValidationUtil.formatAmount(totalDisbursed), 42));
        sb.append(drawBoxRow("Outstanding", sym + ValidationUtil.formatAmount(totalOutstanding), 42));
        
        if (total > 0) {
            String rateStr = String.format("%.1f%%", (defaulted * 100.0) / total);
            sb.append(drawBoxRow("Default Rate", rateStr, 42));
        }
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Helper to draw a box row with label and value perfectly aligned within width.
     */
    private String drawBoxRow(String label, String value, int width) {
        String left = " " + label;
        String right = value + " ";
        int fill = width - left.length() - right.length() - 3; // 3 for " : "
        if (fill < 1) fill = 1;
        return "║" + left + " ".repeat(fill) + " : " + right + "║\n";
    }

    /**
     * Helper to draw a centered line within box width.
     */
    private String drawBoxLine(String text, int width) {
        int left = (width - text.length()) / 2;
        int right = width - text.length() - left;
        return "║" + " ".repeat(left) + text + " ".repeat(right) + "║\n";
    }

    /**
     * Generates a compliance/audit report summary.
     * @return Formatted audit activity report
     */
    public String generateAuditReport() {
        List<String> auditLines = FileHandler.readAllLines(FileHandler.AUDIT_LOG_FILE);
        int recentCount = Math.min(auditLines.size(), 30);
        List<String> recentLines = new ArrayList<>();
        int maxLineLen = 40; // Base min width
        
        for (int i = Math.max(0, auditLines.size() - recentCount); i < auditLines.size(); i++) {
            String line = auditLines.get(i).trim();
            recentLines.add(line);
            maxLineLen = Math.max(maxLineLen, line.length());
        }

        int contentWidth = Math.min(100, maxLineLen + 4);
        String title = "COMPLIANCE & AUDIT REPORT";
        
        StringBuilder sb = new StringBuilder();
        String border = "═".repeat(contentWidth);
        String subBorder = "─".repeat(contentWidth);

        sb.append("\n╔").append(border).append("╗\n");
        sb.append(drawBoxLine(title, contentWidth));
        sb.append("╠").append(border).append("╣\n");
        sb.append(drawBoxRow("Total Audit Entries", String.valueOf(auditLines.size()), contentWidth));
        sb.append(drawBoxRow("Report Generated", DateUtil.getCurrentDateTime(), contentWidth));
        sb.append("╠").append(border).append("╣\n");
        
        String recentHeader = "Recent Activity (last " + recentLines.size() + " entries):";
        sb.append("║ ").append(recentHeader).append(" ".repeat(Math.max(0, contentWidth - recentHeader.length() - 1))).append("║\n");
        sb.append("║").append(subBorder).append("║\n");

        for (String entry : recentLines) {
            String truncEntry = entry.length() > contentWidth - 2 ? entry.substring(0, contentWidth - 5) + "..." : entry;
            sb.append("║ ").append(truncEntry).append(" ".repeat(Math.max(0, contentWidth - truncEntry.length() - 1))).append("║\n");
        }

        sb.append("╚").append(border).append("╝\n");
        return sb.toString();
    }

    /**
     * Helper to draw a table row with 5 columns for the transaction statement.
     */
    private String drawBoxTableLine(String c1, String c2, String c3, String c4, String c5, int totalWidth) {
        // widths: 10 | 15 | 12 | 12 | 11 (+ 10 spaces + 4 bars = 74)
        return String.format("║ %-10s │ %-15s │ %-12s │ %-12s │ %-11s ║\n",
                truncate(c1, 10), truncate(c2, 15), truncate(c3, 12), truncate(c4, 12), truncate(c5, 11));
    }

    private String truncate(String text, int len) {
        if (text == null) return "";
        if (text.length() <= len) return text;
        return text.substring(0, len - 2) + "..";
    }

    /**
     * Saves a generated report content to a text file.
     * @param accountNumber Account number associated with the report
     * @param content Report content to save
     */
    public void saveStatementToFile(String accountNumber, String content) {
        String timestamp = DateUtil.getCurrentDateTime().replace(":", "-").replace(" ", "_");
        String filename = "data/statement_" + accountNumber + "_" + timestamp + ".txt";
        FileHandler.writeStatement(filename, content);
    }
}
