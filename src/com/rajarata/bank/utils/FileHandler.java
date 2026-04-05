package com.rajarata.bank.utils;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for all file I/O operations in the banking system.
 * Handles reading, writing, and managing data files for customers,
 * accounts, transactions, loans, and audit logs.
 * 
 * OOP Concept: File Handling - Comprehensive file I/O using BufferedReader,
 * BufferedWriter, and PrintWriter for data persistence.
 * 
 * OOP Concept: Encapsulation - All file paths and I/O logic are encapsulated
 * within this class. Other classes interact with data through this interface only.
 * 
 * OOP Concept: Static Members - File paths are defined as static constants,
 * methods are static for stateless access.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public final class FileHandler {

    /** Base directory for all data files */
    private static final String DATA_DIR = "data";

    /** File path for customer data */
    public static final String CUSTOMERS_FILE = DATA_DIR + File.separator + "customers.txt";
    /** File path for account data */
    public static final String ACCOUNTS_FILE = DATA_DIR + File.separator + "accounts.txt";
    /** File path for transaction data */
    public static final String TRANSACTIONS_FILE = DATA_DIR + File.separator + "transactions.txt";
    /** File path for loan data */
    public static final String LOANS_FILE = DATA_DIR + File.separator + "loans.txt";
    /** File path for notification data */
    public static final String NOTIFICATIONS_FILE = DATA_DIR + File.separator + "notifications.txt";
    /** File path for bill payee data */
    public static final String PAYEES_FILE = DATA_DIR + File.separator + "payees.txt";
    /** File path for scheduled payments */
    public static final String SCHEDULED_PAYMENTS_FILE = DATA_DIR + File.separator + "scheduled_payments.txt";
    /** File path for exchange rates */
    public static final String EXCHANGE_RATES_FILE = DATA_DIR + File.separator + "exchange_rates.txt";
    /** File path for fraud cases */
    public static final String FRAUD_CASES_FILE = DATA_DIR + File.separator + "fraud_cases.txt";
    /** File path for audit log (append-only) */
    public static final String AUDIT_LOG_FILE = DATA_DIR + File.separator + "audit.log";
    /** Delimiter used to separate fields in data files */
    public static final String DELIMITER = "|";
    /** Escaped delimiter for use in regex split */
    public static final String DELIMITER_REGEX = "\\|";

    /**
     * Private constructor to prevent instantiation.
     */
    private FileHandler() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Initializes the data directory and creates necessary files if they don't exist.
     * Should be called at application startup.
     */
    public static void initializeDataFiles() {
        try {
            // Create data directory if it doesn't exist
            Files.createDirectories(Paths.get(DATA_DIR));

            // Create each data file if it doesn't exist
            String[] files = {
                CUSTOMERS_FILE, ACCOUNTS_FILE, TRANSACTIONS_FILE,
                LOANS_FILE, NOTIFICATIONS_FILE, PAYEES_FILE,
                SCHEDULED_PAYMENTS_FILE, EXCHANGE_RATES_FILE,
                FRAUD_CASES_FILE, AUDIT_LOG_FILE
            };

            for (String filePath : files) {
                Path path = Paths.get(filePath);
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
            }

            // Initialize exchange rates if file is empty
            if (Files.size(Paths.get(EXCHANGE_RATES_FILE)) == 0) {
                initializeExchangeRates();
            }

        } catch (IOException e) {
            System.err.println("ERROR: Failed to initialize data files: " + e.getMessage());
        }
    }

    /**
     * Reads all lines from a specified data file.
     * 
     * @param filePath The path to the data file
     * @return List of strings, one per line, empty list if file doesn't exist
     */
    public static List<String> readAllLines(String filePath) {
        List<String> lines = new ArrayList<>();
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to read file " + filePath + ": " + e.getMessage());
        }

        return lines;
    }

    /**
     * Writes all lines to a specified data file, replacing existing content.
     * Creates a backup of the existing file before writing.
     * 
     * @param filePath The path to the data file
     * @param lines List of strings to write, one per line
     */
    public static void writeAllLines(String filePath, List<String> lines) {
        try {
            // Create backup before writing
            createBackup(filePath);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to write file " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Appends a single line to a data file. Used for adding new records
     * without rewriting the entire file.
     * 
     * @param filePath The path to the data file
     * @param line The line to append
     */
    public static void appendLine(String filePath, String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("ERROR: Failed to append to file " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Appends an audit log entry with timestamp. Audit log is append-only.
     * Format: [timestamp] | action | details
     * 
     * @param action The action being logged (e.g., "LOGIN", "DEPOSIT")
     * @param details The details of the action
     */
    public static void logAudit(String action, String details) {
        String timestamp = DateUtil.getCurrentDateTime();
        String logEntry = "[" + timestamp + "] " + DELIMITER + " " + action + " " + DELIMITER + " " + details;
        appendLine(AUDIT_LOG_FILE, logEntry);
    }

    /**
     * Updates a specific line in a file identified by a key field.
     * Searches for a line starting with the key and replaces it with the new data.
     * 
     * @param filePath The path to the data file
     * @param key The key to search for (typically the first field)
     * @param newLine The replacement line
     * @return true if the line was found and replaced
     */
    public static boolean updateLine(String filePath, String key, String newLine) {
        List<String> lines = readAllLines(filePath);
        boolean found = false;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(key + DELIMITER) || lines.get(i).startsWith(key)) {
                lines.set(i, newLine);
                found = true;
                break;
            }
        }

        if (found) {
            writeAllLines(filePath, lines);
        }
        return found;
    }

    /**
     * Deletes a line from a file identified by a key field.
     * 
     * @param filePath The path to the data file
     * @param key The key identifying the line to delete
     * @return true if the line was found and deleted
     */
    public static boolean deleteLine(String filePath, String key) {
        List<String> lines = readAllLines(filePath);
        boolean found = lines.removeIf(line -> line.startsWith(key + DELIMITER) || line.equals(key));

        if (found) {
            writeAllLines(filePath, lines);
        }
        return found;
    }

    /**
     * Finds all lines matching a key in any field position.
     * 
     * @param filePath The path to the data file
     * @param searchValue The value to search for
     * @return List of matching lines
     */
    public static List<String> findLines(String filePath, String searchValue) {
        List<String> allLines = readAllLines(filePath);
        List<String> matchingLines = new ArrayList<>();

        for (String line : allLines) {
            if (line.contains(searchValue)) {
                matchingLines.add(line);
            }
        }

        return matchingLines;
    }

    /**
     * Creates a backup copy of a data file.
     * Backup is stored with .bak extension in the same directory.
     * 
     * @param filePath The path to the file to backup
     */
    public static void createBackup(String filePath) {
        try {
            Path source = Paths.get(filePath);
            if (Files.exists(source) && Files.size(source) > 0) {
                Path backup = Paths.get(filePath + ".bak");
                Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("WARNING: Failed to create backup of " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Exports data to a CSV file format for external use.
     * 
     * @param outputPath The path for the output CSV file
     * @param headers CSV header row
     * @param data List of data rows
     */
    public static void exportToCsv(String outputPath, String headers, List<String> data) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println(headers);
            for (String row : data) {
                // Convert internal delimiter to CSV comma format
                writer.println(row.replace(DELIMITER, ","));
            }
            System.out.println("Data exported successfully to: " + outputPath);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to export data: " + e.getMessage());
        }
    }

    /**
     * Generates a formatted text statement file.
     * 
     * @param outputPath The path for the output file
     * @param content The formatted statement content
     */
    public static void writeStatement(String outputPath, String content) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.print(content);
            System.out.println("Statement generated: " + outputPath);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to generate statement: " + e.getMessage());
        }
    }

    /**
     * Initializes the exchange rates file with default rates.
     * Rates are relative to USD (base currency).
     */
    private static void initializeExchangeRates() {
        List<String> rates = new ArrayList<>();
        rates.add("USD" + DELIMITER + "1.0000");
        rates.add("EUR" + DELIMITER + "0.9200");
        rates.add("GBP" + DELIMITER + "0.7900");
        rates.add("LKR" + DELIMITER + "320.5000");
        writeAllLines(EXCHANGE_RATES_FILE, rates);
    }

    /**
     * Checks if the data directory and files exist and are accessible.
     * @return true if data infrastructure is healthy
     */
    public static boolean isDataHealthy() {
        try {
            Path dataDir = Paths.get(DATA_DIR);
            return Files.exists(dataDir) && Files.isDirectory(dataDir);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the total number of records in a data file.
     * @param filePath Path to the data file
     * @return Number of non-empty lines in the file
     */
    public static int getRecordCount(String filePath) {
        return readAllLines(filePath).size();
    }
}
