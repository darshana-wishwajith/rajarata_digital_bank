package com.rajarata.bank.interfaces;

/**
 * Interface defining reportable behavior for entities that can
 * generate summary reports of their data.
 * 
 * OOP Concept: Abstraction - Defines a contract for report generation
 * that can be implemented differently by accounts, customers, or system modules.
 * 
 * @author Rajarata Digital Bank Development Team
 * @version 1.0
 */
public interface Reportable {

    /**
     * Generates a text-based summary report of the entity.
     * 
     * @return Formatted string containing the report summary
     */
    String generateReport();

    /**
     * Generates a detailed report for a specific date range.
     * 
     * @param startDate Start date in "yyyy-MM-dd" format
     * @param endDate End date in "yyyy-MM-dd" format
     * @return Formatted string containing the detailed report
     */
    String generateDetailedReport(String startDate, String endDate);
}
