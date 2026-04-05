package com.rajarata.bank.services;

import com.rajarata.bank.models.account.Account;
import com.rajarata.bank.models.transaction.*;
import com.rajarata.bank.models.loan.*;
import com.rajarata.bank.models.user.*;
import com.rajarata.bank.utils.*;

import java.util.*;

/**
 * Service class for generating various reports and account statements.
 * Supports customer activity, loan performance, transaction analysis,
 * and compliance/audit reports.
 * 
 * OOP Concept: Reportable Interface - Works with entities implementing
 * the Reportable interface to generate polymorphic reports.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public class ReportingService {

    private final AccountService accountService;
    private final AuthenticationService authService;
    private final LoanService loanService;

    public ReportingService(AccountService accountService,
                            AuthenticationService authService,
                            LoanService loanService) {
        this.accountService = accountService;
        this.authService = authService;
        this.loanService = loanService;
    }

    /**
     * Generates a monthly account statement with period-specific interest summary.
     * 
     * OOP Concept: Polymorphism - Works with any Account subtype through the
     * Account base class, displaying type-specific details like interest rates.
     */
    public String generateMonthlyStatement(String accountNumber, int month, int year) {
        try {
            Account account = accountService.getAccount(accountNumber);
            String startDate = String.format("%04d-%02d-01", year, month);
            String endDate = String.format("%04d-%02d-28", year, month);

            Customer customer = authService.getCustomer(account.getCustomerId());
            String customerName = customer != null ? customer.getFullName() : "Unknown";

            List<Transaction> txns = account.getTransactionsByDateRange(startDate, endDate);

            StringBuilder sb = new StringBuilder();
            sb.append("╔══════════════════════════════════════════════════════════════╗\n");
            sb.append("║              RAJARATA DIGITAL BANK                          ║\n");
            sb.append("║           Monthly Account Statement                         ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Account Number : %-43s ║\n", accountNumber));
            sb.append(String.format("║ Account Holder : %-43s ║\n", customerName));
            sb.append(String.format("║ Account Type   : %-43s ║\n", account.getAccountType()));
            sb.append(String.format("║ Currency       : %-43s ║\n", account.getCurrency()));
            sb.append(String.format("║ Interest Rate  : %-43s ║\n",
                    String.format("%.2f%% per annum", account.getInterestRate() * 100)));
            sb.append(String.format("║ Statement Date : %-43s ║\n", DateUtil.getCurrentDate()));
            sb.append(String.format("║ Period         : %02d/%04d                                    ║\n", month, year));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");

            // Calculate statistics — separating interest credits from other credits/debits
            double totalDeposits = 0, totalWithdrawals = 0, periodInterestEarned = 0;
            double openingBalance = account.getBalance();
            int depositCount = 0, withdrawalCount = 0, interestCount = 0;

            for (Transaction txn : txns) {
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
            // Estimate opening balance (reverse-engineer from closing balance)
            openingBalance = account.getBalance() - totalDeposits - periodInterestEarned + totalWithdrawals;

            sb.append(String.format("║ Opening Balance: %s %-40s ║\n", account.getCurrency(), ValidationUtil.formatAmount(openingBalance)));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append("║  Date       │ Type         │     Amount │ Balance    │ Desc ║\n");
            sb.append("║─────────────┼──────────────┼────────────┼────────────┼──────║\n");

            double runningBalance = openingBalance;
            for (Transaction txn : txns) {
                if (txn.getStatus() != TransactionStatus.COMPLETED) continue;
                String sign = (txn.getType() == TransactionType.DEPOSIT ||
                               txn.getType() == TransactionType.LOAN_DISBURSEMENT ||
                               txn.getType() == TransactionType.INTEREST_CREDIT) ? "+" : "-";
                if (sign.equals("+")) runningBalance += txn.getAmount();
                else runningBalance -= txn.getAmount();

                String desc = txn.getDescription() != null ?
                    (txn.getDescription().length() > 5 ? txn.getDescription().substring(0, 5) : txn.getDescription()) : "";
                sb.append(String.format("║  %-10s │ %-12s │ %s%9s │ %10s │ %-4s ║\n",
                        txn.getTimestamp().substring(0, 10),
                        txn.getType().getDisplayName().length() > 12 ?
                            txn.getType().getDisplayName().substring(0, 12) : txn.getType().getDisplayName(),
                        sign,
                        ValidationUtil.formatAmount(txn.getAmount()),
                        ValidationUtil.formatAmount(runningBalance),
                        desc));
            }

            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Closing Balance   : %s %-38s ║\n", account.getCurrency(), ValidationUtil.formatAmount(account.getBalance())));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append("║                    TRANSACTION SUMMARY                      ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Total Deposits    : %s %-38s ║\n", account.getCurrency(), ValidationUtil.formatAmount(totalDeposits)));
            sb.append(String.format("║   # of Deposits   : %-41d ║\n", depositCount));
            sb.append(String.format("║ Total Withdrawals : %s %-38s ║\n", account.getCurrency(), ValidationUtil.formatAmount(totalWithdrawals)));
            sb.append(String.format("║   # of Withdrawals: %-41d ║\n", withdrawalCount));
            sb.append(String.format("║ Total Transactions: %-41d ║\n", txns.size()));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append("║                    INTEREST SUMMARY                         ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Interest Rate     : %-41s ║\n",
                    String.format("%.2f%% per annum", account.getInterestRate() * 100)));
            sb.append(String.format("║ Interest This Period : %s %-36s ║\n",
                    account.getCurrency(), ValidationUtil.formatAmount(periodInterestEarned)));
            sb.append(String.format("║   # of Credits    : %-41d ║\n", interestCount));
            sb.append(String.format("║ Total Interest (Lifetime): %s %-31s ║\n",
                    account.getCurrency(), ValidationUtil.formatAmount(account.getTotalInterestEarned())));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append("║     This is a computer-generated statement.                 ║\n");
            sb.append("║     For queries, contact: support@rajarata.com              ║\n");
            sb.append("╚══════════════════════════════════════════════════════════════╝\n");

            return sb.toString();

        } catch (Exception e) {
            return "Error generating statement: " + e.getMessage();
        }
    }

    /**
     * Generates system statistics summary.
     */
    public String generateSystemStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append("║        SYSTEM STATISTICS                 ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append(String.format("║ Total Users     : %-22d ║\n", authService.getUserCount()));
        sb.append(String.format("║ Total Accounts  : %-22d ║\n", accountService.getAccountCount()));
        sb.append(String.format("║ Total Loans     : %-22d ║\n", loanService.getAllLoans().size()));

        // Calculate total balances per currency
        java.util.Map<String, Double> currencyTotals = new java.util.TreeMap<>();
        for (Account acc : accountService.getAllAccounts().values()) {
            currencyTotals.merge(acc.getCurrency(), acc.getBalance(), Double::sum);
        }
        for (java.util.Map.Entry<String, Double> entry : currencyTotals.entrySet()) {
            sb.append(String.format("║ Total %-3s       : %-22s ║\n",
                    entry.getKey(), ValidationUtil.formatAmount(entry.getValue())));
        }

        // Active loans
        long activeLoans = loanService.getAllLoans().values().stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE).count();
        sb.append(String.format("║ Active Loans    : %-22d ║\n", activeLoans));

        sb.append(String.format("║ Report Date     : %-22s ║\n", DateUtil.getCurrentDate()));
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Generates a loan performance report.
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
        sb.append("║      LOAN PERFORMANCE REPORT             ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append(String.format("║ Total Loans     : %-22d ║\n", total));
        sb.append(String.format("║ Active          : %-22d ║\n", active));
        sb.append(String.format("║ Fully Paid      : %-22d ║\n", paid));
        sb.append(String.format("║ Defaulted       : %-22d ║\n", defaulted));
        sb.append(String.format("║ Rejected        : %-22d ║\n", rejected));
        sb.append(String.format("║ Pending         : %-22d ║\n", pending));
        sb.append("╠══════════════════════════════════════════╣\n");
        String sym = CurrencyUtil.getCurrencySymbol(null);
        sb.append(String.format("║ Total Disbursed : %s%-20s ║\n", sym, ValidationUtil.formatAmount(totalDisbursed)));
        sb.append(String.format("║ Outstanding     : %s%-20s ║\n", sym, ValidationUtil.formatAmount(totalOutstanding)));
        if (total > 0) {
            sb.append(String.format("║ Default Rate    : %.1f%%                         ║\n",
                    (defaulted * 100.0) / total));
        }
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Generates a compliance/audit report from audit log.
     */
    public String generateAuditReport() {
        List<String> auditLines = FileHandler.readAllLines(FileHandler.AUDIT_LOG_FILE);
        int recentCount = Math.min(auditLines.size(), 50); // Last 50 entries

        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║              COMPLIANCE & AUDIT REPORT                      ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Total Audit Entries : %-39d ║\n", auditLines.size()));
        sb.append(String.format("║ Report Generated    : %-39s ║\n", DateUtil.getCurrentDateTime()));
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        sb.append("║ Recent Activity (last " + recentCount + " entries):                        ║\n");
        sb.append("║──────────────────────────────────────────────────────────────║\n");

        for (int i = Math.max(0, auditLines.size() - recentCount); i < auditLines.size(); i++) {
            String entry = auditLines.get(i);
            if (entry.length() > 62) entry = entry.substring(0, 59) + "...";
            sb.append(String.format("║ %-60s ║\n", entry));
        }

        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Saves a statement to file.
     */
    public void saveStatementToFile(String accountNumber, String content) {
        String filename = "data/statement_" + accountNumber + "_" + DateUtil.getTransactionDate() + ".txt";
        FileHandler.writeStatement(filename, content);
    }
}
