package com.paymentlabeling.controller;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.service.LabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API Controller for Label management
 * Provides endpoints for CRUD operations on labels
 */
@RestController
@RequestMapping("/api/labels")
public class LabelController {

    @Autowired
    private LabelService labelService;

    /**
     * GET /api/labels - Get all labels
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllLabels() {
        List<Label> labels = labelService.getAllLabels();
        List<Map<String, Object>> response = labels.stream()
            .map(this::buildLabelDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/labels/{id} - Get label by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLabelById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Label label = labelService.getLabelById(id);
            return ResponseEntity.ok(buildLabelDto(label));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/labels - Create a new label
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createLabel(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.getOrDefault("description", "");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Label name is required"));
            }

            Label label = new Label();
            label.setName(name);
            label.setDescription(description);

            Label saved = labelService.saveLabel(label);
            return ResponseEntity.status(HttpStatus.CREATED).body(buildLabelDto(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/labels/{id} - Update a label
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateLabel(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }

            Label label = labelService.getLabelById(id);
            if (label == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.containsKey("name")) {
                String name = (String) request.get("name");
                if (name == null || name.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Label name cannot be empty"));
                }
                label.setName(name);
            }

            if (request.containsKey("description")) {
                label.setDescription((String) request.get("description"));
            }

            Label updated = labelService.saveLabel(label);
            return ResponseEntity.ok(buildLabelDto(updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/labels/{id} - Delete a label
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }
            labelService.deleteLabel(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Build label DTO for response
     */
    private Map<String, Object> buildLabelDto(Label label) {
        return Map.of(
            "id", label.getId(),
            "name", label.getName(),
            "description", label.getDescription() != null ? label.getDescription() : "",
            "createdAt", label.getCreatedAt(),
            "updatedAt", label.getUpdatedAt()
        );
    }
}

