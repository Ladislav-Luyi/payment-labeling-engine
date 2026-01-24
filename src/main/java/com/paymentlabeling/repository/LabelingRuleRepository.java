package com.paymentlabeling.repository;

import com.paymentlabeling.model.LabelingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for LabelingRule entity
 */
@Repository
public interface LabelingRuleRepository extends JpaRepository<LabelingRule, Long> {

    /**
     * Find all active labeling rules
     */
    List<LabelingRule> findByIsActiveTrue();

    /**
     * Find all labeling rules for a specific label
     */
    List<LabelingRule> findByLabelId(Long labelId);
}
