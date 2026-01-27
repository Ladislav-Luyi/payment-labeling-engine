package com.paymentlabeling.service;

import com.paymentlabeling.model.Payment;
import com.paymentlabeling.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Payment operations (Issue #5)
 * Provides CRUD operations and business logic for payment management
 */
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Save or update a payment
     *
     * @param payment Payment to be saved
     * @return Saved payment with generated ID
     */
    @Override
    public Payment savePayment(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }
        
        if (payment.getAmount() == null) {
            throw new IllegalArgumentException("Payment amount is required");
        }
        
        if (payment.getPaymentDate() == null) {
            throw new IllegalArgumentException("Payment date is required");
        }
        
        if (payment.getCurrency() == null) {
            throw new IllegalArgumentException("Payment currency is required");
        }
        
        return paymentRepository.save(payment);
    }

    /**
     * Get all payments
     *
     * @return List of all payments
     */
    @Override
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Get payment by ID
     *
     * @param id ID of the payment to retrieve
     * @return Payment if found
     */
    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid payment ID");
        }
        
        return paymentRepository.findById(id).orElse(null);
    }

    /**
     * Delete a payment by ID
     *
     * @param id ID of the payment to delete
     */
    @Override
    public void deletePayment(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid payment ID");
        }
        
        if (!paymentRepository.existsById(id)) {
            throw new IllegalArgumentException("Payment with ID " + id + " not found");
        }
        
        paymentRepository.deleteById(id);
    }

    /**
     * Check if payment already exists (duplicate detection)
     * Checks based on unique constraint: (payment_date, amount, reference, account_number)
     *
     * @param payment Payment to check
     * @return true if payment exists, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean paymentExists(Payment payment) {
        if (payment == null) {
            return false;
        }
        
        // Find payments with same date, amount, reference and account number
        return paymentRepository.findAll().stream()
            .anyMatch(p -> p.getPaymentDate().equals(payment.getPaymentDate()) &&
                          p.getAmount().equals(payment.getAmount()) &&
                          (p.getReference() == null ? payment.getReference() == null : p.getReference().equals(payment.getReference())) &&
                          (p.getAccountNumber() == null ? payment.getAccountNumber() == null : p.getAccountNumber().equals(payment.getAccountNumber())));
    }
}
