package com.paymentlabeling.controller;

import com.paymentlabeling.model.Aggregate;
import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.model.PaymentLabel;
import com.paymentlabeling.repository.AggregateRepository;
import com.paymentlabeling.repository.LabelRepository;
import com.paymentlabeling.repository.PaymentLabelRepository;
import com.paymentlabeling.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Aggregate & Reporting API endpoints (Issue #6)
 * Tests REST API for retrieving, filtering, and reporting on aggregated payment data
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Aggregate API Integration Tests")
class AggregateApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AggregateRepository aggregateRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private PaymentLabelRepository paymentLabelRepository;

    private Label groceryLabel;
    private Label parkingLabel;
    private Label utilitiesLabel;
    private Payment payment1;
    private Payment payment2;
    private Payment payment3;

    @BeforeEach
    void setUp() {
        // Clear data
        paymentLabelRepository.deleteAll();
        aggregateRepository.deleteAll();
        paymentRepository.deleteAll();
        labelRepository.deleteAll();

        // Create test labels
        groceryLabel = new Label();
        groceryLabel.setName("Grocery Shopping");
        groceryLabel.setDescription("Grocery store purchases");
        groceryLabel = labelRepository.save(groceryLabel);

        parkingLabel = new Label();
        parkingLabel.setName("Parking");
        parkingLabel.setDescription("Parking fees");
        parkingLabel = labelRepository.save(parkingLabel);

        utilitiesLabel = new Label();
        utilitiesLabel.setName("Utilities");
        utilitiesLabel.setDescription("Utility bills");
        utilitiesLabel = labelRepository.save(utilitiesLabel);

        // Create test payments
        payment1 = new Payment();
        payment1.setPaymentDate(LocalDate.of(2026, 1, 5));
        payment1.setAmount(new BigDecimal("-52.13"));
        payment1.setCurrency("EUR");
        payment1.setReference("/VS405000021/SS1130630652/KS0608");
        payment1.setTransactionType("Platba kartou");
        payment1.setCounterpartyName("KAUFLAND 8820 BA I.CE");
        payment1.setAccountNumber("SK0575000000004020035946");
        payment1 = paymentRepository.save(payment1);

        payment2 = new Payment();
        payment2.setPaymentDate(LocalDate.of(2026, 1, 10));
        payment2.setAmount(new BigDecimal("-3.50"));
        payment2.setCurrency("EUR");
        payment2.setReference("/VS123456789/SS9876543210/KS0608");
        payment2.setTransactionType("Platba kartou");
        payment2.setCounterpartyName("PARKING LOT");
        payment2.setAccountNumber("SK0575000000004020035946");
        payment2 = paymentRepository.save(payment2);

        payment3 = new Payment();
        payment3.setPaymentDate(LocalDate.of(2026, 1, 15));
        payment3.setAmount(new BigDecimal("-89.45"));
        payment3.setCurrency("EUR");
        payment3.setReference("/VS555555555/SS7777777777/KS0608");
        payment3.setTransactionType("Platba kartou");
        payment3.setCounterpartyName("KAUFLAND 8820 BA I.CE");
        payment3.setAccountNumber("SK0575000000004020035946");
        payment3 = paymentRepository.save(payment3);

        // Assign labels to payments
        PaymentLabel pl1 = new PaymentLabel();
        pl1.setPayment(payment1);
        pl1.setLabel(groceryLabel);
        paymentLabelRepository.save(pl1);

        PaymentLabel pl2 = new PaymentLabel();
        pl2.setPayment(payment2);
        pl2.setLabel(parkingLabel);
        paymentLabelRepository.save(pl2);

        PaymentLabel pl3 = new PaymentLabel();
        pl3.setPayment(payment3);
        pl3.setLabel(groceryLabel);
        paymentLabelRepository.save(pl3);
    }

    @Nested
    @DisplayName("Aggregate Retrieval Endpoints")
    class AggregateRetrievalTests {

        private void setupAggregates() {
            Aggregate agg1 = new Aggregate();
            agg1.setLabel(groceryLabel);
            agg1.setYear(2026);
            agg1.setMonth(1);
            agg1.setTotalAmount(new BigDecimal("141.58"));
            agg1.setTransactionCount(2L);
            aggregateRepository.save(agg1);

            Aggregate agg2 = new Aggregate();
            agg2.setLabel(parkingLabel);
            agg2.setYear(2026);
            agg2.setMonth(1);
            agg2.setTotalAmount(new BigDecimal("3.50"));
            agg2.setTransactionCount(1L);
            aggregateRepository.save(agg2);

            Aggregate agg3 = new Aggregate();
            agg3.setLabel(groceryLabel);
            agg3.setYear(2026);
            agg3.setMonth(2);
            agg3.setTotalAmount(new BigDecimal("200.00"));
            agg3.setTransactionCount(3L);
            aggregateRepository.save(agg3);

            Aggregate agg4 = new Aggregate();
            agg4.setLabel(utilitiesLabel);
            agg4.setYear(2025);
            agg4.setMonth(12);
            agg4.setTotalAmount(new BigDecimal("120.00"));
            agg4.setTransactionCount(1L);
            aggregateRepository.save(agg4);
        }

        @Test
        @DisplayName("Should retrieve all aggregates")
        void testGetAllAggregates() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
        }

        @Test
        @DisplayName("Should retrieve aggregates filtered by year")
        void testGetAggregatesByYear() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("year", "2026")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].year", everyItem(equalTo(2026))));
        }

        @Test
        @DisplayName("Should retrieve aggregates filtered by year and month")
        void testGetAggregatesByYearAndMonth() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("year", "2026")
                .param("month", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].year", everyItem(equalTo(2026))))
                .andExpect(jsonPath("$[*].month", everyItem(equalTo(1))));
        }

        @Test
        @DisplayName("Should retrieve aggregates filtered by label")
        void testGetAggregatesByLabel() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("labelId", groceryLabel.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].label.id", everyItem(equalTo(groceryLabel.getId().intValue()))));
        }

        @Test
        @DisplayName("Should retrieve aggregate by ID")
        void testGetAggregateById() throws Exception {
            // Arrange
            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(groceryLabel);
            aggregate.setYear(2026);
            aggregate.setMonth(1);
            aggregate.setTotalAmount(new BigDecimal("141.58"));
            aggregate.setTransactionCount(2L);
            aggregate = aggregateRepository.save(aggregate);

            // Act & Assert
            mockMvc.perform(get("/api/aggregates/" + aggregate.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aggregate.getId()))
                .andExpect(jsonPath("$.totalAmount").value(141.58))
                .andExpect(jsonPath("$.transactionCount").value(2));
        }

        @Test
        @DisplayName("Should return 404 for non-existent aggregate")
        void testGetAggregateByIdNotFound() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/aggregates/9999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return empty list for year with no aggregates")
        void testGetAggregatesByYearEmpty() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("year", "2024")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Aggregate Reporting and Summary Endpoints")
    class AggregateReportingTests {

        private void setupAggregates() {
            Aggregate agg1 = new Aggregate();
            agg1.setLabel(groceryLabel);
            agg1.setYear(2026);
            agg1.setMonth(1);
            agg1.setTotalAmount(new BigDecimal("141.58"));
            agg1.setTransactionCount(2L);
            aggregateRepository.save(agg1);

            Aggregate agg2 = new Aggregate();
            agg2.setLabel(parkingLabel);
            agg2.setYear(2026);
            agg2.setMonth(1);
            agg2.setTotalAmount(new BigDecimal("3.50"));
            agg2.setTransactionCount(1L);
            aggregateRepository.save(agg2);

            Aggregate agg3 = new Aggregate();
            agg3.setLabel(groceryLabel);
            agg3.setYear(2026);
            agg3.setMonth(2);
            agg3.setTotalAmount(new BigDecimal("200.00"));
            agg3.setTransactionCount(3L);
            aggregateRepository.save(agg3);
        }

        @Test
        @DisplayName("Should retrieve monthly summary for a label")
        void testGetMonthlySummaryByLabel() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates/summary/monthly")
                .param("labelId", groceryLabel.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].label.id", everyItem(equalTo(groceryLabel.getId().intValue()))));
        }

        @Test
        @DisplayName("Should retrieve yearly summary for all labels")
        void testGetYearlySummary() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates/summary/yearly")
                .param("year", "2026")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Should retrieve label statistics for a month")
        void testGetLabelStatisticsForMonth() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates/stats")
                .param("year", "2026")
                .param("month", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("Aggregate Data Export")
    class AggregateExportTests {

        private void setupAggregates() {
            Aggregate agg1 = new Aggregate();
            agg1.setLabel(groceryLabel);
            agg1.setYear(2026);
            agg1.setMonth(1);
            agg1.setTotalAmount(new BigDecimal("141.58"));
            agg1.setTransactionCount(2L);
            aggregateRepository.save(agg1);

            Aggregate agg2 = new Aggregate();
            agg2.setLabel(parkingLabel);
            agg2.setYear(2026);
            agg2.setMonth(1);
            agg2.setTotalAmount(new BigDecimal("3.50"));
            agg2.setTransactionCount(1L);
            aggregateRepository.save(agg2);
        }

        @Test
        @DisplayName("Should export aggregates as CSV")
        void testExportAggregatesAsCSV() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates/export/csv")
                .param("year", "2026")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
        }

        @Test
        @DisplayName("Should export aggregates with PDF response")
        void testExportAggregatesAsPDF() throws Exception {
            // Arrange
            setupAggregates();

            // Act & Assert
            mockMvc.perform(get("/api/aggregates/export/pdf")
                .param("year", "2026")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
        }
    }

    @Nested
    @DisplayName("Aggregate Error Handling")
    class AggregateErrorHandlingTests {

        @Test
        @DisplayName("Should return 400 for invalid year parameter")
        void testInvalidYearParameter() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("year", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for invalid month parameter")
        void testInvalidMonthParameter() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("year", "2026")
                .param("month", "13")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for month without year")
        void testMonthWithoutYear() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("month", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Aggregate Complex Scenarios")
    class AggregateComplexScenarios {

        @Test
        @DisplayName("Should handle full year monthly breakdown")
        void testFullYearMonthlyBreakdown() throws Exception {
            // Arrange
            for (int month = 1; month <= 12; month++) {
                Aggregate agg = new Aggregate();
                agg.setLabel(groceryLabel);
                agg.setYear(2026);
                agg.setMonth(month);
                agg.setTotalAmount(new BigDecimal(month * 50));
                agg.setTransactionCount((long) month);
                aggregateRepository.save(agg);
            }

            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("year", "2026")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)));
        }

        @Test
        @DisplayName("Should retrieve multi-year data comparison")
        void testMultiYearComparison() throws Exception {
            // Arrange
            for (int year = 2024; year <= 2026; year++) {
                for (int month = 1; month <= 3; month++) {
                    Aggregate agg = new Aggregate();
                    agg.setLabel(groceryLabel);
                    agg.setYear(year);
                    agg.setMonth(month);
                    agg.setTotalAmount(new BigDecimal(year + month * 50));
                    agg.setTransactionCount((long) month);
                    aggregateRepository.save(agg);
                }
            }

            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("labelId", groceryLabel.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)));
        }

        @Test
        @DisplayName("Should handle large dataset retrieval")
        void testLargeDatasetRetrieval() throws Exception {
            // Arrange
            for (int i = 0; i < 50; i++) {
                Label label = new Label();
                label.setName("Label " + i);
                label.setDescription("Description " + i);
                label = labelRepository.save(label);

                Aggregate agg = new Aggregate();
                agg.setLabel(label);
                agg.setYear(2026);
                agg.setMonth((i % 12) + 1);
                agg.setTotalAmount(new BigDecimal(i * 100));
                agg.setTransactionCount((long) i);
                aggregateRepository.save(agg);
            }

            // Act & Assert
            mockMvc.perform(get("/api/aggregates")
                .param("year", "2026")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50)));
        }

        @Test
        @DisplayName("Should retrieve aggregates for payment with multiple labels")
        void testPaymentWithMultipleLabelsAggregates() throws Exception {
            // Arrange - Create aggregates representing same payment with different labels
            Aggregate agg1 = new Aggregate();
            agg1.setLabel(groceryLabel);
            agg1.setYear(2026);
            agg1.setMonth(1);
            agg1.setTotalAmount(new BigDecimal("75.50"));
            agg1.setTransactionCount(1L);
            aggregateRepository.save(agg1);

            Aggregate agg2 = new Aggregate();
            agg2.setLabel(parkingLabel);
            agg2.setYear(2026);
            agg2.setMonth(1);
            agg2.setTotalAmount(new BigDecimal("75.50"));
            agg2.setTransactionCount(1L);
            aggregateRepository.save(agg2);

            Aggregate agg3 = new Aggregate();
            agg3.setLabel(utilitiesLabel);
            agg3.setYear(2026);
            agg3.setMonth(1);
            agg3.setTotalAmount(new BigDecimal("75.50"));
            agg3.setTransactionCount(1L);
            aggregateRepository.save(agg3);

            // Act & Assert - Get all aggregates for the month (multiple labels)
            mockMvc.perform(get("/api/aggregates")
                .param("year", "2026")
                .param("month", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].totalAmount", everyItem(equalTo(75.50))));
        }

        @Test
        @DisplayName("Should retrieve each label independently when payment has multiple labels")
        void testMultipleLabelIndependenceInRetrieval() throws Exception {
            // Arrange - Create aggregates for overlapping labels
            Aggregate groceryAgg = new Aggregate();
            groceryAgg.setLabel(groceryLabel);
            groceryAgg.setYear(2026);
            groceryAgg.setMonth(1);
            groceryAgg.setTotalAmount(new BigDecimal("200.00"));
            groceryAgg.setTransactionCount(2L);
            aggregateRepository.save(groceryAgg);

            Aggregate parkingAgg = new Aggregate();
            parkingAgg.setLabel(parkingLabel);
            parkingAgg.setYear(2026);
            parkingAgg.setMonth(1);
            parkingAgg.setTotalAmount(new BigDecimal("50.00"));
            parkingAgg.setTransactionCount(1L);
            aggregateRepository.save(parkingAgg);

            // Act & Assert - Verify each label can be queried independently
            mockMvc.perform(get("/api/aggregates")
                .param("year", "2026")
                .param("month", "1")
                .param("labelId", groceryLabel.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].totalAmount").value(200.00))
                .andExpect(jsonPath("$[0].transactionCount").value(2));

            mockMvc.perform(get("/api/aggregates")
                .param("year", "2026")
                .param("month", "1")
                .param("labelId", parkingLabel.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].totalAmount").value(50.00))
                .andExpect(jsonPath("$[0].transactionCount").value(1));
        }
    }
}

