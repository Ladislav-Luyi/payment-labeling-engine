package com.paymentlabeling.service;

import com.paymentlabeling.model.LabelingRule;
import com.paymentlabeling.repository.LabelingRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for LabelingRule operations
 */
@Service
@Transactional
public class LabelingRuleServiceImpl implements LabelingRuleService {

    private final LabelingRuleRepository labelingRuleRepository;

    public LabelingRuleServiceImpl(LabelingRuleRepository labelingRuleRepository) {
        this.labelingRuleRepository = labelingRuleRepository;
    }

    @Override
    public LabelingRule saveLabelingRule(LabelingRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Labeling rule cannot be null");
        }
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name cannot be null or empty");
        }
        if (rule.getRegexPattern() == null || rule.getRegexPattern().trim().isEmpty()) {
            throw new IllegalArgumentException("Regex pattern cannot be null or empty");
        }
        if (rule.getLabel() == null) {
            throw new IllegalArgumentException("Rule must have an associated label");
        }
        return labelingRuleRepository.save(rule);
    }

    @Override
    public List<LabelingRule> getAllRules() {
        return labelingRuleRepository.findAll();
    }

    @Override
    public List<LabelingRule> getActiveRules() {
        return labelingRuleRepository.findByIsActiveTrue();
    }

    @Override
    public LabelingRule getRuleById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Rule ID must be a positive number");
        }
        return labelingRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rule with ID " + id + " not found"));
    }

    @Override
    public void deleteRule(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Rule ID must be a positive number");
        }
        if (!labelingRuleRepository.existsById(id)) {
            throw new IllegalArgumentException("Rule with ID " + id + " not found");
        }
        labelingRuleRepository.deleteById(id);
    }

    @Override
    public List<LabelingRule> getRulesForLabel(Long labelId) {
        if (labelId == null || labelId <= 0) {
            throw new IllegalArgumentException("Label ID must be a positive number");
        }
        return labelingRuleRepository.findByLabelId(labelId);
    }

    /**
     * Count total number of rules
     */
    public long countRules() {
        return labelingRuleRepository.count();
    }

    /**
     * Count active rules
     */
    public long countActiveRules() {
        return getActiveRules().size();
    }
}
