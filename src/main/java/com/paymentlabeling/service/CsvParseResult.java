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

    // Manually added getters and setters since Lombok is not working properly
    public List<Payment> getParsedPayments() {
        return parsedPayments;
    }

    public void setParsedPayments(List<Payment> parsedPayments) {
        this.parsedPayments = parsedPayments;
    }

    public int getSkippedDuplicates() {
        return skippedDuplicates;
    }

    public void setSkippedDuplicates(int skippedDuplicates) {
        this.skippedDuplicates = skippedDuplicates;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}

