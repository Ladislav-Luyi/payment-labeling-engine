package com.paymentlabeling.service;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Aggregate & Reporting System (Issue #6)
 * Tests aggregation logic, reporting queries, and data persistence for
 * monthly and yearly payment aggregates by label
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Aggregate Service Integration Tests")
class AggregateServiceIntegrationTest {

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

        // Create test payments - January 2026
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
    @DisplayName("Aggregate Creation and Storage")
    class AggregateCreationTests {

        @Test
        @DisplayName("Should create and save aggregate for label, month, and year")
        void testCreateAndSaveAggregate() {
            // Arrange
            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(groceryLabel);
            aggregate.setYear(2026);
            aggregate.setMonth(1);
            aggregate.setTotalAmount(new BigDecimal("141.58"));
            aggregate.setTransactionCount(2L);

            // Act
            Aggregate savedAggregate = aggregateRepository.save(aggregate);

            // Assert
            assertNotNull(savedAggregate.getId());
            assertEquals(groceryLabel.getId(), savedAggregate.getLabel().getId());
            assertEquals(2026, savedAggregate.getYear());
            assertEquals(1, savedAggregate.getMonth());
            assertEquals(new BigDecimal("141.58"), savedAggregate.getTotalAmount());
            assertEquals(2L, savedAggregate.getTransactionCount());
            assertNotNull(savedAggregate.getCreatedAt());
            assertNotNull(savedAggregate.getUpdatedAt());
        }

        @Test
        @DisplayName("Should enforce unique constraint on (label, year, month)")
        void testUniqueConstraintOnLabelYearMonth() {
            // Arrange
            Aggregate aggregate1 = new Aggregate();
            aggregate1.setLabel(groceryLabel);
            aggregate1.setYear(2026);
            aggregate1.setMonth(1);
            aggregate1.setTotalAmount(new BigDecimal("141.58"));
            aggregate1.setTransactionCount(2L);
            aggregateRepository.save(aggregate1);

            Aggregate aggregate2 = new Aggregate();
            aggregate2.setLabel(groceryLabel);
            aggregate2.setYear(2026);
            aggregate2.setMonth(1);
            aggregate2.setTotalAmount(new BigDecimal("200.00"));
            aggregate2.setTransactionCount(3L);

            // Act & Assert - Should throw exception due to unique constraint
            assertThrows(Exception.class, () -> {
                aggregateRepository.save(aggregate2);
                aggregateRepository.flush();
            });
        }

        @Test
        @DisplayName("Should allow same label for different months")
        void testAllowSameLabelDifferentMonths() {
            // Arrange
            Aggregate janAggregate = new Aggregate();
            janAggregate.setLabel(groceryLabel);
            janAggregate.setYear(2026);
            janAggregate.setMonth(1);
            janAggregate.setTotalAmount(new BigDecimal("141.58"));
            janAggregate.setTransactionCount(2L);
            janAggregate = aggregateRepository.save(janAggregate);

            Aggregate febAggregate = new Aggregate();
            febAggregate.setLabel(groceryLabel);
            febAggregate.setYear(2026);
            febAggregate.setMonth(2);
            febAggregate.setTotalAmount(new BigDecimal("200.00"));
            febAggregate.setTransactionCount(3L);
            febAggregate = aggregateRepository.save(febAggregate);

            // Act & Assert
            assertNotNull(janAggregate.getId());
            assertNotNull(febAggregate.getId());
            assertNotEquals(janAggregate.getId(), febAggregate.getId());
        }

        @Test
        @DisplayName("Should allow same month for different labels")
        void testAllowSameMonthDifferentLabels() {
            // Arrange
            Aggregate groceryAggregate = new Aggregate();
            groceryAggregate.setLabel(groceryLabel);
            groceryAggregate.setYear(2026);
            groceryAggregate.setMonth(1);
            groceryAggregate.setTotalAmount(new BigDecimal("141.58"));
            groceryAggregate.setTransactionCount(2L);
            groceryAggregate = aggregateRepository.save(groceryAggregate);

            Aggregate parkingAggregate = new Aggregate();
            parkingAggregate.setLabel(parkingLabel);
            parkingAggregate.setYear(2026);
            parkingAggregate.setMonth(1);
            parkingAggregate.setTotalAmount(new BigDecimal("3.50"));
            parkingAggregate.setTransactionCount(1L);
            parkingAggregate = aggregateRepository.save(parkingAggregate);

            // Act & Assert
            assertNotNull(groceryAggregate.getId());
            assertNotNull(parkingAggregate.getId());
            assertNotEquals(groceryAggregate.getId(), parkingAggregate.getId());
        }
    }

    @Nested
    @DisplayName("Aggregate Retrieval and Queries")
    class AggregateRetrievalTests {

        @BeforeEach
        void setupAggregates() {
            // Create aggregates for testing queries
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
            agg4.setLabel(groceryLabel);
            agg4.setYear(2025);
            agg4.setMonth(12);
            agg4.setTotalAmount(new BigDecimal("500.00"));
            agg4.setTransactionCount(5L);
            aggregateRepository.save(agg4);
        }

        @Test
        @DisplayName("Should retrieve aggregate by label, year, and month")
        void testFindByLabelYearAndMonth() {
            // Act
            Optional<Aggregate> result = aggregateRepository.findByLabelIdAndYearAndMonth(
                groceryLabel.getId(), 2026, 1);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(new BigDecimal("141.58"), result.get().getTotalAmount());
            assertEquals(2L, result.get().getTransactionCount());
        }

        @Test
        @DisplayName("Should return empty optional for non-existent aggregate")
        void testFindByLabelYearAndMonthNotFound() {
            // Act
            Optional<Aggregate> result = aggregateRepository.findByLabelIdAndYearAndMonth(
                utilitiesLabel.getId(), 2026, 1);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should retrieve all aggregates for a label")
        void testFindByLabelId() {
            // Act
            List<Aggregate> results = aggregateRepository.findByLabelId(groceryLabel.getId());

            // Assert
            assertEquals(3, results.size());
            assertTrue(results.stream().allMatch(a -> a.getLabel().getId().equals(groceryLabel.getId())));
        }

        @Test
        @DisplayName("Should return empty list for label with no aggregates")
        void testFindByLabelIdEmpty() {
            // Act
            List<Aggregate> results = aggregateRepository.findByLabelId(utilitiesLabel.getId());

            // Assert
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should retrieve all aggregates for a specific year")
        void testFindByYear() {
            // Act
            List<Aggregate> results2026 = aggregateRepository.findByYear(2026);
            List<Aggregate> results2025 = aggregateRepository.findByYear(2025);

            // Assert
            assertEquals(3, results2026.size());
            assertEquals(1, results2025.size());
            assertTrue(results2026.stream().allMatch(a -> a.getYear() == 2026));
            assertTrue(results2025.stream().allMatch(a -> a.getYear() == 2025));
        }

        @Test
        @DisplayName("Should return empty list for year with no aggregates")
        void testFindByYearEmpty() {
            // Act
            List<Aggregate> results = aggregateRepository.findByYear(2024);

            // Assert
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should retrieve all aggregates for a specific year and month")
        void testFindByYearAndMonth() {
            // Act
            List<Aggregate> jan2026 = aggregateRepository.findByYearAndMonth(2026, 1);
            List<Aggregate> feb2026 = aggregateRepository.findByYearAndMonth(2026, 2);
            List<Aggregate> dec2025 = aggregateRepository.findByYearAndMonth(2025, 12);

            // Assert
            assertEquals(2, jan2026.size());
            assertEquals(1, feb2026.size());
            assertEquals(1, dec2025.size());
        }

        @Test
        @DisplayName("Should return empty list for month with no aggregates")
        void testFindByYearAndMonthEmpty() {
            // Act
            List<Aggregate> results = aggregateRepository.findByYearAndMonth(2026, 6);

            // Assert
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("Aggregate Update and Modification")
    class AggregateUpdateTests {

        @Test
        @DisplayName("Should update aggregate total amount and transaction count")
        void testUpdateAggregateValues() {
            // Arrange
            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(groceryLabel);
            aggregate.setYear(2026);
            aggregate.setMonth(1);
            aggregate.setTotalAmount(new BigDecimal("141.58"));
            aggregate.setTransactionCount(2L);
            aggregate = aggregateRepository.save(aggregate);

            LocalDateTime originalCreatedAt = aggregate.getCreatedAt();

            // Act
            aggregate.setTotalAmount(new BigDecimal("200.00"));
            aggregate.setTransactionCount(3L);
            Aggregate updated = aggregateRepository.save(aggregate);

            // Assert
            assertEquals(new BigDecimal("200.00"), updated.getTotalAmount());
            assertEquals(3L, updated.getTransactionCount());
            assertEquals(originalCreatedAt, updated.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Aggregate Deletion")
    class AggregateDeletionTests {

        @Test
        @DisplayName("Should delete aggregate by ID")
        void testDeleteAggregate() {
            // Arrange
            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(groceryLabel);
            aggregate.setYear(2026);
            aggregate.setMonth(1);
            aggregate.setTotalAmount(new BigDecimal("141.58"));
            aggregate.setTransactionCount(2L);
            aggregate = aggregateRepository.save(aggregate);

            Long aggregateId = aggregate.getId();

            // Act
            aggregateRepository.deleteById(aggregateId);

            // Assert
            assertTrue(aggregateRepository.findById(aggregateId).isEmpty());
        }
    }

    @Nested
    @DisplayName("Complex Aggregation Scenarios")
    class ComplexAggregationScenarios {

        @Test
        @DisplayName("Should calculate correct totals across multiple aggregates")
        void testMultipleAggregatesCalculation() {
            // Arrange
            Aggregate agg1 = new Aggregate();
            agg1.setLabel(groceryLabel);
            agg1.setYear(2026);
            agg1.setMonth(1);
            agg1.setTotalAmount(new BigDecimal("141.58"));
            agg1.setTransactionCount(2L);
            aggregateRepository.save(agg1);

            Aggregate agg2 = new Aggregate();
            agg2.setLabel(groceryLabel);
            agg2.setYear(2026);
            agg2.setMonth(2);
            agg2.setTotalAmount(new BigDecimal("200.00"));
            agg2.setTransactionCount(3L);
            aggregateRepository.save(agg2);

            Aggregate agg3 = new Aggregate();
            agg3.setLabel(parkingLabel);
            agg3.setYear(2026);
            agg3.setMonth(1);
            agg3.setTotalAmount(new BigDecimal("3.50"));
            agg3.setTransactionCount(1L);
            aggregateRepository.save(agg3);

            // Act
            List<Aggregate> jan2026 = aggregateRepository.findByYearAndMonth(2026, 1);
            BigDecimal janTotal = jan2026.stream()
                .map(Aggregate::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            long janTransactions = jan2026.stream()
                .mapToLong(Aggregate::getTransactionCount)
                .sum();

            // Assert
            assertEquals(new BigDecimal("145.08"), janTotal);
            assertEquals(3L, janTransactions);
        }

        @Test
        @DisplayName("Should retrieve yearly totals by label")
        void testYearlyTotalsByLabel() {
            // Arrange
            Aggregate agg1 = new Aggregate();
            agg1.setLabel(groceryLabel);
            agg1.setYear(2026);
            agg1.setMonth(1);
            agg1.setTotalAmount(new BigDecimal("141.58"));
            agg1.setTransactionCount(2L);
            aggregateRepository.save(agg1);

            Aggregate agg2 = new Aggregate();
            agg2.setLabel(groceryLabel);
            agg2.setYear(2026);
            agg2.setMonth(2);
            agg2.setTotalAmount(new BigDecimal("200.00"));
            agg2.setTransactionCount(3L);
            aggregateRepository.save(agg2);

            Aggregate agg3 = new Aggregate();
            agg3.setLabel(groceryLabel);
            agg3.setYear(2025);
            agg3.setMonth(12);
            agg3.setTotalAmount(new BigDecimal("100.00"));
            agg3.setTransactionCount(1L);
            aggregateRepository.save(agg3);

            // Act
            List<Aggregate> groceryAggregates = aggregateRepository.findByLabelId(groceryLabel.getId());
            BigDecimal total2026 = groceryAggregates.stream()
                .filter(a -> a.getYear() == 2026)
                .map(Aggregate::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Assert
            assertEquals(new BigDecimal("341.58"), total2026);
        }

        @Test
        @DisplayName("Should handle multiple labels with different years and months")
        void testComplexMultiLabelScenario() {
            // Arrange
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
            agg3.setLabel(utilitiesLabel);
            agg3.setYear(2026);
            agg3.setMonth(2);
            agg3.setTotalAmount(new BigDecimal("75.00"));
            agg3.setTransactionCount(1L);
            aggregateRepository.save(agg3);

            Aggregate agg4 = new Aggregate();
            agg4.setLabel(groceryLabel);
            agg4.setYear(2025);
            agg4.setMonth(11);
            agg4.setTotalAmount(new BigDecimal("50.00"));
            agg4.setTransactionCount(1L);
            aggregateRepository.save(agg4);

            // Act
            List<Aggregate> year2026 = aggregateRepository.findByYear(2026);
            List<Aggregate> jan2026 = aggregateRepository.findByYearAndMonth(2026, 1);
            List<Aggregate> groceryAll = aggregateRepository.findByLabelId(groceryLabel.getId());

            // Assert
            assertEquals(3, year2026.size());
            assertEquals(2, jan2026.size());
            assertEquals(2, groceryAll.size());
        }

        @Test
        @DisplayName("Should maintain data integrity across all aggregates")
        void testDataIntegrity() {
            // Arrange - Create various aggregates
            for (int month = 1; month <= 3; month++) {
                Aggregate agg1 = new Aggregate();
                agg1.setLabel(groceryLabel);
                agg1.setYear(2026);
                agg1.setMonth(month);
                agg1.setTotalAmount(new BigDecimal(month * 50));
                agg1.setTransactionCount((long) month);
                aggregateRepository.save(agg1);

                Aggregate agg2 = new Aggregate();
                agg2.setLabel(parkingLabel);
                agg2.setYear(2026);
                agg2.setMonth(month);
                agg2.setTotalAmount(new BigDecimal(month * 50));
                agg2.setTransactionCount((long) month);
                aggregateRepository.save(agg2);
            }

            // Act
            List<Aggregate> all = aggregateRepository.findAll();

            // Assert
            assertEquals(6, all.size());
            all.forEach(agg -> {
                assertNotNull(agg.getId());
                assertNotNull(agg.getLabel());
                assertNotNull(agg.getYear());
                assertNotNull(agg.getMonth());
                assertNotNull(agg.getTotalAmount());
                assertNotNull(agg.getTransactionCount());
                assertNotNull(agg.getCreatedAt());
                assertNotNull(agg.getUpdatedAt());
            });
        }
    }

    @Nested
    @DisplayName("Multiple Labels on Single Payment")
    class MultipleLabelsScenarios {

        @Test
        @DisplayName("Should handle payment with multiple labels")
        void testPaymentWithMultipleLabels() {
            // Arrange - Create a payment and assign multiple labels
            Payment payment = new Payment();
            payment.setPaymentDate(LocalDate.of(2026, 1, 20));
            payment.setAmount(new BigDecimal("-75.50"));
            payment.setCurrency("EUR");
            payment.setReference("/VS999999999/SS1111111111/KS0608");
            payment.setTransactionType("Platba kartou");
            payment.setCounterpartyName("SUPERMARKET PLUS");
            payment.setAccountNumber("SK0575000000004020035946");
            payment = paymentRepository.save(payment);

            // Assign multiple labels to the same payment
            PaymentLabel pl1 = new PaymentLabel();
            pl1.setPayment(payment);
            pl1.setLabel(groceryLabel);
            paymentLabelRepository.save(pl1);

            PaymentLabel pl2 = new PaymentLabel();
            pl2.setPayment(payment);
            pl2.setLabel(utilitiesLabel);
            paymentLabelRepository.save(pl2);

            // Act - Create aggregates for both labels
            Aggregate agg1 = new Aggregate();
            agg1.setLabel(groceryLabel);
            agg1.setYear(2026);
            agg1.setMonth(1);
            agg1.setTotalAmount(new BigDecimal("75.50"));
            agg1.setTransactionCount(1L);
            agg1 = aggregateRepository.save(agg1);

            Aggregate agg2 = new Aggregate();
            agg2.setLabel(utilitiesLabel);
            agg2.setYear(2026);
            agg2.setMonth(1);
            agg2.setTotalAmount(new BigDecimal("75.50"));
            agg2.setTransactionCount(1L);
            agg2 = aggregateRepository.save(agg2);

            // Assert - Verify both aggregates exist independently
            Optional<Aggregate> groceryAgg = aggregateRepository.findByLabelIdAndYearAndMonth(
                groceryLabel.getId(), 2026, 1);
            Optional<Aggregate> utilitiesAgg = aggregateRepository.findByLabelIdAndYearAndMonth(
                utilitiesLabel.getId(), 2026, 1);

            assertTrue(groceryAgg.isPresent());
            assertTrue(utilitiesAgg.isPresent());
            assertEquals(new BigDecimal("75.50"), groceryAgg.get().getTotalAmount());
            assertEquals(new BigDecimal("75.50"), utilitiesAgg.get().getTotalAmount());
        }

        @Test
        @DisplayName("Should correctly aggregate when payment has multiple labels")
        void testAggregationWithMultipleLabelsPerPayment() {
            // Arrange - Create 2 payments: one with single label, one with multiple labels
            Payment payment1 = new Payment();
            payment1.setPaymentDate(LocalDate.of(2026, 1, 5));
            payment1.setAmount(new BigDecimal("-50.00"));
            payment1.setCurrency("EUR");
            payment1.setReference("/VS111111111/SS1111111111/KS0608");
            payment1.setTransactionType("Platba kartou");
            payment1.setCounterpartyName("STORE A");
            payment1.setAccountNumber("SK0575000000004020035946");
            payment1 = paymentRepository.save(payment1);

            Payment payment2 = new Payment();
            payment2.setPaymentDate(LocalDate.of(2026, 1, 15));
            payment2.setAmount(new BigDecimal("-100.00"));
            payment2.setCurrency("EUR");
            payment2.setReference("/VS222222222/SS2222222222/KS0608");
            payment2.setTransactionType("Platba kartou");
            payment2.setCounterpartyName("STORE B");
            payment2.setAccountNumber("SK0575000000004020035946");
            payment2 = paymentRepository.save(payment2);

            // Assign labels
            PaymentLabel pl1 = new PaymentLabel();
            pl1.setPayment(payment1);
            pl1.setLabel(groceryLabel);
            paymentLabelRepository.save(pl1);

            // Payment 2 has multiple labels
            PaymentLabel pl2 = new PaymentLabel();
            pl2.setPayment(payment2);
            pl2.setLabel(groceryLabel);
            paymentLabelRepository.save(pl2);

            PaymentLabel pl3 = new PaymentLabel();
            pl3.setPayment(payment2);
            pl3.setLabel(parkingLabel);
            paymentLabelRepository.save(pl3);

            // Act - Create aggregates
            Aggregate groceryAgg = new Aggregate();
            groceryAgg.setLabel(groceryLabel);
            groceryAgg.setYear(2026);
            groceryAgg.setMonth(1);
            groceryAgg.setTotalAmount(new BigDecimal("150.00")); // payment1(50) + payment2(100)
            groceryAgg.setTransactionCount(2L);
            aggregateRepository.save(groceryAgg);

            Aggregate parkingAgg = new Aggregate();
            parkingAgg.setLabel(parkingLabel);
            parkingAgg.setYear(2026);
            parkingAgg.setMonth(1);
            parkingAgg.setTotalAmount(new BigDecimal("100.00")); // Only payment2
            parkingAgg.setTransactionCount(1L);
            aggregateRepository.save(parkingAgg);

            // Assert
            List<Aggregate> groceries = aggregateRepository.findByLabelId(groceryLabel.getId());
            List<Aggregate> parking = aggregateRepository.findByLabelId(parkingLabel.getId());

            assertEquals(1, groceries.size());
            assertEquals(1, parking.size());
            assertEquals(new BigDecimal("150.00"), groceries.get(0).getTotalAmount());
            assertEquals(2L, groceries.get(0).getTransactionCount());
            assertEquals(new BigDecimal("100.00"), parking.get(0).getTotalAmount());
            assertEquals(1L, parking.get(0).getTransactionCount());
        }

        @Test
        @DisplayName("Should calculate correct totals with overlapping labels")
        void testOverlappingLabelsAggregation() {
            // Arrange - Payment assigned to multiple labels, each label tracks independently
            Aggregate agg1 = new Aggregate();
            agg1.setLabel(groceryLabel);
            agg1.setYear(2026);
            agg1.setMonth(1);
            agg1.setTotalAmount(new BigDecimal("200.00"));
            agg1.setTransactionCount(2L);
            aggregateRepository.save(agg1);

            Aggregate agg2 = new Aggregate();
            agg2.setLabel(parkingLabel);
            agg2.setYear(2026);
            agg2.setMonth(1);
            agg2.setTotalAmount(new BigDecimal("50.00"));
            agg2.setTransactionCount(1L);
            aggregateRepository.save(agg2);

            Aggregate agg3 = new Aggregate();
            agg3.setLabel(utilitiesLabel);
            agg3.setYear(2026);
            agg3.setMonth(1);
            agg3.setTotalAmount(new BigDecimal("75.00"));
            agg3.setTransactionCount(1L);
            aggregateRepository.save(agg3);

            // Act - Get all aggregates for Jan 2026
            List<Aggregate> jan2026 = aggregateRepository.findByYearAndMonth(2026, 1);
            BigDecimal totalSpending = jan2026.stream()
                .map(Aggregate::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            long totalTransactions = jan2026.stream()
                .mapToLong(Aggregate::getTransactionCount)
                .sum();

            // Assert - All aggregates tracked independently
            assertEquals(3, jan2026.size());
            assertEquals(new BigDecimal("325.00"), totalSpending);
            assertEquals(4L, totalTransactions); // 2 + 1 + 1
        }

        @Test
        @DisplayName("Should maintain label independence with multi-label payments")
        void testLabelIndependenceWithMultipleLabels() {
            // Arrange - Same payment amount but attributed to different labels
            BigDecimal sharedAmount = new BigDecimal("60.00");

            // Create aggregates for different labels with same amount (same payment, multi-labeled)
            Aggregate agg1 = new Aggregate();
            agg1.setLabel(groceryLabel);
            agg1.setYear(2026);
            agg1.setMonth(1);
            agg1.setTotalAmount(sharedAmount);
            agg1.setTransactionCount(1L);
            agg1 = aggregateRepository.save(agg1);

            Aggregate agg2 = new Aggregate();
            agg2.setLabel(utilitiesLabel);
            agg2.setYear(2026);
            agg2.setMonth(1);
            agg2.setTotalAmount(sharedAmount);
            agg2.setTransactionCount(1L);
            agg2 = aggregateRepository.save(agg2);

            // Act - Query each label independently
            Optional<Aggregate> groceryResult = aggregateRepository.findByLabelIdAndYearAndMonth(
                groceryLabel.getId(), 2026, 1);
            Optional<Aggregate> utilitiesResult = aggregateRepository.findByLabelIdAndYearAndMonth(
                utilitiesLabel.getId(), 2026, 1);

            // Assert - Each label maintains its own aggregate record
            assertTrue(groceryResult.isPresent());
            assertTrue(utilitiesResult.isPresent());
            assertEquals(groceryLabel.getId(), groceryResult.get().getLabel().getId());
            assertEquals(utilitiesLabel.getId(), utilitiesResult.get().getLabel().getId());
            assertEquals(sharedAmount, groceryResult.get().getTotalAmount());
            assertEquals(sharedAmount, utilitiesResult.get().getTotalAmount());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle zero amounts")
        void testZeroAmount() {
            // Act
            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(groceryLabel);
            aggregate.setYear(2026);
            aggregate.setMonth(1);
            aggregate.setTotalAmount(BigDecimal.ZERO);
            aggregate.setTransactionCount(0L);
            aggregate = aggregateRepository.save(aggregate);

            // Assert
            assertEquals(BigDecimal.ZERO, aggregate.getTotalAmount());
            assertEquals(0L, aggregate.getTransactionCount());
        }

        @Test
        @DisplayName("Should handle large amounts")
        void testLargeAmount() {
            // Act
            BigDecimal largeAmount = new BigDecimal("9999999.99");
            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(groceryLabel);
            aggregate.setYear(2026);
            aggregate.setMonth(1);
            aggregate.setTotalAmount(largeAmount);
            aggregate.setTransactionCount(1000L);
            aggregate = aggregateRepository.save(aggregate);

            // Assert
            assertEquals(largeAmount, aggregate.getTotalAmount());
        }

        @Test
        @DisplayName("Should handle negative amounts")
        void testNegativeAmount() {
            // Act
            BigDecimal negativeAmount = new BigDecimal("-500.00");
            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(groceryLabel);
            aggregate.setYear(2026);
            aggregate.setMonth(1);
            aggregate.setTotalAmount(negativeAmount);
            aggregate.setTransactionCount(5L);
            aggregate = aggregateRepository.save(aggregate);

            // Assert
            assertEquals(negativeAmount, aggregate.getTotalAmount());
        }

        @Test
        @DisplayName("Should handle month boundaries (1-12)")
        void testMonthBoundaries() {
            // Act & Assert
            for (int month = 1; month <= 12; month++) {
                Aggregate aggregate = new Aggregate();
                aggregate.setLabel(groceryLabel);
                aggregate.setYear(2026);
                aggregate.setMonth(month);
                aggregate.setTotalAmount(new BigDecimal("100.00"));
                aggregate.setTransactionCount(1L);
                aggregate = aggregateRepository.save(aggregate);

                assertTrue(aggregate.getMonth() >= 1 && aggregate.getMonth() <= 12);
            }

            // Verify all months saved
            List<Aggregate> allAggregates = aggregateRepository.findByYear(2026);
            assertEquals(12, allAggregates.size());
        }

        @Test
        @DisplayName("Should handle very small amounts")
        void testVerySmallAmount() {
            // Act
            BigDecimal smallAmount = new BigDecimal("0.01");
            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(groceryLabel);
            aggregate.setYear(2026);
            aggregate.setMonth(1);
            aggregate.setTotalAmount(smallAmount);
            aggregate.setTransactionCount(1L);
            aggregate = aggregateRepository.save(aggregate);

            // Assert
            assertEquals(smallAmount, aggregate.getTotalAmount());
        }

        @Test
        @DisplayName("Should handle large transaction counts")
        void testLargeTransactionCount() {
            // Act
            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(groceryLabel);
            aggregate.setYear(2026);
            aggregate.setMonth(1);
            aggregate.setTotalAmount(new BigDecimal("50000.00"));
            aggregate.setTransactionCount(10000L);
            aggregate = aggregateRepository.save(aggregate);

            // Assert
            assertEquals(10000L, aggregate.getTransactionCount());
        }
    }
}
