package com.paymentlabeling.repository;

import com.paymentlabeling.model.AggregatePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AggregatePayment entity
 */
@Repository
public interface AggregatePaymentRepository extends JpaRepository<AggregatePayment, Long> {

    /**
     * Find all payments for a specific aggregate
     */
    List<AggregatePayment> findByAggregateId(Long aggregateId);

    /**
     * Find aggregate payment by aggregate ID and payment ID
     */
    Optional<AggregatePayment> findByAggregateIdAndPaymentId(Long aggregateId, Long paymentId);

    /**
     * Find all aggregates for a specific payment
     */
    List<AggregatePayment> findByPaymentId(Long paymentId);

    /**
     * Check if an aggregate payment exists
     */
    boolean existsByAggregateIdAndPaymentId(Long aggregateId, Long paymentId);

    /**
     * Delete all aggregate payments for a specific aggregate
     */
    void deleteByAggregateId(Long aggregateId);

    /**
     * Delete all aggregate payments for a specific payment
     */
    void deleteByPaymentId(Long paymentId);
}

