package com.paymentlabeling.service;

import com.paymentlabeling.model.Payment;
import java.util.List;

/**
 * Service interface for Payment operations
 */
public interface PaymentService {

    /**
     * Save a payment
     */
    Payment savePayment(Payment payment);

    /**
     * Get all payments
     */
    List<Payment> getAllPayments();

    /**
     * Get payment by ID
     */
    Payment getPaymentById(Long id);

    /**
     * Delete payment by ID
     */
    void deletePayment(Long id);

    /**
     * Check if payment already exists (duplicate detection)
     */
    boolean paymentExists(Payment payment);
}
