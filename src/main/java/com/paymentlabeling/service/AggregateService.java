package com.paymentlabeling.service;

import com.paymentlabeling.model.Aggregate;
import com.paymentlabeling.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service interface for Aggregate operations
 * Handles aggregation logic for payments grouped by label sets, month, and year
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
     * Get aggregates by year
     */
    List<Aggregate> getAggregatesByYear(Integer year);

    /**
     * Get aggregates by year and month
     */
    List<Aggregate> getAggregatesByYearAndMonth(Integer year, Integer month);

    /**
     * Get aggregates that contain a specific label
     */
    List<Aggregate> getAggregatesByLabel(Long labelId);

    /**
     * Get an aggregate by its label set, year, and month
     */
    Optional<Aggregate> getAggregateByLabelSetYearMonth(Set<Long> labelIds, Integer year, Integer month);

    /**
     * Save or update an aggregate
     */
    Aggregate saveAggregate(Aggregate aggregate);

    /**
     * Delete aggregate by ID
     */
    void deleteAggregate(Long id);

    /**
     * Recalculate all aggregates based on payment labels
     * This scans all payments and creates/updates aggregates based on their label combinations
     */
    void recalculateAggregates();

    /**
     * Get payments for a specific aggregate
     */
    List<Payment> getPaymentsForAggregate(Long aggregateId);

    /**
     * Add a payment to an aggregate
     */
    void addPaymentToAggregate(Long aggregateId, Long paymentId);

    /**
     * Remove a payment from an aggregate
     */
    void removePaymentFromAggregate(Long aggregateId, Long paymentId);

    /**
     * Calculate aggregates for a specific payment (may belong to multiple aggregates)
     */
    void calculateAggregatesForPayment(Long paymentId);
}
