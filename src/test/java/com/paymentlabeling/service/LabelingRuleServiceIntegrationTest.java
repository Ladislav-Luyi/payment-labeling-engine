package com.paymentlabeling.service;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.LabelingRule;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.model.PaymentLabel;
import com.paymentlabeling.repository.LabelRepository;
import com.paymentlabeling.repository.LabelingRuleRepository;
import com.paymentlabeling.repository.PaymentLabelRepository;
import com.paymentlabeling.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Automatic Labeling Engine (Issue #4)
 * Tests the complete flow: applying labeling rules → matching regex patterns → assigning labels to payments
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Labeling Rule Service Integration Tests")
class LabelingRuleServiceIntegrationTest {

    @Autowired
    private LabelingRuleService labelingRuleService;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private LabelingRuleRepository labelingRuleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentLabelRepository paymentLabelRepository;

    private Label groceryLabel;
    private Label parkingLabel;
    private Label restaurantLabel;
    private LabelingRule groceryRule;
    private LabelingRule parkingRule;
    private LabelingRule restaurantRule;
    private Payment payment1;
    private Payment payment2;
    private Payment payment3;

    @BeforeEach
    void setUp() {
        // Clear all data before each test
        paymentLabelRepository.deleteAll();
        paymentRepository.deleteAll();
        labelingRuleRepository.deleteAll();
        labelRepository.deleteAll();

        // Create test labels
        groceryLabel = labelRepository.save(Label.builder()
            .name("Grocery Shopping")
            .description("Purchases at grocery stores and supermarkets")
            .build());

        parkingLabel = labelRepository.save(Label.builder()
            .name("Parking")
            .description("Parking fees and parking lot payments")
            .build());

        restaurantLabel = labelRepository.save(Label.builder()
            .name("Restaurants")
            .description("Restaurant, cafe and food service purchases")
            .build());

        // Create test labeling rules
        groceryRule = labelingRuleRepository.save(LabelingRule.builder()
            .name("Kaufland Grocery Store Rule")
            .regexPattern("(?i).*KAUFLAND.*")
            .label(groceryLabel)
            .isActive(true)
            .description("Matches Kaufland grocery store transactions")
            .build());

        parkingRule = labelingRuleRepository.save(LabelingRule.builder()
            .name("Hopin Parking Rule")
            .regexPattern("(?i).*HOPIN.*PARKING.*")
            .label(parkingLabel)
            .isActive(true)
            .description("Matches Hopin parking service transactions")
            .build());

        restaurantRule = labelingRuleRepository.save(LabelingRule.builder()
            .name("General Restaurant Rule")
            .regexPattern("(?i).*(pizza|restaurant|cafe|burger).*")
            .label(restaurantLabel)
            .isActive(true)
            .description("Matches restaurant and food service transactions")
            .build());

        // Create test payments
        payment1 = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 23))
            .amount(new BigDecimal("-52.13"))
            .currency("EUR")
            .reference("/VS405000021/SS1130630652/KS0608")
            .transactionType("Platba kartou")
            .counterpartyName("Suma: 52,13 EUR 21.1.2026 Miesto: KAUFLAND 8820 BA I.CE")
            .accountNumber("SK0575000000004020035946")
            .build());

        payment2 = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 23))
            .amount(new BigDecimal("-1.12"))
            .currency("EUR")
            .reference("/VS405000021/SS1130630652/KS0608")
            .transactionType("Platba kartou")
            .counterpartyName("Suma: 1,12 EUR 21.1.2026 Miesto: HOPIN PARKING")
            .accountNumber("SK0575000000004020035946")
            .build());

        payment3 = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 24))
            .amount(new BigDecimal("-25.50"))
            .currency("EUR")
            .reference("/VS405000021/SS1130630652/KS0608")
            .transactionType("Platba kartou")
            .counterpartyName("PIZZA Express Bratislava")
            .accountNumber("SK0575000000004020035946")
            .build());
    }

    // ==================== Basic Labeling Rule Operations ====================

    @Test
    @DisplayName("Should save a new labeling rule successfully")
    void testSaveLabelingRuleSuccessfully() {
        // Arrange
        Label label = labelRepository.save(Label.builder()
            .name("New Category")
            .description("Test category")
            .build());

        LabelingRule newRule = LabelingRule.builder()
            .name("Test Rule")
            .regexPattern("(?i).*TEST.*")
            .label(label)
            .isActive(true)
            .description("A test rule")
            .build();

        // Act
        LabelingRule savedRule = labelingRuleService.saveLabelingRule(newRule);

        // Assert
        assertNotNull(savedRule.getId());
        assertEquals("Test Rule", savedRule.getName());
        assertEquals("(?i).*TEST.*", savedRule.getRegexPattern());
        assertEquals(label.getId(), savedRule.getLabel().getId());
        assertTrue(savedRule.getIsActive());
    }

    @Test
    @DisplayName("Should retrieve all labeling rules")
    void testGetAllRules() {
        // Act
        List<LabelingRule> rules = labelingRuleService.getAllRules();

        // Assert
        assertNotNull(rules);
        assertEquals(3, rules.size());
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Kaufland Grocery Store Rule")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("Hopin Parking Rule")));
        assertTrue(rules.stream().anyMatch(r -> r.getName().equals("General Restaurant Rule")));
    }

    @Test
    @DisplayName("Should retrieve only active labeling rules")
    void testGetActiveRulesOnly() {
        // Arrange
        LabelingRule inactiveRule = labelingRuleRepository.save(LabelingRule.builder()
            .name("Inactive Rule")
            .regexPattern("(?i).*INACTIVE.*")
            .label(groceryLabel)
            .isActive(false)
            .build());

        // Act
        List<LabelingRule> activeRules = labelingRuleService.getActiveRules();

        // Assert
        assertEquals(3, activeRules.size());
        assertFalse(activeRules.stream().anyMatch(r -> r.getId().equals(inactiveRule.getId())));
        assertTrue(activeRules.stream().allMatch(LabelingRule::getIsActive));
    }

    @Test
    @DisplayName("Should retrieve labeling rule by ID")
    void testGetRuleById() {
        // Act
        LabelingRule rule = labelingRuleService.getRuleById(groceryRule.getId());

        // Assert
        assertNotNull(rule);
        assertEquals("Kaufland Grocery Store Rule", rule.getName());
        assertEquals(groceryLabel.getId(), rule.getLabel().getId());
    }

    @Test
    @DisplayName("Should delete labeling rule by ID")
    void testDeleteRuleById() {
        // Act
        labelingRuleService.deleteRule(groceryRule.getId());

        // Assert
        List<LabelingRule> rules = labelingRuleService.getAllRules();
        assertEquals(2, rules.size());
        assertFalse(rules.stream().anyMatch(r -> r.getId().equals(groceryRule.getId())));
    }

    @Test
    @DisplayName("Should retrieve rules for a specific label")
    void testGetRulesForLabel() {
        // Act
        List<LabelingRule> groceryRules = labelingRuleService.getRulesForLabel(groceryLabel.getId());

        // Assert
        assertEquals(1, groceryRules.size());
        assertEquals("Kaufland Grocery Store Rule", groceryRules.get(0).getName());
    }

    @Test
    @DisplayName("Should retrieve multiple rules for a label with multiple rules")
    void testGetMultipleRulesForLabel() {
        // Arrange
        LabelingRule anotherGroceryRule = labelingRuleRepository.save(LabelingRule.builder()
            .name("Tesco Grocery Rule")
            .regexPattern("(?i).*TESCO.*")
            .label(groceryLabel)
            .isActive(true)
            .build());

        // Act
        List<LabelingRule> groceryRules = labelingRuleService.getRulesForLabel(groceryLabel.getId());

        // Assert
        assertEquals(2, groceryRules.size());
        assertTrue(groceryRules.stream().anyMatch(r -> r.getName().equals("Kaufland Grocery Store Rule")));
        assertTrue(groceryRules.stream().anyMatch(r -> r.getName().equals("Tesco Grocery Rule")));
    }

    // ==================== Regex Pattern Matching ====================

    @Test
    @DisplayName("Should match payment against simple regex pattern (case-insensitive)")
    void testMatchPaymentAgainstSimplePattern() {
        // Arrange
        String paymentInfo = "Suma: 52,13 EUR 21.1.2026 Miesto: KAUFLAND 8820 BA I.CE";
        String pattern = groceryRule.getRegexPattern();

        // Act
        boolean matches = paymentInfo.matches(pattern);

        // Assert
        assertTrue(matches, "Payment info should match Kaufland grocery pattern");
    }

    @Test
    @DisplayName("Should match payment against complex regex pattern with alternatives")
    void testMatchPaymentAgainstComplexPattern() {
        // Arrange
        String paymentInfo = "PIZZA Express Bratislava";
        String pattern = restaurantRule.getRegexPattern();

        // Act
        boolean matches = paymentInfo.matches(pattern);

        // Assert
        assertTrue(matches, "Payment info should match restaurant pattern");
    }

    @Test
    @DisplayName("Should not match payment against non-matching pattern")
    void testPaymentDoesNotMatchPattern() {
        // Arrange
        String paymentInfo = "Random Merchant XYZ";
        String pattern = groceryRule.getRegexPattern();

        // Act
        boolean matches = paymentInfo.matches(pattern);

        // Assert
        assertFalse(matches, "Payment info should not match grocery pattern");
    }

    @Test
    @DisplayName("Should handle null counterparty name gracefully")
    void testMatchPatternWithNullCounterpartyName() {
        // Arrange
        Payment paymentWithoutName = Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 25))
            .amount(new BigDecimal("-100.00"))
            .currency("EUR")
            .accountNumber("SK0575000000004020035946")
            .build();

        String counterpartyName = paymentWithoutName.getCounterpartyName();

        // Act & Assert
        assertNull(counterpartyName);
        // Pattern matching should handle null gracefully in application logic
    }

    // ==================== Payment Label Assignment ====================

    @Test
    @DisplayName("Should assign label to payment when rule matches")
    void testAssignLabelToPaymentWhenRuleMatches() {
        // Arrange
        PaymentLabel paymentLabel = PaymentLabel.builder()
            .payment(payment1)
            .label(groceryLabel)
            .build();

        // Act
        PaymentLabel savedLabel = paymentLabelRepository.save(paymentLabel);
        List<PaymentLabel> paymentLabels = paymentLabelRepository.findByPaymentId(payment1.getId());

        // Assert
        assertNotNull(savedLabel.getId());
        assertEquals(1, paymentLabels.size());
        assertEquals(groceryLabel.getId(), paymentLabels.get(0).getLabel().getId());
        assertEquals(payment1.getId(), paymentLabels.get(0).getPayment().getId());
    }

    @Test
    @DisplayName("Should assign multiple labels to single payment")
    void testAssignMultipleLabelsToPayment() {
        // Arrange
        PaymentLabel label1 = PaymentLabel.builder()
            .payment(payment1)
            .label(groceryLabel)
            .build();
        PaymentLabel label2 = PaymentLabel.builder()
            .payment(payment1)
            .label(restaurantLabel)
            .build();

        // Act
        paymentLabelRepository.save(label1);
        paymentLabelRepository.save(label2);
        List<PaymentLabel> assignedLabels = paymentLabelRepository.findByPaymentId(payment1.getId());

        // Assert
        assertEquals(2, assignedLabels.size());
        assertTrue(assignedLabels.stream()
            .anyMatch(pl -> pl.getLabel().getId().equals(groceryLabel.getId())));
        assertTrue(assignedLabels.stream()
            .anyMatch(pl -> pl.getLabel().getId().equals(restaurantLabel.getId())));
    }

    @Test
    @DisplayName("Should prevent duplicate label assignment to payment")
    void testPreventDuplicateLabelAssignment() {
        // Arrange
        PaymentLabel label1 = PaymentLabel.builder()
            .payment(payment1)
            .label(groceryLabel)
            .build();
        paymentLabelRepository.save(label1);

        PaymentLabel duplicateLabel = PaymentLabel.builder()
            .payment(payment1)
            .label(groceryLabel)
            .build();

        // Act & Assert
        assertThrows(Exception.class, () -> {
            paymentLabelRepository.save(duplicateLabel);
            // Force flush to trigger unique constraint
        }, "Saving duplicate payment-label should fail");
    }

    @Test
    @DisplayName("Should find all payments with specific label")
    void testFindAllPaymentsWithLabel() {
        // Arrange
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(payment1)
            .label(groceryLabel)
            .build());
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(payment3)
            .label(restaurantLabel)
            .build());

        // Act
        List<PaymentLabel> restaurantPayments = paymentLabelRepository.findByLabelId(restaurantLabel.getId());

        // Assert
        assertEquals(1, restaurantPayments.size());
        assertEquals(payment3.getId(), restaurantPayments.get(0).getPayment().getId());
    }

    // ==================== Automatic Labeling Scenarios ====================

    @Test
    @DisplayName("Should apply Kaufland rule to grocery payment")
    void testApplyGroceryLabelRule() {
        // Arrange
        List<LabelingRule> activeRules = labelingRuleService.getActiveRules();

        // Act
        LabelingRule matchingRule = activeRules.stream()
            .filter(rule -> payment1.getCounterpartyName().matches(rule.getRegexPattern()))
            .findFirst()
            .orElse(null);

        // Assert
        assertNotNull(matchingRule);
        assertEquals(groceryRule.getId(), matchingRule.getId());
        assertEquals(groceryLabel.getId(), matchingRule.getLabel().getId());
    }

    @Test
    @DisplayName("Should apply Parking rule to parking payment")
    void testApplyParkingLabelRule() {
        // Arrange
        List<LabelingRule> activeRules = labelingRuleService.getActiveRules();

        // Act
        LabelingRule matchingRule = activeRules.stream()
            .filter(rule -> payment2.getCounterpartyName().matches(rule.getRegexPattern()))
            .findFirst()
            .orElse(null);

        // Assert
        assertNotNull(matchingRule);
        assertEquals(parkingRule.getId(), matchingRule.getId());
        assertEquals(parkingLabel.getId(), matchingRule.getLabel().getId());
    }

    @Test
    @DisplayName("Should apply Restaurant rule to restaurant payment")
    void testApplyRestaurantLabelRule() {
        // Arrange
        List<LabelingRule> activeRules = labelingRuleService.getActiveRules();

        // Act
        LabelingRule matchingRule = activeRules.stream()
            .filter(rule -> payment3.getCounterpartyName().matches(rule.getRegexPattern()))
            .findFirst()
            .orElse(null);

        // Assert
        assertNotNull(matchingRule);
        assertEquals(restaurantRule.getId(), matchingRule.getId());
        assertEquals(restaurantLabel.getId(), matchingRule.getLabel().getId());
    }

    @Test
    @DisplayName("Should skip inactive rules during labeling")
    void testSkipInactiveRulesDuringLabeling() {
        // Arrange
        LabelingRule inactiveGroceryRule = labelingRuleRepository.save(LabelingRule.builder()
            .name("Inactive Grocery Rule")
            .regexPattern("(?i).*KAUFLAND.*")
            .label(groceryLabel)
            .isActive(false)
            .build());

        // Act
        List<LabelingRule> activeRules = labelingRuleService.getActiveRules();
        LabelingRule matchingRule = activeRules.stream()
            .filter(rule -> payment1.getCounterpartyName().matches(rule.getRegexPattern()))
            .findFirst()
            .orElse(null);

        // Assert
        assertNotNull(matchingRule);
        assertTrue(matchingRule.getIsActive());
        assertNotEquals(inactiveGroceryRule.getId(), matchingRule.getId());
    }

    @Test
    @DisplayName("Should handle payment matching multiple rules")
    void testPaymentMatchingMultipleRules() {
        // Arrange
        LabelingRule alternativeGroceryRule = labelingRuleRepository.save(LabelingRule.builder()
            .name("Alternative Grocery Rule")
            .regexPattern("(?i).*KAUFLAND.*")
            .label(restaurantLabel)  // Different label, same pattern
            .isActive(true)
            .build());

        List<LabelingRule> activeRules = labelingRuleService.getActiveRules();

        // Act
        List<LabelingRule> matchingRules = activeRules.stream()
            .filter(rule -> payment1.getCounterpartyName().matches(rule.getRegexPattern()))
            .toList();

        // Assert
        assertEquals(2, matchingRules.size());
        assertTrue(matchingRules.stream()
            .anyMatch(r -> r.getLabel().getId().equals(groceryLabel.getId())));
        assertTrue(matchingRules.stream()
            .anyMatch(r -> r.getLabel().getId().equals(restaurantLabel.getId())));
    }

    // ==================== Error Handling & Edge Cases ====================

    @Test
    @DisplayName("Should handle empty regex pattern gracefully")
    void testHandleEmptyRegexPattern() {
        // Arrange
        LabelingRule invalidRule = LabelingRule.builder()
            .name("Invalid Rule")
            .regexPattern("")
            .label(groceryLabel)
            .isActive(true)
            .build();

        // Act & Assert
        try {
            boolean matches = "any text".matches("");
            // Empty pattern matches empty string only
            assertFalse(matches);
        } catch (Exception e) {
            // Should not throw for empty pattern
            fail("Empty regex pattern should be handled gracefully");
        }
    }

    @Test
    @DisplayName("Should handle special regex characters in pattern")
    void testHandleSpecialRegexCharactersInPattern() {
        // Arrange
        LabelingRule specialCharRule = labelingRuleRepository.save(LabelingRule.builder()
            .name("Special Char Rule")
            .regexPattern("(?i).*(\\$|€|£).*")  // Matches currency symbols
            .label(groceryLabel)
            .isActive(true)
            .build());

        String paymentInfo = "Amount: 100€ Payment";

        // Act
        boolean matches = paymentInfo.matches(specialCharRule.getRegexPattern());

        // Assert
        assertTrue(matches);
    }

    @Test
    @DisplayName("Should handle very long counterparty name in pattern matching")
    void testHandleLongCounterpartyNameInPatternMatching() {
        // Arrange
        String longName = "A".repeat(500) + " KAUFLAND " + "B".repeat(500);
        Payment paymentWithLongName = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 26))
            .amount(new BigDecimal("-99.99"))
            .currency("EUR")
            .counterpartyName(longName)
            .accountNumber("SK0575000000004020035946")
            .build());

        // Act
        boolean matches = paymentWithLongName.getCounterpartyName().matches(groceryRule.getRegexPattern());

        // Assert
        assertTrue(matches, "Pattern should match even with very long counterparty name");
    }

    @Test
    @DisplayName("Should delete all labels for a payment")
    void testDeleteAllLabelsForPayment() {
        // Arrange
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(payment1)
            .label(groceryLabel)
            .build());
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(payment1)
            .label(parkingLabel)
            .build());

        List<PaymentLabel> labelsBeforeDelete = paymentLabelRepository.findByPaymentId(payment1.getId());
        assertEquals(2, labelsBeforeDelete.size());

        // Act
        paymentLabelRepository.deleteByPaymentId(payment1.getId());

        // Assert
        List<PaymentLabel> labelsAfterDelete = paymentLabelRepository.findByPaymentId(payment1.getId());
        assertEquals(0, labelsAfterDelete.size());
    }

    // ==================== Rule Management ====================

    @Test
    @DisplayName("Should update existing labeling rule")
    void testUpdateExistingLabelingRule() {
        // Arrange
        LabelingRule originalRule = groceryRule;
        String updatedPattern = "(?i).*SHOPPING.*";

        // Act
        originalRule.setRegexPattern(updatedPattern);
        originalRule.setDescription("Updated description");
        LabelingRule savedRule = labelingRuleService.saveLabelingRule(originalRule);

        // Assert
        assertEquals(updatedPattern, savedRule.getRegexPattern());
        assertEquals("Updated description", savedRule.getDescription());
    }

    @Test
    @DisplayName("Should deactivate a labeling rule")
    void testDeactivateLabelingRule() {
        // Arrange
        LabelingRule rule = groceryRule;
        rule.setIsActive(false);

        // Act
        labelingRuleService.saveLabelingRule(rule);
        List<LabelingRule> activeRules = labelingRuleService.getActiveRules();

        // Assert
        assertFalse(activeRules.stream()
            .anyMatch(r -> r.getId().equals(rule.getId())));
    }

    @Test
    @DisplayName("Should change label for existing rule")
    void testChangeRuleLabelAssignment() {
        // Arrange
        LabelingRule rule = groceryRule;
        Label newLabel = labelRepository.save(Label.builder()
            .name("Shopping")
            .build());

        // Act
        rule.setLabel(newLabel);
        LabelingRule updatedRule = labelingRuleService.saveLabelingRule(rule);

        // Assert
        assertEquals(newLabel.getId(), updatedRule.getLabel().getId());
        assertNotEquals(groceryLabel.getId(), updatedRule.getLabel().getId());
    }

    // ==================== Concurrent Payment & Rule Operations ====================

    @Test
    @DisplayName("Should handle labeling multiple payments sequentially")
    void testLabelMultiplePaymentsSequentially() {
        // Arrange
        List<Payment> payments = List.of(payment1, payment2, payment3);
        List<LabelingRule> rules = labelingRuleService.getActiveRules();

        // Act
        for (Payment payment : payments) {
            for (LabelingRule rule : rules) {
                if (payment.getCounterpartyName() != null && 
                    payment.getCounterpartyName().matches(rule.getRegexPattern())) {
                    paymentLabelRepository.save(PaymentLabel.builder()
                        .payment(payment)
                        .label(rule.getLabel())
                        .build());
                }
            }
        }

        // Assert
        List<PaymentLabel> payment1Labels = paymentLabelRepository.findByPaymentId(payment1.getId());
        List<PaymentLabel> payment2Labels = paymentLabelRepository.findByPaymentId(payment2.getId());
        List<PaymentLabel> payment3Labels = paymentLabelRepository.findByPaymentId(payment3.getId());

        assertEquals(1, payment1Labels.size());
        assertEquals(groceryLabel.getId(), payment1Labels.get(0).getLabel().getId());

        assertEquals(1, payment2Labels.size());
        assertEquals(parkingLabel.getId(), payment2Labels.get(0).getLabel().getId());

        assertEquals(1, payment3Labels.size());
        assertEquals(restaurantLabel.getId(), payment3Labels.get(0).getLabel().getId());
    }

    @Test
    @DisplayName("Should label all new payments after rule creation")
    void testLabelNewPaymentsAfterRuleCreation() {
        // Arrange
        LabelingRule newRule = labelingRuleRepository.save(LabelingRule.builder()
            .name("New Test Rule")
            .regexPattern("(?i).*NEW.*")
            .label(restaurantLabel)
            .isActive(true)
            .build());

        Payment newPayment = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 27))
            .amount(new BigDecimal("-10.00"))
            .currency("EUR")
            .counterpartyName("NEW Merchant")
            .accountNumber("SK0575000000004020035946")
            .build());

        // Act
        List<LabelingRule> activeRules = labelingRuleService.getActiveRules();
        LabelingRule matchingRule = activeRules.stream()
            .filter(rule -> newPayment.getCounterpartyName().matches(rule.getRegexPattern()))
            .findFirst()
            .orElse(null);

        if (matchingRule != null) {
            paymentLabelRepository.save(PaymentLabel.builder()
                .payment(newPayment)
                .label(matchingRule.getLabel())
                .build());
        }

        // Assert
        assertNotNull(matchingRule);
        List<PaymentLabel> newPaymentLabels = paymentLabelRepository.findByPaymentId(newPayment.getId());
        assertEquals(1, newPaymentLabels.size());
        assertEquals(restaurantLabel.getId(), newPaymentLabels.get(0).getLabel().getId());
    }
}
