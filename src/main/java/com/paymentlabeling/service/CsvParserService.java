package com.paymentlabeling.service;

import com.paymentlabeling.model.Payment;

import java.io.InputStream;
import java.util.List;

/**
 * Service interface for parsing CSV files containing bank transactions
 */
public interface CsvParserService {

    /**
     * Parse a CSV input stream containing Slovak bank statement data
     * 
     * @param csvStream the input stream of the CSV file
     * @return CsvParseResult containing parsed payments and errors
     */
    CsvParseResult parseCsv(InputStream csvStream);

    /**
     * Save parsed payments to the database
     * 
     * @param payments list of payments to save
     */
    void savePayments(List<Payment> payments);

    /**
     * Save payments with duplicate detection
     * Skips payments that already exist in the database
     * 
     * @param payments list of payments to save
     * @return CsvImportResult with statistics about the import
     */
    CsvImportResult savePaymentsWithDuplicateDetection(List<Payment> payments);
}
