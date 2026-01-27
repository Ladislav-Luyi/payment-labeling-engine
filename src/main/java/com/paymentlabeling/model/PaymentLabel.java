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

    // Manually added getters and setters since Lombok is not working properly
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    // Builder pattern
    public static PaymentLabelBuilder builder() {
        return new PaymentLabelBuilder();
    }

    public static class PaymentLabelBuilder {
        private Long id;
        private Payment payment;
        private Label label;

        public PaymentLabelBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PaymentLabelBuilder payment(Payment payment) {
            this.payment = payment;
            return this;
        }

        public PaymentLabelBuilder label(Label label) {
            this.label = label;
            return this;
        }

        public PaymentLabel build() {
            PaymentLabel paymentLabel = new PaymentLabel();
            paymentLabel.id = this.id;
            paymentLabel.payment = this.payment;
            paymentLabel.label = this.label;
            return paymentLabel;
        }
    }
}
