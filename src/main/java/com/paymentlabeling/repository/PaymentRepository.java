package com.paymentlabeling.repository;

import com.paymentlabeling.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find a payment by its unique constraint fields
     */
    Optional<Payment> findByPaymentDateAndAmountAndReferenceAndAccountNumber(
            LocalDate paymentDate, BigDecimal amount, String reference, String accountNumber);

    /**
     * Find all payments for a specific date
     */
    List<Payment> findByPaymentDate(LocalDate paymentDate);

    /**
     * Find all payments within a date range
     */
    List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Check if payment exists (for duplicate detection)
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.paymentDate = :paymentDate AND p.amount = :amount AND p.reference = :reference AND p.accountNumber = :accountNumber")
    boolean existsByPaymentDateAndAmountAndReferenceAndAccountNumber(
            @Param("paymentDate") LocalDate paymentDate,
            @Param("amount") BigDecimal amount,
            @Param("reference") String reference,
            @Param("accountNumber") String accountNumber);
}
