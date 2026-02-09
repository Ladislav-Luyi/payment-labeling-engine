package com.paymentlabeling.controller;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.LabelingRule;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.service.LabelingRuleService;
import com.paymentlabeling.service.LabelService;
import com.paymentlabeling.service.PaymentService;
import com.paymentlabeling.service.PaymentLabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * REST API Controller for Labeling Rules management and automatic labeling
 * Provides endpoints for CRUD operations on labeling rules and applying rules to payments
 */
@RestController
@RequestMapping("/api/rules")
public class LabelingRuleController {

    @Autowired
    private LabelingRuleService labelingRuleService;

    @Autowired
    private LabelService labelService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentLabelService paymentLabelService;

    /**
     * GET /api/rules - Get all labeling rules
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRules() {
        List<LabelingRule> rules = labelingRuleService.getAllRules();
        List<Map<String, Object>> response = rules.stream()
            .map(this::buildRuleDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rules/active - Get all active labeling rules
     */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveRules() {
        List<LabelingRule> rules = labelingRuleService.getActiveRules();
        List<Map<String, Object>> response = rules.stream()
            .map(this::buildRuleDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rules/{id} - Get labeling rule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRuleById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        try {
            LabelingRule rule = labelingRuleService.getRuleById(id);
            return ResponseEntity.ok(buildRuleDto(rule));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/rules - Create a new labeling rule
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String regexPattern = (String) request.get("regexPattern");
            Long labelId = ((Number) request.get("labelId")).longValue();
            Boolean isActive = (Boolean) request.getOrDefault("isActive", true);
            String description = (String) request.getOrDefault("description", "");
            String matchingField = (String) request.getOrDefault("matchingField", "counterpartyName");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rule name is required"));
            }
            if (regexPattern == null || regexPattern.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Regex pattern is required"));
            }
            if (labelId == null || labelId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Valid label ID is required"));
            }

            // Validate regex pattern
            try {
                Pattern.compile(regexPattern);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid regex pattern: " + e.getMessage()));
            }

            Label label = labelService.getLabelById(labelId);
            if (label == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Label not found"));
            }

            LabelingRule rule = new LabelingRule();
            rule.setName(name);
            rule.setRegexPattern(regexPattern);
            rule.setLabel(label);
            rule.setIsActive(isActive);
            rule.setDescription(description);
            rule.setMatchingField(matchingField);

            LabelingRule saved = labelingRuleService.saveLabelingRule(rule);
            return ResponseEntity.status(HttpStatus.CREATED).body(buildRuleDto(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/rules/{id} - Update a labeling rule
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }

            LabelingRule rule = labelingRuleService.getRuleById(id);
            if (rule == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.containsKey("name")) {
                String name = (String) request.get("name");
                if (name == null || name.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Rule name cannot be empty"));
                }
                rule.setName(name);
            }

            if (request.containsKey("regexPattern")) {
                String regexPattern = (String) request.get("regexPattern");
                if (regexPattern == null || regexPattern.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Regex pattern cannot be empty"));
                }
                try {
                    Pattern.compile(regexPattern);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid regex pattern: " + e.getMessage()));
                }
                rule.setRegexPattern(regexPattern);
            }

            if (request.containsKey("labelId")) {
                Long labelId = ((Number) request.get("labelId")).longValue();
                Label label = labelService.getLabelById(labelId);
                if (label == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Label not found"));
                }
                rule.setLabel(label);
            }

            if (request.containsKey("isActive")) {
                rule.setIsActive((Boolean) request.get("isActive"));
            }

            if (request.containsKey("description")) {
                rule.setDescription((String) request.get("description"));
            }

            if (request.containsKey("matchingField")) {
                rule.setMatchingField((String) request.get("matchingField"));
            }

            LabelingRule updated = labelingRuleService.saveLabelingRule(rule);
            return ResponseEntity.ok(buildRuleDto(updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/rules/{id} - Delete a labeling rule
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }
            labelingRuleService.deleteRule(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/rules/test - Test a regex pattern against a payment field
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testRule(@RequestBody Map<String, Object> request) {
        try {
            String regexPattern = (String) request.get("regexPattern");
            String fieldValue = (String) request.get("fieldValue");

            if (regexPattern == null || regexPattern.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Regex pattern is required"));
            }
            if (fieldValue == null || fieldValue.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Field value is required"));
            }

            try {
                Pattern pattern = Pattern.compile(regexPattern);
                boolean matches = pattern.matcher(fieldValue).find();
                return ResponseEntity.ok(Map.of(
                    "matches", matches,
                    "pattern", regexPattern,
                    "value", fieldValue
                ));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid regex pattern: " + e.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/rules/apply - Apply labeling rules to payments
     * Body: {
     *   "paymentIds": [1, 2, 3],  // null or empty means apply to all payments
     *   "ruleIds": [1, 2]          // null or empty means apply all active rules
     * }
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyRules(@RequestBody(required = false) Map<String, Object> request) {
        try {
            List<Long> paymentIds = extractPaymentIds(request);
            List<Long> ruleIds = extractRuleIds(request);

            // If no specific payment IDs provided, get all payments
            List<Payment> paymentsToProcess;
            if (paymentIds == null || paymentIds.isEmpty()) {
                paymentsToProcess = paymentService.getAllPayments();
            } else {
                paymentsToProcess = paymentIds.stream()
                    .map(paymentService::getPaymentById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }

            // Get rules to apply
            List<LabelingRule> rulesToApply;
            if (ruleIds == null || ruleIds.isEmpty()) {
                rulesToApply = labelingRuleService.getActiveRules();
            } else {
                rulesToApply = ruleIds.stream()
                    .map(id -> {
                        try {
                            return labelingRuleService.getRuleById(id);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }

            int totalLabelsApplied = 0;
            Map<Long, Integer> paymentLabelCounts = new HashMap<>();

            // Apply rules to each payment
            for (Payment payment : paymentsToProcess) {
                int labelsForThisPayment = 0;

                for (LabelingRule rule : rulesToApply) {
                    String fieldValue = getPaymentFieldValue(payment, rule.getMatchingField());

                    if (fieldValue != null && !fieldValue.isEmpty()) {
                        try {
                            Pattern pattern = Pattern.compile(rule.getRegexPattern());
                            if (pattern.matcher(fieldValue).find()) {
                                // Check if label is not already assigned
                                boolean alreadyAssigned = paymentLabelService.getLabelsForPayment(payment)
                                    .stream()
                                    .anyMatch(l -> l.getId().equals(rule.getLabel().getId()));

                                if (!alreadyAssigned) {
                                    paymentLabelService.assignLabelToPayment(payment, rule.getLabel());
                                    totalLabelsApplied++;
                                    labelsForThisPayment++;
                                }
                            }
                        } catch (Exception e) {
                            // Skip invalid regex patterns
                            continue;
                        }
                    }
                }

                paymentLabelCounts.put(payment.getId(), labelsForThisPayment);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalPaymentsProcessed", paymentsToProcess.size(),
                "totalLabelsApplied", totalLabelsApplied,
                "paymentLabelCounts", paymentLabelCounts
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get payment field value based on field name
     */
    private String getPaymentFieldValue(Payment payment, String fieldName) {
        if (fieldName == null) {
            fieldName = "counterpartyName";
        }

        return switch (fieldName) {
            case "counterpartyName" -> payment.getCounterpartyName();
            case "reference" -> payment.getReference();
            case "transactionType" -> payment.getTransactionType();
            case "counterpartyBank" -> payment.getCounterpartyBank();
            case "counterpartyAccount" -> payment.getCounterpartyAccount();
            case "receiverInfo" -> payment.getReceiverInfo();
            case "additionalInfo" -> payment.getAdditionalInfo();
            default -> payment.getCounterpartyName();
        };
    }

    /**
     * Build rule DTO for response
     */
    private Map<String, Object> buildRuleDto(LabelingRule rule) {
        return Map.of(
            "id", rule.getId(),
            "name", rule.getName(),
            "regexPattern", rule.getRegexPattern(),
            "label", Map.of(
                "id", rule.getLabel().getId(),
                "name", rule.getLabel().getName()
            ),
            "isActive", rule.getIsActive(),
            "description", rule.getDescription() != null ? rule.getDescription() : "",
            "matchingField", rule.getMatchingField() != null ? rule.getMatchingField() : "counterpartyName",
            "createdAt", rule.getCreatedAt(),
            "updatedAt", rule.getUpdatedAt()
        );
    }

    /**
     * Extract payment IDs from request body
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractPaymentIds(Map<String, Object> request) {
        if (request == null || !request.containsKey("paymentIds")) {
            return null;
        }
        Object paymentIds = request.get("paymentIds");
        if (paymentIds instanceof List) {
            return ((List<?>) paymentIds).stream()
                .map(id -> ((Number) id).longValue())
                .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * Extract rule IDs from request body
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractRuleIds(Map<String, Object> request) {
        if (request == null || !request.containsKey("ruleIds")) {
            return null;
        }
        Object ruleIds = request.get("ruleIds");
        if (ruleIds instanceof List) {
            return ((List<?>) ruleIds).stream()
                .map(id -> ((Number) id).longValue())
                .collect(Collectors.toList());
        }
        return null;
    }
}

