package com.paymentlabeling.service;

import com.paymentlabeling.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CSV Parser Service
 * Tests the complete flow: parsing CSV → validating data → detecting duplicates → storing in database
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("CSV Parser Integration Tests")
class CsvParserServiceIntegrationTest {

    @Autowired
    private CsvParserService csvParserService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully parse valid Slovak bank statement CSV")
    void testParseValidSlovakBankStatement() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");
        assertNotNull(csvStream, "Test CSV file should exist");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(2, result.getParsedPayments().size());
        assertEquals(0, result.getSkippedDuplicates());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    @DisplayName("Should correctly parse payment date from CSV")
    void testParsesPaymentDateCorrectly() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);
        List<Payment> payments = result.getParsedPayments();

        // Assert
        assertEquals(LocalDate.of(2026, 1, 23), payments.get(0).getPaymentDate());
        assertEquals(LocalDate.of(2026, 1, 23), payments.get(1).getPaymentDate());
    }

    @Test
    @DisplayName("Should correctly parse amount with proper decimal precision")
    void testParsesAmountWithDecimalPrecision() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);
        List<Payment> payments = result.getParsedPayments();

        // Assert
        assertEquals(new BigDecimal("-52.13"), payments.get(0).getAmount());
        assertEquals(new BigDecimal("-1.12"), payments.get(1).getAmount());
    }

    @Test
    @DisplayName("Should parse all required payment fields correctly")
    void testParsesAllPaymentFields() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);
        Payment payment = result.getParsedPayments().get(0);

        // Assert
        assertNotNull(payment.getPaymentDate());
        assertNotNull(payment.getAmount());
        assertEquals("EUR", payment.getCurrency());
        assertEquals("/VS405000021/SS1130630652/KS0608", payment.getReference());
        assertEquals("Platba kartou", payment.getTransactionType());
        assertEquals("Suma: 52,13 EUR 21.1.2026 Miesto: KAUFLAND 8820 BA I.CE", payment.getReceiverInfo());
    }

    @Test
    @DisplayName("Should skip duplicate payments when parsing CSV")
    void testSkipsDuplicatePayments() {
        // Arrange - First import
        InputStream csvStream1 = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");
        CsvParseResult firstImport = csvParserService.parseCsv(csvStream1);
        assertEquals(2, firstImport.getParsedPayments().size());
        csvParserService.savePayments(firstImport.getParsedPayments());

        // Act - Second import (overlapping)
        InputStream csvStream2 = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");
        CsvParseResult secondImport = csvParserService.parseCsv(csvStream2);
        CsvImportResult importResult = csvParserService.savePaymentsWithDuplicateDetection(
            secondImport.getParsedPayments()
        );

        // Assert
        assertEquals(2, importResult.getDuplicateCount());
        assertEquals(0, importResult.getNewPaymentCount());
        assertEquals(0, importResult.getErrors().size());
    }

    @Test
    @DisplayName("Should detect duplicates using date + amount + reference + account combination")
    void testDuplicateDetectionByUniqueConstraint() {
        // Arrange
        Payment payment1 = Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 23))
            .amount(new BigDecimal("-52.13"))
            .currency("EUR")
            .reference("/VS405000021/SS1130630652/KS0608")
            .transactionType("Platba kartou")
            .accountNumber("SK0575000000004020035946")
            .receiverInfo("Suma: 52,13 EUR 21.1.2026 Miesto: KAUFLAND 8820 BA I.CE")
            .build();

        Payment payment2 = Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 23))
            .amount(new BigDecimal("-52.13"))
            .currency("EUR")
            .reference("/VS405000021/SS1130630652/KS0608")
            .transactionType("Platba kartou")
            .accountNumber("SK0575000000004020035946")
            .receiverInfo("Different receiver info")
            .build();

        paymentRepository.save(payment1);

        // Act
        boolean isDuplicate = paymentRepository.existsByPaymentDateAndAmountAndReferenceAndAccountNumber(
            payment2.getPaymentDate(),
            payment2.getAmount(),
            payment2.getReference(),
            payment2.getAccountNumber()
        );

        // Assert
        assertTrue(isDuplicate, "Payments with same date, amount, reference, and account should be detected as duplicates");
    }

    @Test
    @DisplayName("Should not flag different amounts as duplicates")
    void testDuplicateDetectionIgnoresDifferentAmounts() {
        // Arrange
        Payment payment1 = Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 23))
            .amount(new BigDecimal("-52.13"))
            .currency("EUR")
            .reference("/VS405000021/SS1130630652/KS0608")
            .accountNumber("SK0575000000004020035946")
            .build();

        Payment payment2 = Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 23))
            .amount(new BigDecimal("-52.14"))  // Different amount
            .currency("EUR")
            .reference("/VS405000021/SS1130630652/KS0608")
            .accountNumber("SK0575000000004020035946")
            .build();

        paymentRepository.save(payment1);

        // Act
        boolean isDuplicate = paymentRepository.existsByPaymentDateAndAmountAndReferenceAndAccountNumber(
            payment2.getPaymentDate(),
            payment2.getAmount(),
            payment2.getReference(),
            payment2.getAccountNumber()
        );

        // Assert
        assertFalse(isDuplicate, "Payments with different amounts should not be duplicates");
    }

    @Test
    @DisplayName("Should extract month and year from payment date")
    void testExtractsMonthAndYearFromPaymentDate() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);
        Payment payment = result.getParsedPayments().get(0);

        // Assert
        assertEquals(1, payment.getPaymentDate().getMonthValue()); // January
        assertEquals(2026, payment.getPaymentDate().getYear());
    }

    @Test
    @DisplayName("Should handle CSV with only header lines (empty payment data)")
    void testHandlesEmptyPaymentData() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/empty_bank_statement.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(0, result.getParsedPayments().size());
    }

    @Test
    @DisplayName("Should skip first 3 lines of CSV (header information)")
    void testSkipsFirstThreeHeaderLines() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);

        // Assert
        // Should parse only the payment lines, not the header lines
        assertEquals(2, result.getParsedPayments().size());
        // If header was parsed incorrectly, we'd have more than 2 records or parsing would fail
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle CSV with missing optional fields")
    void testHandlesMissingOptionalFields() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/minimal_payment_statement.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);

        // Assert
        assertTrue(result.isSuccess());
        Payment payment = result.getParsedPayments().get(0);
        assertNotNull(payment.getPaymentDate());
        assertNotNull(payment.getAmount());
        assertNotNull(payment.getCurrency());
        // Optional fields can be null
        assertNull(payment.getCounterpartyName());
    }

    @Test
    @DisplayName("Should return errors for invalid date format")
    void testHandlesInvalidDateFormat() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/invalid_date_format.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);

        // Assert
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("date") || e.contains("Date")),
            "Error message should mention date parsing");
    }

    @Test
    @DisplayName("Should return errors for invalid amount format")
    void testHandlesInvalidAmountFormat() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/invalid_amount_format.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);

        // Assert
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("amount") || e.contains("Amount")),
            "Error message should mention amount parsing");
    }

    @Test
    @DisplayName("Should save parsed payments to database")
    void testSavesParsedPaymentsToDatabase() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");
        CsvParseResult parseResult = csvParserService.parseCsv(csvStream);

        // Act
        csvParserService.savePayments(parseResult.getParsedPayments());

        // Assert
        List<Payment> savedPayments = paymentRepository.findAll();
        assertEquals(2, savedPayments.size());
        assertTrue(savedPayments.stream()
            .anyMatch(p -> p.getAmount().equals(new BigDecimal("-52.13"))));
    }

    @Test
    @DisplayName("Should import multiple CSV files with overlapping records")
    void testImportsMultipleCsvsWithOverlap() {
        // Arrange - First import with 2 payments
        InputStream csvStream1 = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");
        CsvParseResult firstParse = csvParserService.parseCsv(csvStream1);
        csvParserService.savePayments(firstParse.getParsedPayments());

        // Act - Second import with 1 overlapping + 1 new payment
        InputStream csvStream2 = getClass().getResourceAsStream("/csv/overlapping_bank_statement.csv");
        CsvParseResult secondParse = csvParserService.parseCsv(csvStream2);
        CsvImportResult importResult = csvParserService.savePaymentsWithDuplicateDetection(
            secondParse.getParsedPayments()
        );

        // Assert
        assertEquals(1, importResult.getNewPaymentCount(), "Should import 1 new payment");
        assertEquals(1, importResult.getDuplicateCount(), "Should skip 1 duplicate");
        assertEquals(0, importResult.getErrors().size());

        // Verify database state
        List<Payment> allPayments = paymentRepository.findAll();
        assertEquals(3, allPayments.size(), "Total should be 3 payments (2 original + 1 new)");
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void testHandlesNullValuesGracefully() {
        // Arrange
        Payment payment = Payment.builder()
            .paymentDate(LocalDate.of(2026, 1, 23))
            .amount(new BigDecimal("-52.13"))
            .currency("EUR")
            .reference(null)  // Null reference
            .accountNumber(null)  // Null account
            .build();

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> paymentRepository.save(payment));
    }

    @Test
    @DisplayName("Should correctly parse CSV with different delimiters (comma-separated)")
    void testParsesCsvWithCommaDelimiter() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/comma_delimited.csv");

        // Act
        CsvParseResult result = csvParserService.parseCsv(csvStream);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(2, result.getParsedPayments().size());
    }

    @Test
    @DisplayName("Should return import summary with statistics")
    void testReturnsImportSummaryWithStatistics() {
        // Arrange
        InputStream csvStream = getClass().getResourceAsStream("/csv/valid_bank_statement.csv");
        CsvParseResult parseResult = csvParserService.parseCsv(csvStream);

        // Act
        CsvImportResult importResult = csvParserService.savePaymentsWithDuplicateDetection(
            parseResult.getParsedPayments()
        );

        // Assert
        assertEquals(2, importResult.getNewPaymentCount());
        assertEquals(0, importResult.getDuplicateCount());
        assertEquals(0, importResult.getErrors().size());
        assertNotNull(importResult.getImportedAt());
        assertEquals(2, importResult.getTotalProcessed());
    }
}
