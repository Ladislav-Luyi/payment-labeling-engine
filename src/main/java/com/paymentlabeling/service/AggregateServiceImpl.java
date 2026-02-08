package com.paymentlabeling.service;

import com.paymentlabeling.model.Aggregate;
import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.model.PaymentLabel;
import com.paymentlabeling.repository.AggregateRepository;
import com.paymentlabeling.repository.LabelRepository;
import com.paymentlabeling.repository.PaymentLabelRepository;
import com.paymentlabeling.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of AggregateService
 * Provides business logic for aggregating payments by label sets, month, and year
 */
@Service
@Transactional
public class AggregateServiceImpl implements AggregateService {

    @Autowired
    private AggregateRepository aggregateRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentLabelRepository paymentLabelRepository;

    @Autowired
    private LabelRepository labelRepository;

    /**
     * Get aggregate by ID
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Aggregate> getAggregateById(Long id) {
        return aggregateRepository.findById(id);
    }

    /**
     * Get all aggregates
     */
    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAllAggregates() {
        return aggregateRepository.findAll();
    }

    /**
     * Get aggregates by label ID
     */
    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAggregatesByLabel(Long labelId) {
        return aggregateRepository.findByLabelId(labelId);
    }

    /**
     * Get aggregates by year
     */
    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAggregatesByYear(Integer year) {
        return aggregateRepository.findByYear(year);
    }

    /**
     * Get aggregates by year and month
     */
    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAggregatesByYearAndMonth(Integer year, Integer month) {
        return aggregateRepository.findByYearAndMonth(year, month);
    }

    /**
     * Get aggregate by label set, year, and month
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Aggregate> getAggregateByLabelSetYearMonth(Set<Long> labelIds, Integer year, Integer month) {
        List<Long> sortedLabelIds = labelIds.stream().sorted().collect(Collectors.toList());
        return aggregateRepository.findByLabelSetYearAndMonth(sortedLabelIds, sortedLabelIds.size(), year, month);
    }

    /**
     * Save or update an aggregate
     */
    @Override
    public Aggregate saveAggregate(Aggregate aggregate) {
        return aggregateRepository.save(aggregate);
    }

    /**
     * Delete aggregate by ID
     */
    @Override
    public void deleteAggregate(Long id) {
        aggregateRepository.deleteById(id);
    }

    /**
     * Recalculate all aggregates based on payment labels
     * This scans all payments and creates/updates aggregates based on their label combinations
     */
    @Override
    public void recalculateAggregates() {
        // Get all payments with their labels
        List<Payment> allPayments = paymentRepository.findAll();

        // Clear existing aggregates (we'll rebuild them)
        aggregateRepository.deleteAll();

        // Map to track aggregates: key is "year-month-labelSetHash", value is aggregate
        Map<String, AggregateData> aggregateMap = new HashMap<>();

        for (Payment payment : allPayments) {
            // Get all labels for this payment
            List<PaymentLabel> paymentLabels = paymentLabelRepository.findByPaymentId(payment.getId());

            if (paymentLabels.isEmpty()) {
                continue; // Skip payments without labels
            }

            // Extract year and month from payment date
            LocalDate paymentDate = payment.getPaymentDate();
            Integer year = paymentDate.getYear();
            Integer month = paymentDate.getMonthValue();

            // Get sorted label IDs for this payment
            Set<Long> labelIds = paymentLabels.stream()
                    .map(pl -> pl.getLabel().getId())
                    .collect(Collectors.toSet());

            // Create a key for this aggregate group
            String key = createAggregateKey(year, month, labelIds);

            // Add to map or update existing
            aggregateMap.putIfAbsent(key, new AggregateData(year, month, labelIds));
            aggregateMap.get(key).addPayment(payment);
        }

        // Save all aggregates
        for (AggregateData aggData : aggregateMap.values()) {
            aggData.save(this);
        }
    }

    /**
     * Get payments for a specific aggregate
     */
    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForAggregate(Long aggregateId) {
        Optional<Aggregate> aggregate = aggregateRepository.findById(aggregateId);
        if (aggregate.isEmpty()) {
            return Collections.emptyList();
        }

        // Get all payments that have all the labels in this aggregate
        Aggregate agg = aggregate.get();
        Set<Long> aggregateLabelIds = agg.getLabels().stream()
                .map(l -> l.getId())
                .collect(Collectors.toSet());

        List<Payment> allPayments = paymentRepository.findAll();
        return allPayments.stream()
                .filter(payment -> {
                    List<PaymentLabel> paymentLabels = paymentLabelRepository.findByPaymentId(payment.getId());
                    Set<Long> paymentLabelIds = paymentLabels.stream()
                            .map(pl -> pl.getLabel().getId())
                            .collect(Collectors.toSet());

                    // Payment belongs to aggregate if it has the exact same label set
                    return paymentLabelIds.equals(aggregateLabelIds) &&
                            agg.getYear().equals(payment.getPaymentDate().getYear())  &&
                            agg.getMonth().equals(payment.getPaymentDate().getMonthValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * Add a payment to an aggregate (not typically needed - aggregates are calculated)
     */
    @Override
    public void addPaymentToAggregate(Long aggregateId, Long paymentId) {
        // This would be called if manually adding a payment to an aggregate
        // For now, aggregates are auto-calculated from payment labels
    }

    /**
     * Remove a payment from an aggregate (not typically needed)
     */
    @Override
    public void removePaymentFromAggregate(Long aggregateId, Long paymentId) {
        // This would be called if manually removing a payment from an aggregate
    }

    /**
     * Calculate aggregates for a specific payment
     * This is called when a payment's labels are modified
     */
    @Override
    public void calculateAggregatesForPayment(Long paymentId) {
        Optional<Payment> payment = paymentRepository.findById(paymentId);
        if (payment.isEmpty()) {
            return;
        }

        // Recalculate all aggregates (simpler approach)
        recalculateAggregates();
    }

    /**
     * Create a unique key for an aggregate group
     */
    private String createAggregateKey(Integer year, Integer month, Set<Long> labelIds) {
        String labelSetStr = labelIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return year + "-" + month + "-" + labelSetStr;
    }

    /**
     * Helper class to accumulate aggregate data before saving
     */
    private class AggregateData {
        Integer year;
        Integer month;
        Set<Long> labelIds;
        List<Payment> payments = new ArrayList<>();

        AggregateData(Integer year, Integer month, Set<Long> labelIds) {
            this.year = year;
            this.month = month;
            this.labelIds = labelIds;
        }

        void addPayment(Payment payment) {
            payments.add(payment);
        }

        void save(AggregateServiceImpl service) {
            // Create aggregate entity
            Aggregate aggregate = new Aggregate();
            aggregate.setYear(year);
            aggregate.setMonth(month);

            // Fetch and set labels
            Set<Label> labels = new HashSet<>();
            for (Long labelId : labelIds) {
                service.labelRepository.findById(labelId).ifPresent(labels::add);
            }
            aggregate.setLabels(labels);

            // Calculate totals
            BigDecimal totalAmount = payments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            aggregate.setTotalAmount(totalAmount);
            aggregate.setTransactionCount((long) payments.size());

            // Save aggregate
            service.aggregateRepository.save(aggregate);
        }
    }
}
