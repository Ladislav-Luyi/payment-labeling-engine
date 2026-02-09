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
import java.time.YearMonth;
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
        return aggregateRepository.findByPeriodEndDayIsNull();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAggregatesByPeriodEndDay(Integer periodEndDay) {
        if (periodEndDay == null) {
            return aggregateRepository.findByPeriodEndDayIsNull();
        }
        return aggregateRepository.findByPeriodEndDay(periodEndDay);
    }

    /**
     * Get aggregates by year
     */
    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAggregatesByYear(Integer year) {
        return aggregateRepository.findByYearAndPeriodEndDayIsNull(year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAggregatesByYearAndPeriodEndDay(Integer year, Integer periodEndDay) {
        if (periodEndDay == null) {
            return aggregateRepository.findByYearAndPeriodEndDayIsNull(year);
        }
        return aggregateRepository.findByYearAndPeriodEndDay(year, periodEndDay);
    }

    /**
     * Get aggregates by year and month
     */
    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAggregatesByYearAndMonth(Integer year, Integer month) {
        return aggregateRepository.findByYearAndMonthAndPeriodEndDayIsNull(year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAggregatesByLabel(Long labelId) {
        return aggregateRepository.findByLabelIdAndPeriodEndDayIsNull(labelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aggregate> getAggregatesByYearAndMonthAndPeriodEndDay(Integer year, Integer month, Integer periodEndDay) {
        if (periodEndDay == null) {
            return aggregateRepository.findByYearAndMonthAndPeriodEndDayIsNull(year, month);
        }
        return aggregateRepository.findByYearAndMonthAndPeriodEndDay(year, month, periodEndDay);
    }

    /**
     * Get aggregate by label set, year, and month
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Aggregate> getAggregateByLabelSetYearMonth(Set<Long> labelIds, Integer year, Integer month, Integer periodEndDay) {
        List<Long> sortedLabelIds = labelIds.stream().sorted().collect(Collectors.toList());
        return aggregateRepository.findByLabelSetYearAndMonth(sortedLabelIds, sortedLabelIds.size(), year, month, periodEndDay);
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
        recalculateAggregates(null);
    }

    @Override
    public void recalculateAggregates(Integer periodEndDay) {
        // Get all payments with their labels
        List<Payment> allPayments = paymentRepository.findAll();

        // Clear existing aggregates for this period type (we'll rebuild them)
        if (periodEndDay == null) {
            aggregateRepository.deleteByPeriodEndDayIsNull();
        } else {
            aggregateRepository.deleteByPeriodEndDay(periodEndDay);
        }

        // Map to track aggregates: key is "year-month-periodEndDay-labelSetHash", value is aggregate
        Map<String, AggregateData> aggregateMap = new HashMap<>();

        for (Payment payment : allPayments) {
            // Get all labels for this payment
            List<PaymentLabel> paymentLabels = paymentLabelRepository.findByPaymentId(payment.getId());

            if (paymentLabels.isEmpty()) {
                continue; // Skip payments without labels
            }

            // Extract year and month from payment date using period rules
            LocalDate paymentDate = payment.getPaymentDate();
            YearMonth periodYearMonth = resolvePeriodYearMonth(paymentDate, periodEndDay);
            Integer year = periodYearMonth.getYear();
            Integer month = periodYearMonth.getMonthValue();

            // Get sorted label IDs for this payment
            Set<Long> labelIds = paymentLabels.stream()
                    .map(pl -> pl.getLabel().getId())
                    .collect(Collectors.toSet());

            // Create a key for this aggregate group
            String key = createAggregateKey(year, month, periodEndDay, labelIds);

            // Add to map or update existing
            aggregateMap.putIfAbsent(key, new AggregateData(year, month, periodEndDay, labelIds));
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
        Integer periodEndDay = agg.getPeriodEndDay();

        List<Payment> allPayments = paymentRepository.findAll();
        return allPayments.stream()
                .filter(payment -> {
                    List<PaymentLabel> paymentLabels = paymentLabelRepository.findByPaymentId(payment.getId());
                    Set<Long> paymentLabelIds = paymentLabels.stream()
                            .map(pl -> pl.getLabel().getId())
                            .collect(Collectors.toSet());

                    if (!paymentLabelIds.equals(aggregateLabelIds)) {
                        return false;
                    }

                    YearMonth periodYearMonth = resolvePeriodYearMonth(payment.getPaymentDate(), periodEndDay);
                    return agg.getYear().equals(periodYearMonth.getYear()) &&
                            agg.getMonth().equals(periodYearMonth.getMonthValue());
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
    private String createAggregateKey(Integer year, Integer month, Integer periodEndDay, Set<Long> labelIds) {
        String labelSetStr = labelIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String periodKey = periodEndDay == null ? "monthly" : String.valueOf(periodEndDay);
        return year + "-" + month + "-" + periodKey + "-" + labelSetStr;
    }

    private YearMonth resolvePeriodYearMonth(LocalDate paymentDate, Integer periodEndDay) {
        YearMonth current = YearMonth.from(paymentDate);
        if (periodEndDay == null) {
            return current;
        }
        int effectiveEndDay = Math.min(periodEndDay, current.lengthOfMonth());
        if (paymentDate.getDayOfMonth() <= effectiveEndDay) {
            return current;
        }
        return YearMonth.from(paymentDate.plusMonths(1));
    }

    /**
     * Helper class to accumulate aggregate data before saving
     */
    private class AggregateData {
        Integer year;
        Integer month;
        Integer periodEndDay;
        Set<Long> labelIds;
        List<Payment> payments = new ArrayList<>();

        AggregateData(Integer year, Integer month, Integer periodEndDay, Set<Long> labelIds) {
            this.year = year;
            this.month = month;
            this.periodEndDay = periodEndDay;
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
            aggregate.setPeriodEndDay(periodEndDay);

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
