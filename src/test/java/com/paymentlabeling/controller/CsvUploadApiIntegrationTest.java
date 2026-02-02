package com.paymentlabeling.controller;

import com.paymentlabeling.repository.PaymentRepository;
import com.paymentlabeling.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CSV Upload API endpoints (Issue #10)
 * Tests the REST API for uploading and parsing CSV payment files
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CSV Upload API Integration Tests")
class CsvUploadApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        // Clear data
        paymentRepository.deleteAll();
    }

    @Nested
    @DisplayName("CSV Upload Functionality")
    class CsvUploadTests {

        @Test
        @DisplayName("Should upload valid CSV file successfully")
        void testUploadValidCsvFile() throws Exception {
            // Arrange
            String csvFileName = "valid_bank_statement.csv";
            byte[] csvContent = Files.readAllBytes(
                Paths.get("src/test/resources/csv/valid_bank_statement.csv")
            );
            MockMultipartFile file = new MockMultipartFile(
                "file",
                csvFileName,
                MediaType.TEXT_PLAIN_VALUE,
                csvContent
            );

            long countBefore = paymentRepository.count();

            // Act & Assert
            mockMvc.perform(multipart("/api/payments/upload")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newPaymentCount").isNumber())
                .andExpect(jsonPath("$.duplicateCount").isNumber())
                .andExpect(jsonPath("$.importedAt").exists());

            long countAfter = paymentRepository.count();
            assertTrue(countAfter > countBefore, "Payment count should increase after upload");
        }

        @Test
        @DisplayName("Should handle CSV file with no file uploaded")
        void testUploadWithoutFile() throws Exception {
            // Act & Assert
            mockMvc.perform(multipart("/api/payments/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should detect duplicates during upload")
        void testUploadWithDuplicates() throws Exception {
            // Arrange - upload the same file twice
            String csvFileName = "valid_bank_statement.csv";
            byte[] csvContent = Files.readAllBytes(
                Paths.get("src/test/resources/csv/valid_bank_statement.csv")
            );

            MockMultipartFile file1 = new MockMultipartFile(
                "file",
                csvFileName,
                MediaType.TEXT_PLAIN_VALUE,
                csvContent
            );

            // First upload
            mockMvc.perform(multipart("/api/payments/upload")
                .file(file1)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            long firstUploadCount = paymentRepository.count();

            // Second upload with same file
            MockMultipartFile file2 = new MockMultipartFile(
                "file",
                csvFileName,
                MediaType.TEXT_PLAIN_VALUE,
                csvContent
            );

            // Act & Assert
            mockMvc.perform(multipart("/api/payments/upload")
                .file(file2)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.duplicateCount").value(greaterThan(0)));

            long secondUploadCount = paymentRepository.count();
            assertEquals(firstUploadCount, secondUploadCount, "No new payments should be added on duplicate upload");
        }

        @Test
        @DisplayName("Should handle CSV file with parsing errors")
        void testUploadCsvWithErrors() throws Exception {
            // Arrange
            String csvFileName = "invalid_date_format.csv";
            byte[] csvContent = Files.readAllBytes(
                Paths.get("src/test/resources/csv/invalid_date_format.csv")
            );
            MockMultipartFile file = new MockMultipartFile(
                "file",
                csvFileName,
                MediaType.TEXT_PLAIN_VALUE,
                csvContent
            );

            // Act & Assert
            mockMvc.perform(multipart("/api/payments/upload")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Should handle empty CSV file")
        void testUploadEmptyCsv() throws Exception {
            // Arrange
            String csvFileName = "empty_bank_statement.csv";
            byte[] csvContent = Files.readAllBytes(
                Paths.get("src/test/resources/csv/empty_bank_statement.csv")
            );
            MockMultipartFile file = new MockMultipartFile(
                "file",
                csvFileName,
                MediaType.TEXT_PLAIN_VALUE,
                csvContent
            );

            long countBefore = paymentRepository.count();

            // Act & Assert
            mockMvc.perform(multipart("/api/payments/upload")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newPaymentCount").value(0));

            long countAfter = paymentRepository.count();
            assertEquals(countBefore, countAfter, "No new payments should be added for empty file");
        }

        @Test
        @DisplayName("Should return response with import result metadata")
        void testUploadResponseStructure() throws Exception {
            // Arrange
            String csvFileName = "valid_bank_statement.csv";
            byte[] csvContent = Files.readAllBytes(
                Paths.get("src/test/resources/csv/valid_bank_statement.csv")
            );
            MockMultipartFile file = new MockMultipartFile(
                "file",
                csvFileName,
                MediaType.TEXT_PLAIN_VALUE,
                csvContent
            );

            // Act & Assert
            mockMvc.perform(multipart("/api/payments/upload")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.newPaymentCount").exists())
                .andExpect(jsonPath("$.duplicateCount").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.importedAt").exists())
                .andExpect(jsonPath("$.totalProcessed").exists());
        }
    }
}
