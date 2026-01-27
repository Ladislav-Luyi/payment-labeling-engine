package com.paymentlabeling.service;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.LabelingRule;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.model.PaymentLabel;
import com.paymentlabeling.repository.PaymentLabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing payment label assignments.
 * Handles the automatic and manual labeling of payments based on rules.
 */
@Service
@Transactional
public class PaymentLabelService {

    private final PaymentLabelRepository paymentLabelRepository;
    private final LabelingRuleService labelingRuleService;

    public PaymentLabelService(PaymentLabelRepository paymentLabelRepository,
                              LabelingRuleService labelingRuleService) {
        this.paymentLabelRepository = paymentLabelRepository;
        this.labelingRuleService = labelingRuleService;
    }

    /**
     * Assign a label to a payment
     */
    public PaymentLabel assignLabelToPayment(Payment payment, Label label) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("Payment must be persisted with valid ID");
        }
        if (label == null || label.getId() == null) {
            throw new IllegalArgumentException("Label must be persisted with valid ID");
        }

        PaymentLabel paymentLabel = new PaymentLabel();
        paymentLabel.setPayment(payment);
        paymentLabel.setLabel(label);

        return paymentLabelRepository.save(paymentLabel);
    }

    /**
     * Automatically apply labeling rules to a payment.
     * Returns list of labels that were assigned based on matching rules.
     */
    public List<Label> applyAutoLabelsToPayment(Payment payment) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("Payment must be persisted with valid ID");
        }

        List<Label> appliedLabels = new ArrayList<>();
        String counterpartyName = payment.getCounterpartyName();

        // Skip if counterparty name is empty
        if (counterpartyName == null || counterpartyName.trim().isEmpty()) {
            System.out.println("Payment " + payment.getId() + " has no counterparty name. Skipping auto-labeling.");
            return appliedLabels;
        }

        // Get all active rules
        List<LabelingRule> activeRules = labelingRuleService.getActiveRules();

        // Track labels already assigned to avoid duplicates
        Set<Long> assignedLabelIds = new HashSet<>();

        // Try to match each rule
        for (LabelingRule rule : activeRules) {
            try {
                if (counterpartyName.matches(rule.getRegexPattern())) {
                    Label label = rule.getLabel();
                    
                    // Only assign if not already assigned
                    if (!assignedLabelIds.contains(label.getId())) {
                        assignLabelToPayment(payment, label);
                        appliedLabels.add(label);
                        assignedLabelIds.add(label.getId());
                        
                        System.out.println("Applied rule '" + rule.getName() + "' to payment " + payment.getId() 
                            + ". Assigned label '" + label.getName() + "'");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error applying rule '" + rule.getName() + "' to payment " + payment.getId() 
                    + ". Error: " + e.getMessage());
            }
        }

        if (!appliedLabels.isEmpty()) {
            System.out.println("Applied " + appliedLabels.size() + " labels to payment " + payment.getId());
        }

        return appliedLabels;
    }

    /**
     * Get all labels assigned to a payment
     */
    public List<Label> getLabelsForPayment(Payment payment) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("Payment must be persisted with valid ID");
        }

        return paymentLabelRepository.findByPaymentId(payment.getId()).stream()
            .map(PaymentLabel::getLabel)
            .toList();
    }

    /**
     * Check if payment has a specific label
     */
    public boolean hasLabel(Payment payment, Label label) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("Payment must be persisted with valid ID");
        }
        if (label == null || label.getId() == null) {
            throw new IllegalArgumentException("Label must be persisted with valid ID");
        }

        return paymentLabelRepository.findByPaymentId(payment.getId()).stream()
            .anyMatch(pl -> pl.getLabel().getId().equals(label.getId()));
    }

    /**
     * Remove a label from a payment
     */
    public void removeLabelFromPayment(Payment payment, Label label) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("Payment must be persisted with valid ID");
        }
        if (label == null || label.getId() == null) {
            throw new IllegalArgumentException("Label must be persisted with valid ID");
        }

        List<PaymentLabel> paymentLabels = paymentLabelRepository.findByPaymentId(payment.getId());
        paymentLabels.stream()
            .filter(pl -> pl.getLabel().getId().equals(label.getId()))
            .forEach(paymentLabelRepository::delete);
    }

    /**
     * Remove all labels from a payment
     */
    public void removeAllLabelsFromPayment(Payment payment) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("Payment must be persisted with valid ID");
        }

        paymentLabelRepository.deleteByPaymentId(payment.getId());
    }

    /**
     * Get all payments with a specific label
     */
    public List<Payment> getPaymentsWithLabel(Label label) {
        if (label == null || label.getId() == null) {
            throw new IllegalArgumentException("Label must be persisted with valid ID");
        }

        return paymentLabelRepository.findByLabelId(label.getId()).stream()
            .map(PaymentLabel::getPayment)
            .toList();
    }

    /**
     * Count labels assigned to a payment
     */
    public long countLabelsForPayment(Payment payment) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("Payment must be persisted with valid ID");
        }

        return paymentLabelRepository.findByPaymentId(payment.getId()).size();
    }
}
