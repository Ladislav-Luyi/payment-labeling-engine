package com.paymentlabeling.repository;

import com.paymentlabeling.model.PaymentLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PaymentLabel entity
 */
@Repository
public interface PaymentLabelRepository extends JpaRepository<PaymentLabel, Long> {

    /**
     * Find all labels for a payment
     */
    List<PaymentLabel> findByPaymentId(Long paymentId);

    /**
     * Find all payments with a specific label
     */
    List<PaymentLabel> findByLabelId(Long labelId);

    /**
     * Delete all labels for a payment
     */
    void deleteByPaymentId(Long paymentId);
}
