package com.paymentlabeling.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Result object from CSV import operation (parsing + saving + duplicate detection)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvImportResult {

    /**
     * Number of new payments successfully imported
     */
    private long newPaymentCount;

    /**
     * Number of duplicate payments skipped
     */
    private long duplicateCount;

    /**
     * List of error messages during import
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * Timestamp when import was completed
     */
    private LocalDateTime importedAt;

    /**
     * Total number of records processed (new + duplicates)
     */
    public long getTotalProcessed() {
        return newPaymentCount + duplicateCount;
    }

    /**
     * Add an error message
     */
    public void addError(String error) {
        errors.add(error);
    }
}
