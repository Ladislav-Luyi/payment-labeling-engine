package com.paymentlabeling.service;

import com.paymentlabeling.model.LabelingRule;
import java.util.List;

/**
 * Service interface for LabelingRule operations
 */
public interface LabelingRuleService {

    /**
     * Save a labeling rule
     */
    LabelingRule saveLabelingRule(LabelingRule rule);

    /**
     * Get all labeling rules
     */
    List<LabelingRule> getAllRules();

    /**
     * Get all active labeling rules
     */
    List<LabelingRule> getActiveRules();

    /**
     * Get labeling rule by ID
     */
    LabelingRule getRuleById(Long id);

    /**
     * Delete labeling rule by ID
     */
    void deleteRule(Long id);

    /**
     * Get all rules for a label
     */
    List<LabelingRule> getRulesForLabel(Long labelId);
}
