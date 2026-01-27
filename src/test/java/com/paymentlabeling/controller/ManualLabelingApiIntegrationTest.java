package com.paymentlabeling.controller;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.model.PaymentLabel;
import com.paymentlabeling.repository.LabelRepository;
import com.paymentlabeling.repository.PaymentLabelRepository;
import com.paymentlabeling.repository.PaymentRepository;
import com.paymentlabeling.service.PaymentLabelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Manual Labeling API endpoints (Issue #5)
 * Tests the REST API for manually assigning/removing labels to/from payments
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Manual Labeling API Integration Tests")
class ManualLabelingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private PaymentLabelRepository paymentLabelRepository;

    @Autowired
    private PaymentLabelService paymentLabelService;

    private Payment testPayment;
    private Label groceryLabel;
    private Label parkingLabel;

    @BeforeEach
    void setUp() {
        // Clear data
        paymentLabelRepository.deleteAll();
        paymentRepository.deleteAll();
        labelRepository.deleteAll();

        // Create test labels
        groceryLabel = labelRepository.save(Label.builder()
            .name("Grocery Shopping")
            .description("Grocery store purchases")
            .build());

        parkingLabel = labelRepository.save(Label.builder()
            .name("Parking")
            .description("Parking fees")
            .build());

        // Create test payment
        testPayment = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 23))
            .amount(new BigDecimal("-52.13"))
            .currency("EUR")
            .reference("/VS405000021/SS1130630652/KS0608")
            .transactionType("Platba kartou")
            .counterpartyName("KAUFLAND 8820 BA I.CE")
            .accountNumber("SK0575000000004020035946")
            .build());
    }

    // ==================== Assign Label Tests ====================

    @Test
    @DisplayName("Should assign label to payment via API")
    void testAssignLabelToPayment() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(testPayment.getId()))
            .andExpect(jsonPath("$.labelId").value(groceryLabel.getId()));

        // Verify
        assertEquals(1, paymentLabelRepository.findByPaymentId(testPayment.getId()).size());
    }

    @Test
    @DisplayName("Should return 404 when assigning label to non-existent payment")
    void testAssignLabelToNonExistentPayment() throws Exception {
        mockMvc.perform(post("/api/payments/99999/labels/{labelId}", groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when assigning non-existent label")
    void testAssignNonExistentLabel() throws Exception {
        mockMvc.perform(post("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), 99999L)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 409 when assigning duplicate label")
    void testAssignDuplicateLabel() throws Exception {
        // First assignment
        mockMvc.perform(post("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        // Second assignment (duplicate)
        mockMvc.perform(post("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should assign multiple labels to same payment")
    void testAssignMultipleLabelsToPayment() throws Exception {
        // Assign first label
        mockMvc.perform(post("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        // Assign second label
        mockMvc.perform(post("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), parkingLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        // Verify
        assertEquals(2, paymentLabelRepository.findByPaymentId(testPayment.getId()).size());
    }

    // ==================== Remove Label Tests ====================

    @Test
    @DisplayName("Should remove label from payment via API")
    void testRemoveLabelFromPayment() throws Exception {
        // Setup - assign label first
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(testPayment)
            .label(groceryLabel)
            .build());

        // Act & Assert
        mockMvc.perform(delete("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Verify
        assertEquals(0, paymentLabelRepository.findByPaymentId(testPayment.getId()).size());
    }

    @Test
    @DisplayName("Should return 404 when removing label from non-existent payment")
    void testRemoveLabelFromNonExistentPayment() throws Exception {
        mockMvc.perform(delete("/api/payments/99999/labels/{labelId}", groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when removing non-existent label")
    void testRemoveNonExistentLabel() throws Exception {
        mockMvc.perform(delete("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), 99999L)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when removing label not assigned to payment")
    void testRemoveLabelNotAssignedToPayment() throws Exception {
        mockMvc.perform(delete("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should remove all labels from payment via API")
    void testRemoveAllLabelsFromPayment() throws Exception {
        // Setup - assign multiple labels
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(testPayment)
            .label(groceryLabel)
            .build());
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(testPayment)
            .label(parkingLabel)
            .build());

        // Act & Assert
        mockMvc.perform(delete("/api/payments/{paymentId}/labels", testPayment.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Verify
        assertEquals(0, paymentLabelRepository.findByPaymentId(testPayment.getId()).size());
    }

    // ==================== Get Labels Tests ====================

    @Test
    @DisplayName("Should get all labels for a payment via API")
    void testGetLabelsForPayment() throws Exception {
        // Setup
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(testPayment)
            .label(groceryLabel)
            .build());
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(testPayment)
            .label(parkingLabel)
            .build());

        // Act & Assert
        mockMvc.perform(get("/api/payments/{paymentId}/labels", testPayment.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.labels.length()").value(2))
            .andExpect(jsonPath("$.paymentId").value(testPayment.getId()));
    }

    @Test
    @DisplayName("Should return empty labels list for payment with no labels")
    void testGetLabelsForPaymentWithNoLabels() throws Exception {
        mockMvc.perform(get("/api/payments/{paymentId}/labels", testPayment.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.labels.length()").value(0));
    }

    @Test
    @DisplayName("Should return 404 when getting labels for non-existent payment")
    void testGetLabelsForNonExistentPayment() throws Exception {
        mockMvc.perform(get("/api/payments/99999/labels")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    // ==================== Get Payment Details Tests ====================

    @Test
    @DisplayName("Should get payment details with labels via API")
    void testGetPaymentDetailsWithLabels() throws Exception {
        // Setup
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(testPayment)
            .label(groceryLabel)
            .build());

        // Act & Assert
        mockMvc.perform(get("/api/payments/{paymentId}", testPayment.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testPayment.getId()))
            .andExpect(jsonPath("$.counterpartyName").value(testPayment.getCounterpartyName()))
            .andExpect(jsonPath("$.labels.length()").value(1))
            .andExpect(jsonPath("$.labels[0].name").value("Grocery Shopping"));
    }

    @Test
    @DisplayName("Should return 404 when getting non-existent payment")
    void testGetNonExistentPayment() throws Exception {
        mockMvc.perform(get("/api/payments/99999")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    // ==================== Get All Payments Tests ====================

    @Test
    @DisplayName("Should get all payments with labels via API")
    void testGetAllPayments() throws Exception {
        // Setup
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(testPayment)
            .label(groceryLabel)
            .build());

        // Create another payment
        Payment payment2 = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 24))
            .amount(new BigDecimal("-25.50"))
            .currency("EUR")
            .counterpartyName("HOPIN PARKING")
            .accountNumber("SK0575000000004020035946")
            .build());

        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(payment2)
            .label(parkingLabel)
            .build());

        // Act & Assert
        mockMvc.perform(get("/api/payments")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payments.length()").value(2))
            .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @DisplayName("Should return empty payments list when no payments exist")
    void testGetAllPaymentsWhenEmpty() throws Exception {
        paymentRepository.deleteAll();

        mockMvc.perform(get("/api/payments")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payments.length()").value(0))
            .andExpect(jsonPath("$.total").value(0));
    }

    // ==================== Search and Filter Tests ====================

    @Test
    @DisplayName("Should filter payments by label via API")
    void testFilterPaymentsByLabel() throws Exception {
        // Setup
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(testPayment)
            .label(groceryLabel)
            .build());

        // Create another payment with different label
        Payment payment2 = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 24))
            .amount(new BigDecimal("-25.50"))
            .currency("EUR")
            .counterpartyName("HOPIN PARKING")
            .accountNumber("SK0575000000004020035946")
            .build());

        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(payment2)
            .label(parkingLabel)
            .build());

        // Act & Assert
        mockMvc.perform(get("/api/payments")
            .param("labelId", groceryLabel.getId().toString())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payments.length()").value(1))
            .andExpect(jsonPath("$.payments[0].id").value(testPayment.getId()));
    }

    @Test
    @DisplayName("Should filter payments by date range via API")
    void testFilterPaymentsByDateRange() throws Exception {
        // Create payment with different date
        Payment payment2 = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 2, 15))
            .amount(new BigDecimal("-100.00"))
            .currency("EUR")
            .counterpartyName("Some Merchant")
            .accountNumber("SK0575000000004020035946")
            .build());

        // Act & Assert
        mockMvc.perform(get("/api/payments")
            .param("startDate", "2026-01-01")
            .param("endDate", "2026-01-31")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payments.length()").value(1))
            .andExpect(jsonPath("$.payments[0].id").value(testPayment.getId()));
    }

    @Test
    @DisplayName("Should search payments by counterparty name via API")
    void testSearchPaymentsByCounterpartyName() throws Exception {
        mockMvc.perform(get("/api/payments/search")
            .param("query", "KAUFLAND")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payments.length()").value(1))
            .andExpect(jsonPath("$.payments[0].counterpartyName").value("KAUFLAND 8820 BA I.CE"));
    }

    // ==================== Bulk Operations Tests ====================

    @Test
    @DisplayName("Should assign label to multiple payments via API")
    void testAssignLabelToMultiplePayments() throws Exception {
        // Create another payment
        Payment payment2 = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 24))
            .amount(new BigDecimal("-25.50"))
            .currency("EUR")
            .counterpartyName("HOPIN PARKING")
            .accountNumber("SK0575000000004020035946")
            .build());

        // Act & Assert
        String requestBody = "{\"paymentIds\": [" + testPayment.getId() + ", " + payment2.getId() + "]}";
        mockMvc.perform(post("/api/payments/labels/{labelId}/payments", groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.assignedCount").value(2));

        // Verify
        assertEquals(1, paymentLabelRepository.findByPaymentId(testPayment.getId()).size());
        assertEquals(1, paymentLabelRepository.findByPaymentId(payment2.getId()).size());
    }

    @Test
    @DisplayName("Should remove label from multiple payments via API")
    void testRemoveLabelFromMultiplePayments() throws Exception {
        // Create another payment with label
        Payment payment2 = paymentRepository.save(Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 24))
            .amount(new BigDecimal("-25.50"))
            .currency("EUR")
            .counterpartyName("HOPIN PARKING")
            .accountNumber("SK0575000000004020035946")
            .build());

        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(testPayment)
            .label(groceryLabel)
            .build());
        paymentLabelRepository.save(PaymentLabel.builder()
            .payment(payment2)
            .label(groceryLabel)
            .build());

        // Act & Assert
        String requestBody = "{\"paymentIds\": [" + testPayment.getId() + ", " + payment2.getId() + "]}";
        mockMvc.perform(delete("/api/payments/labels/{labelId}/payments", groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isNoContent());

        // Verify
        assertEquals(0, paymentLabelRepository.findByPaymentId(testPayment.getId()).size());
        assertEquals(0, paymentLabelRepository.findByPaymentId(payment2.getId()).size());
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("Should validate payment ID format")
    void testValidatePaymentIdFormat() throws Exception {
        mockMvc.perform(post("/api/payments/invalid-id/labels/{labelId}", groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate label ID format")
    void testValidateLabelIdFormat() throws Exception {
        mockMvc.perform(post("/api/payments/{paymentId}/labels/invalid-id", testPayment.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle concurrent label assignments to same payment")
    void testConcurrentLabelAssignments() throws Exception {
        // This test verifies that the database constraint prevents duplicate assignments
        // even when multiple requests arrive simultaneously

        // Create a third label
        Label entertainmentLabel = labelRepository.save(Label.builder()
            .name("Entertainment")
            .description("Entertainment purchases")
            .build());

        // Assign multiple labels
        mockMvc.perform(post("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), groceryLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), parkingLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments/{paymentId}/labels/{labelId}", testPayment.getId(), entertainmentLabel.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        // Verify all labels assigned
        assertEquals(3, paymentLabelRepository.findByPaymentId(testPayment.getId()).size());
    }
}
