package com.paymentlabeling.service;

import com.paymentlabeling.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object from CSV parsing operation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvParseResult {

    /**
     * List of successfully parsed payment records
     */
    @Builder.Default
    private List<Payment> parsedPayments = new ArrayList<>();

    /**
     * Number of duplicate records encountered
     */
    private int skippedDuplicates;

    /**
     * List of error messages during parsing
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * Whether parsing was successful (no critical errors)
     */
    private boolean success;

    /**
     * Add an error message
     */
    public void addError(String error) {
        errors.add(error);
        this.success = false;
    }
}
