package com.paymentlabeling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the junction table between Aggregates and Payments (many-to-many)
 * Tracks which payments belong to which aggregates
 */
@Entity
@Table(name = "aggregate_payments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"aggregate_id", "payment_id"}, name = "uk_aggregate_payment_unique")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aggregate_id", nullable = false)
    private Aggregate aggregate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // Manually added getters and setters since Lombok is not working properly
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Aggregate getAggregate() {
        return aggregate;
    }

    public void setAggregate(Aggregate aggregate) {
        this.aggregate = aggregate;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    // Builder pattern
    public static AggregatePaymentBuilder builder() {
        return new AggregatePaymentBuilder();
    }

    public static class AggregatePaymentBuilder {
        private Long id;
        private Aggregate aggregate;
        private Payment payment;

        public AggregatePaymentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AggregatePaymentBuilder aggregate(Aggregate aggregate) {
            this.aggregate = aggregate;
            return this;
        }

        public AggregatePaymentBuilder payment(Payment payment) {
            this.payment = payment;
            return this;
        }

        public AggregatePayment build() {
            AggregatePayment aggregatePayment = new AggregatePayment();
            aggregatePayment.id = this.id;
            aggregatePayment.aggregate = this.aggregate;
            aggregatePayment.payment = this.payment;
            return aggregatePayment;
        }
    }
}

