package com.paymentlabeling.service;

import com.paymentlabeling.model.Payment;
import com.paymentlabeling.repository.PaymentRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of CSV Parser Service for Slovak bank statements
 */
@Service
public class CsvParserServiceImpl implements CsvParserService {

    private final PaymentRepository paymentRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int HEADER_LINES_TO_SKIP = 3;

    public CsvParserServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public CsvParseResult parseCsv(InputStream csvStream) {
        List<Payment> payments = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            // Skip first 3 header lines
            for (int i = 0; i < HEADER_LINES_TO_SKIP; i++) {
                String line = reader.readLine();
                if (line == null) {
                    // File doesn't have enough lines, just return empty result
                    return createParseResult(payments, errors, 0, true);
                }
            }

            // Parse CSV with proper format (no header since we've already skipped 3 lines)
            CSVFormat format = CSVFormat.DEFAULT
                .withIgnoreEmptyLines()
                .withTrim();

            CSVParser csvParser = format.parse(reader);

            for (CSVRecord record : csvParser) {
                // Skip empty records
                if (record.size() == 0) {
                    continue;
                }
                
                // Skip if it looks like a header (contains text like "datum" or "suma")
                String firstCol = record.get(0).toLowerCase();
                if (firstCol.contains("datum") || firstCol.contains("total")) {
                    continue;
                }
                
                try {
                    Payment payment = parsePaymentRecord(record);
                    if (payment != null) {
                        payments.add(payment);
                    }
                } catch (IllegalArgumentException e) {
                    errors.add("Row " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }

            csvParser.close();

        } catch (Exception e) {
            errors.add("Failed to read CSV file: " + e.getMessage());
        }

        return createParseResult(payments, errors, 0, errors.isEmpty());
    }

    private CsvParseResult createParseResult(List<Payment> payments, List<String> errors, int skippedDuplicates, boolean success) {
        CsvParseResult result = new CsvParseResult();
        result.setParsedPayments(payments);
        result.setSkippedDuplicates(skippedDuplicates);
        result.setErrors(errors);
        result.setSuccess(success);
        return result;
    }

    /**
     * Parse a single CSV record into a Payment object
     */
    private Payment parsePaymentRecord(CSVRecord record) {
        try {
            // The CSV has at least 3 required fields: datum, suma, mena
            if (record.size() < 3) {
                return null; // Skip records with insufficient data
            }
            
            // Extract fields from CSV record, handling missing optional fields
            String dateStr = record.get(0); // datum zauctovania (required)
            String amountStr = record.get(1); // suma (required)
            String currency = record.size() > 2 ? record.get(2) : ""; // mena (required)
            String reference = record.size() > 3 ? record.get(3) : ""; // referencia platitela
            String transactionType = record.size() > 4 ? record.get(4) : ""; // typ transakcie
            String counterpartyAccount = record.size() > 5 ? record.get(5) : ""; // cislo uctu protistrany
            String counterpartyBank = record.size() > 6 ? record.get(6) : ""; // banka protistrany
            String counterpartyName = record.size() > 7 ? record.get(7) : ""; // nazov protistrany
            String receiverInfo = record.size() > 8 ? record.get(8) : ""; // informacia pre prijemcu
            // record.get(9) is doplnujuce udaje (additional info) if present

            // Parse and validate date (DD.MM.YYYY format)
            LocalDate paymentDate;
            try {
                paymentDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format: '" + dateStr + "'. Expected format: DD.MM.YYYY");
            }

            // Parse and validate amount (BigDecimal with 2 decimal places)
            BigDecimal amount;
            try {
                // Replace comma with dot for proper decimal parsing
                String normalizedAmount = amountStr.replace(",", ".");
                amount = new BigDecimal(normalizedAmount);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid amount format: '" + amountStr + "'. Expected a valid number");
            }

            // Build Payment entity
            Payment payment = new Payment();
            payment.setPaymentDate(paymentDate);
            payment.setAmount(amount);
            payment.setCurrency(isEmpty(currency) ? null : currency);
            payment.setReference(isEmpty(reference) ? null : reference);
            payment.setTransactionType(isEmpty(transactionType) ? null : transactionType);
            payment.setCounterpartyAccount(isEmpty(counterpartyAccount) ? null : counterpartyAccount);
            payment.setCounterpartyBank(isEmpty(counterpartyBank) ? null : counterpartyBank);
            payment.setCounterpartyName(isEmpty(counterpartyName) ? null : counterpartyName);
            payment.setReceiverInfo(isEmpty(receiverInfo) ? null : receiverInfo);
            payment.setAccountNumber("SK0575000000004020035946");

            return payment;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing payment record: " + e.getMessage());
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    @Override
    @Transactional
    public void savePayments(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return;
        }
        paymentRepository.saveAll(payments);
    }

    @Override
    @Transactional
    public CsvImportResult savePaymentsWithDuplicateDetection(List<Payment> payments) {
        long newPaymentCount = 0;
        long duplicateCount = 0;
        List<String> errors = new ArrayList<>();

        if (payments == null || payments.isEmpty()) {
            return createImportResult(0, 0, errors);
        }

        for (Payment payment : payments) {
            try {
                // Check if payment already exists using unique constraint fields
                boolean isDuplicate = paymentRepository.existsByPaymentDateAndAmountAndReferenceAndAccountNumber(
                    payment.getPaymentDate(),
                    payment.getAmount(),
                    payment.getReference(),
                    payment.getAccountNumber()
                );

                if (isDuplicate) {
                    duplicateCount++;
                } else {
                    paymentRepository.save(payment);
                    newPaymentCount++;
                }
            } catch (Exception e) {
                duplicateCount++;
                errors.add("Error processing payment: " + e.getMessage());
            }
        }

        return createImportResult(newPaymentCount, duplicateCount, errors);
    }

    private CsvImportResult createImportResult(long newCount, long duplicateCount, List<String> errors) {
        CsvImportResult result = new CsvImportResult();
        result.setNewPaymentCount(newCount);
        result.setDuplicateCount(duplicateCount);
        result.setErrors(errors);
        result.setImportedAt(LocalDateTime.now());
        return result;
    }
}
