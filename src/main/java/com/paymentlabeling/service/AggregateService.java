package com.paymentlabeling.service;

import com.paymentlabeling.model.Aggregate;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Aggregate operations (Issue #6)
 * Handles aggregation logic and reporting for payment data by label, month, and year
 */
public interface AggregateService {

    /**
     * Get aggregate by ID
     */
    Optional<Aggregate> getAggregateById(Long id);

    /**
     * Get all aggregates
     */
    List<Aggregate> getAllAggregates();

    /**
     * Get aggregates by label ID
     */
    List<Aggregate> getAggregatesByLabel(Long labelId);

    /**
     * Get aggregates by year
     */
    List<Aggregate> getAggregatesByYear(Integer year);

    /**
     * Get aggregates by year and month
     */
    List<Aggregate> getAggregatesByYearAndMonth(Integer year, Integer month);

    /**
     * Get aggregate by label, year, and month
     */
    Optional<Aggregate> getAggregateByLabelYearMonth(Long labelId, Integer year, Integer month);

    /**
     * Save or update an aggregate
     */
    Aggregate saveAggregate(Aggregate aggregate);

    /**
     * Delete aggregate by ID
     */
    void deleteAggregate(Long id);

    /**
     * Calculate aggregates from payments for a specific label, month, and year
     * This would be called to generate aggregates from payment data
     */
    Aggregate calculateAndSaveAggregate(Long labelId, Integer year, Integer month);
}
