package com.paymentlabeling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the junction table between Payments and Labels (many-to-many)
 */
@Entity
@Table(name = "payment_labels", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"payment_id", "label_id"}, name = "uk_payment_label_unique")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;
}
