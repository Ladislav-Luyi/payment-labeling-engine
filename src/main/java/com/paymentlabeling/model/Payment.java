package com.paymentlabeling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a payment transaction from CSV
 */
@Entity
@Table(name = "payments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"payment_date", "amount", "reference", "account_number"}, name = "uk_payment_unique")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "transaction_type", length = 100)
    private String transactionType;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "counterparty_account", length = 50)
    private String counterpartyAccount;

    @Column(name = "counterparty_bank", length = 100)
    private String counterpartyBank;

    @Column(name = "counterparty_name", length = 255)
    private String counterpartyName;

    @Column(name = "receiver_info", columnDefinition = "TEXT")
    private String receiverInfo;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
