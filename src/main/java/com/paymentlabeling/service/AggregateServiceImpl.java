package com.paymentlabeling.service;

import com.paymentlabeling.model.Aggregate;
import com.paymentlabeling.model.Label;
import com.paymentlabeling.repository.AggregateRepository;
import com.paymentlabeling.repository.LabelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of AggregateService
 * Provides business logic for aggregation and reporting operations
 */
@Service
@Transactional
public class AggregateServiceImpl implements AggregateService {

    @Autowired
    private AggregateRepository aggregateRepository;

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
     * Get aggregate by label, year, and month
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Aggregate> getAggregateByLabelYearMonth(Long labelId, Integer year, Integer month) {
        return aggregateRepository.findByLabelIdAndYearAndMonth(labelId, year, month);
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
     * Calculate aggregates from payments for a specific label, month, and year
     * This would be called to generate aggregates from payment data
     * For now, this is a placeholder that can be extended to calculate from payment data
     */
    @Override
    public Aggregate calculateAndSaveAggregate(Long labelId, Integer year, Integer month) {
        // Validate label exists
        Optional<Label> label = labelRepository.findById(labelId);
        if (label.isEmpty()) {
            throw new IllegalArgumentException("Label not found with ID: " + labelId);
        }

        // Check if aggregate already exists for this label, year, month combination
        Optional<Aggregate> existing = aggregateRepository.findByLabelIdAndYearAndMonth(labelId, year, month);
        
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new aggregate - calculation logic would go here
        // For now, this creates a placeholder aggregate that can be updated
        Aggregate aggregate = new Aggregate();
        aggregate.setLabel(label.get());
        aggregate.setYear(year);
        aggregate.setMonth(month);
        
        return aggregateRepository.save(aggregate);
    }
}
