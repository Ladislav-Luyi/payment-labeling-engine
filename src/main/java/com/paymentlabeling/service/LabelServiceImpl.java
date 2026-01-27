package com.paymentlabeling.service;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.repository.LabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Label operations
 */
@Service
@Transactional
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;

    public LabelServiceImpl(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    @Override
    public Label saveLabel(Label label) {
        if (label == null) {
            throw new IllegalArgumentException("Label cannot be null");
        }
        return labelRepository.save(label);
    }

    @Override
    public List<Label> getAllLabels() {
        return labelRepository.findAll();
    }

    @Override
    public Label getLabelById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Label ID must be a positive number");
        }
        return labelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Label with ID " + id + " not found"));
    }

    @Override
    public void deleteLabel(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Label ID must be a positive number");
        }
        if (!labelRepository.existsById(id)) {
            throw new IllegalArgumentException("Label with ID " + id + " not found");
        }
        labelRepository.deleteById(id);
    }

    @Override
    public Label findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Label name cannot be null or empty");
        }
        return labelRepository.findByName(name)
            .orElseThrow(() -> new IllegalArgumentException("Label with name '" + name + "' not found"));
    }

    /**
     * Check if label exists by name
     */
    public boolean existsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return labelRepository.existsByName(name);
    }
}
