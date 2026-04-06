package com.rajarata.bank.services;

import com.rajarata.bank.utils.*;

import java.util.*;

/**
 * Service for multi-currency operations and exchange rate management.
 * Supports USD, EUR, GBP, and LKR with configurable exchange rates.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public class CurrencyService {

    /** Exchange rates relative to USD (base currency) */
    private final TreeMap<String, Double> exchangeRates;           // OOP: TreeMap for sorted storage

    /** Supported currencies */
    public static final String[] SUPPORTED_CURRENCIES = {"USD", "EUR", "GBP", "LKR"};

    public CurrencyService() {
        this.exchangeRates = new TreeMap<>();
        loadExchangeRates();
    }

    /**
     * Converts an amount from one currency to another.
     * Calculation: Amount in target = Amount in source / source rate * target rate
     * 
     * @param amount The amount to convert
     * @param sourceCurrency Source currency code
     * @param targetCurrency Target currency code
     * @return Converted amount in target currency
     */
    public double convert(double amount, String sourceCurrency, String targetCurrency) {
        if (sourceCurrency.equals(targetCurrency)) return amount;

        double sourceRate = exchangeRates.getOrDefault(sourceCurrency, 1.0);
        double targetRate = exchangeRates.getOrDefault(targetCurrency, 1.0);

        // Convert to USD first (base), then to target
        double amountInUSD = amount / sourceRate;
        return amountInUSD * targetRate;
    }

    /**
     * Gets the exchange rate between two currencies.
     * @param sourceCurrency Source currency
     * @param targetCurrency Target currency
     * @return Exchange rate (1 source = X target)
     */
    public double getExchangeRate(String sourceCurrency, String targetCurrency) {
        double sourceRate = exchangeRates.getOrDefault(sourceCurrency, 1.0);
        double targetRate = exchangeRates.getOrDefault(targetCurrency, 1.0);
        return targetRate / sourceRate;
    }

    /**
     * Updates an exchange rate (admin function).
     * @param currency Currency code
     * @param rateToUSD New rate relative to USD
     */
    public void updateExchangeRate(String currency, double rateToUSD) {
        exchangeRates.put(currency, rateToUSD);
        saveExchangeRates();
        FileHandler.logAudit("RATE_UPDATE", "Exchange rate updated: " + currency + " = " + rateToUSD);
    }

    /**
     * Gets a display of all current exchange rates.
     * @return Formatted exchange rate table
     */
    public String getExchangeRateDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════╗\n");
        sb.append("║        EXCHANGE RATES (Base: USD)        ║\n");
        sb.append("╠══════════════════════════════════════════╣\n");
        for (Map.Entry<String, Double> entry : exchangeRates.entrySet()) {
            sb.append(String.format("║   1 USD = %10.4f %-3s                ║\n",
                    entry.getValue(), entry.getKey()));
        }
        sb.append("╠══════════════════════════════════════════╣\n");
        sb.append("║   Cross Rates:                           ║\n");
        sb.append(String.format("║   1 EUR = %10.4f USD                ║\n",
                1.0 / exchangeRates.getOrDefault("EUR", 0.92)));
        sb.append(String.format("║   1 GBP = %10.4f USD                ║\n",
                1.0 / exchangeRates.getOrDefault("GBP", 0.79)));
        sb.append(String.format("║   1 USD = %10.4f LKR                ║\n",
                exchangeRates.getOrDefault("LKR", 320.5)));
        sb.append("╚══════════════════════════════════════════╝\n");
        return sb.toString();
    }

    /**
     * Checks if a currency is supported.
     * @param currency Currency code
     * @return true if supported
     */
    public boolean isSupportedCurrency(String currency) {
        for (String supported : SUPPORTED_CURRENCIES) {
            if (supported.equals(currency.toUpperCase())) return true;
        }
        return false;
    }

    /**
     * Loads exchange rates from file.
     */
    private void loadExchangeRates() {
        List<String> lines = FileHandler.readAllLines(FileHandler.EXCHANGE_RATES_FILE);
        for (String line : lines) {
            String[] parts = line.split(FileHandler.DELIMITER_REGEX);
            if (parts.length >= 2) {
                try {
                    exchangeRates.put(parts[0], Double.parseDouble(parts[1]));
                } catch (NumberFormatException e) { /* skip */ }
            }
        }
        // Ensure defaults exist
        exchangeRates.putIfAbsent("USD", 1.0);
        exchangeRates.putIfAbsent("EUR", 0.92);
        exchangeRates.putIfAbsent("GBP", 0.79);
        exchangeRates.putIfAbsent("LKR", 320.50);
    }

    /**
     * Saves exchange rates to file.
     */
    private void saveExchangeRates() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Double> entry : exchangeRates.entrySet()) {
            lines.add(entry.getKey() + "|" + String.format("%.4f", entry.getValue()));
        }
        FileHandler.writeAllLines(FileHandler.EXCHANGE_RATES_FILE, lines);
    }

    /** @return Map of all exchange rates */
    public Map<String, Double> getAllRates() { return new TreeMap<>(exchangeRates); }
}

