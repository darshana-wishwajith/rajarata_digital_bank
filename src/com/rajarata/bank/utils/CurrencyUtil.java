package com.rajarata.bank.utils;

/**
 * Utility class for currency-related operations and formatting.
 * 
 * @author Rajarata University Student
 * @version 1.0
 */
public final class CurrencyUtil {

    private CurrencyUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Returns the currency symbol for a given currency code.
     * 
     * @param currency The currency code (USD, EUR, GBP, LKR)
     * @return The corresponding symbol or currency code if symbol not found
     */
    public static String getCurrencySymbol(String currency) {
        if (currency == null) return "Rs.";
        switch (currency.toUpperCase()) {
            case "USD": return "$";
            case "EUR": return "€";
            case "GBP": return "£";
            case "LKR": return "Rs.";
            default: return currency;
        }
    }

    /**
     * Formats an amount with its currency symbol/code.
     * E.g., "Rs. 1,000.00" or "$ 120.50"
     * 
     * @param amount The monetary amount
     * @param currency The currency code
     * @return Formatted currency string
     */
    public static String formatWithSymbol(double amount, String currency) {
        return getCurrencySymbol(currency) + " " + ValidationUtil.formatAmount(amount);
    }
}

