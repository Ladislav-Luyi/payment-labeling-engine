package com.paymentlabeling.service;

import com.paymentlabeling.model.Label;
import java.util.List;

/**
 * Service interface for Label operations
 */
public interface LabelService {

    /**
     * Save a label
     */
    Label saveLabel(Label label);

    /**
     * Get all labels
     */
    List<Label> getAllLabels();

    /**
     * Get label by ID
     */
    Label getLabelById(Long id);

    /**
     * Delete label by ID
     */
    void deleteLabel(Long id);

    /**
     * Find label by name
     */
    Label findByName(String name);
}
