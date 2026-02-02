package com.paymentlabeling.controller;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.service.CsvImportResult;
import com.paymentlabeling.service.CsvParseResult;
import com.paymentlabeling.service.CsvParserService;
import com.paymentlabeling.service.LabelService;
import com.paymentlabeling.service.PaymentLabelService;
import com.paymentlabeling.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentLabelingController {
  private final PaymentService paymentService;
  private final PaymentLabelService paymentLabelService;
  private final LabelService labelService;
  private final CsvParserService csvParserService;

  @Autowired
  public PaymentLabelingController(PaymentService paymentService, PaymentLabelService paymentLabelService, LabelService labelService, CsvParserService csvParserService) {
    this.paymentService = paymentService;
    this.paymentLabelService = paymentLabelService;
    this.labelService = labelService;
    this.csvParserService = csvParserService;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> getAllPayments(
      @RequestParam(required = false) Long labelId,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    List<Payment> payments = paymentService.getAllPayments();
    var filteredByLabel = applyLabelFilter(payments, labelId);
    var filteredByDate = applyDateRangeFilter(filteredByLabel, startDate, endDate);
    if (filteredByLabel == null || filteredByDate == null) return ResponseEntity.badRequest().build();
    return ResponseEntity.ok(buildPaymentsResponse(filteredByDate));
  }

  @GetMapping("/{paymentId}")
  public ResponseEntity<Map<String, Object>> getPaymentById(@PathVariable Long paymentId) {
    if (!isValidId(paymentId)) return ResponseEntity.badRequest().build();
    Payment payment = paymentService.getPaymentById(paymentId);
    if (payment == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(buildPaymentDto(payment));
  }

  @GetMapping("/search")
  public ResponseEntity<Map<String, Object>> searchPaymentsByCounterpartyName(@RequestParam String query) {
    if (isEmptyQuery(query)) return ResponseEntity.badRequest().build();
    List<Payment> allPayments = paymentService.getAllPayments();
    var results = filterPaymentsByCounterpartyName(allPayments, query);
    return ResponseEntity.ok(buildPaymentsResponse(results));
  }

  @PostMapping("/upload")
  public ResponseEntity<Map<String, Object>> uploadCsv(@RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "message", "File is required"
      ));
    }

    try {
      // Parse the CSV file
      CsvParseResult parseResult = csvParserService.parseCsv(file.getInputStream());

      if (!parseResult.isSuccess() && parseResult.getParsedPayments().isEmpty()) {
        return ResponseEntity.ok(buildImportErrorResponse(parseResult));
      }

      // Save payments with duplicate detection
      CsvImportResult importResult = csvParserService.savePaymentsWithDuplicateDetection(parseResult.getParsedPayments());

      return ResponseEntity.ok(buildImportSuccessResponse(importResult, parseResult));
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "success", false,
          "message", "Error processing file: " + e.getMessage()
      ));
    }
  }

  @PostMapping("/{paymentId}/labels/{labelId}")
  public ResponseEntity<Map<String, Object>> assignLabelToPayment(
      @PathVariable Long paymentId,
      @PathVariable Long labelId) {
    if (!isValidId(paymentId) || !isValidId(labelId)) return ResponseEntity.badRequest().build();
    Payment payment = validatePaymentExists(paymentId);
    if (payment == null) return ResponseEntity.notFound().build();
    Label label = validateLabelExists(labelId);
    if (label == null) return ResponseEntity.notFound().build();
    if (isLabelAlreadyAssigned(payment, labelId)) return ResponseEntity.status(HttpStatus.CONFLICT).build();
    return performAssignLabel(payment, label, paymentId, labelId);
  }

  @GetMapping("/{paymentId}/labels")
  public ResponseEntity<Map<String, Object>> getLabelsForPayment(@PathVariable Long paymentId) {
    if (!isValidId(paymentId)) return ResponseEntity.badRequest().build();
    Payment payment = validatePaymentExists(paymentId);
    if (payment == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(buildLabelsResponse(payment, paymentId));
  }

  @DeleteMapping("/{paymentId}/labels/{labelId}")
  public ResponseEntity<Void> removeLabelFromPayment(
      @PathVariable Long paymentId,
      @PathVariable Long labelId) {
    if (!isValidId(paymentId) || !isValidId(labelId)) return ResponseEntity.badRequest().build();
    Payment payment = validatePaymentExists(paymentId);
    if (payment == null) return ResponseEntity.notFound().build();
    Label label = validateLabelExists(labelId);
    if (label == null) return ResponseEntity.notFound().build();
    if (!isLabelAssignedToPayment(payment, labelId)) return ResponseEntity.notFound().build();
    return performRemoveLabel(payment, label);
  }

  @DeleteMapping("/{paymentId}/labels")
  public ResponseEntity<Void> removeAllLabelsFromPayment(@PathVariable Long paymentId) {
    if (!isValidId(paymentId)) return ResponseEntity.badRequest().build();
    Payment payment = validatePaymentExists(paymentId);
    if (payment == null) return ResponseEntity.notFound().build();
    return performRemoveAllLabels(payment);
  }

  @PostMapping("/labels/{labelId}/payments")
  public ResponseEntity<Map<String, Object>> assignLabelToMultiplePayments(
      @PathVariable Long labelId,
      @RequestBody Map<String, List<Long>> request) {
    if (!isValidId(labelId)) return ResponseEntity.badRequest().build();
    Label label = validateLabelExists(labelId);
    if (label == null) return ResponseEntity.notFound().build();
    List<Long> paymentIds = extractPaymentIds(request);
    int assignedCount = assignLabelToPayments(paymentIds, label, labelId);
    return ResponseEntity.status(HttpStatus.CREATED).body(buildBulkAssignResponse(labelId, assignedCount));
  }

  @DeleteMapping("/labels/{labelId}/payments")
  public ResponseEntity<Void> removeLabelFromMultiplePayments(
      @PathVariable Long labelId,
      @RequestBody Map<String, List<Long>> request) {
    if (!isValidId(labelId)) return ResponseEntity.badRequest().build();
    Label label = validateLabelExists(labelId);
    if (label == null) return ResponseEntity.notFound().build();
    List<Long> paymentIds = extractPaymentIds(request);
    removeLabelFromPayments(paymentIds, label);
    return ResponseEntity.noContent().build();
  }

  private List<Payment> applyLabelFilter(List<Payment> payments, Long labelId) {
    if (labelId == null) return payments;
    if (!isValidId(labelId)) return null;
    try {
      Label label = labelService.getLabelById(labelId);
      if (label == null) return null;
      return payments.stream()
          .filter(p -> paymentLabelService.getLabelsForPayment(p)
              .stream().anyMatch(l -> l.getId().equals(labelId)))
          .toList();
    } catch (Exception e) {
      return null;
    }
  }

  private List<Payment> applyDateRangeFilter(List<Payment> payments, String startDate, String endDate) {
    if (startDate == null && endDate == null) return payments;
    try {
      LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
      LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
      return payments.stream()
          .filter(p -> isWithinDateRange(p.getPaymentDate(), start, end))
          .toList();
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private boolean isWithinDateRange(LocalDate paymentDate, LocalDate start, LocalDate end) {
    if (start != null && paymentDate.isBefore(start)) return false;
    if (end != null && paymentDate.isAfter(end)) return false;
    return true;
  }

  private List<Payment> filterPaymentsByCounterpartyName(List<Payment> payments, String query) {
    String lowerQuery = query.toLowerCase();
    return payments.stream()
        .filter(p -> p.getCounterpartyName() != null &&
                    p.getCounterpartyName().toLowerCase().contains(lowerQuery))
        .toList();
  }

  private Payment validatePaymentExists(Long paymentId) {
    try {
      return paymentService.getPaymentById(paymentId);
    } catch (Exception e) {
      return null;
    }
  }

  private Label validateLabelExists(Long labelId) {
    try {
      return labelService.getLabelById(labelId);
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isLabelAlreadyAssigned(Payment payment, Long labelId) {
    return paymentLabelService.getLabelsForPayment(payment)
        .stream().anyMatch(l -> l.getId().equals(labelId));
  }

  private boolean isLabelAssignedToPayment(Payment payment, Long labelId) {
    return paymentLabelService.getLabelsForPayment(payment)
        .stream().anyMatch(l -> l.getId().equals(labelId));
  }

  private ResponseEntity<Map<String, Object>> performAssignLabel(Payment payment, Label label, Long paymentId, Long labelId) {
    try {
      paymentLabelService.assignLabelToPayment(payment, label);
      Map<String, Object> response = new HashMap<>();
      response.put("paymentId", paymentId);
      response.put("labelId", labelId);
      response.put("message", "Label assigned successfully");
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  private ResponseEntity<Void> performRemoveLabel(Payment payment, Label label) {
    try {
      paymentLabelService.removeLabelFromPayment(payment, label);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  private ResponseEntity<Void> performRemoveAllLabels(Payment payment) {
    try {
      paymentLabelService.removeAllLabelsFromPayment(payment);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  private int assignLabelToPayments(List<Long> paymentIds, Label label, Long labelId) {
    int count = 0;
    for (Long paymentId : paymentIds) {
      Payment payment = paymentService.getPaymentById(paymentId);
      if (payment != null && !isLabelAlreadyAssigned(payment, labelId)) {
        try {
          paymentLabelService.assignLabelToPayment(payment, label);
          count++;
        } catch (Exception e) {
          // skip on error
        }
      }
    }
    return count;
  }

  private void removeLabelFromPayments(List<Long> paymentIds, Label label) {
    for (Long paymentId : paymentIds) {
      Payment payment = paymentService.getPaymentById(paymentId);
      if (payment != null) {
        try {
          paymentLabelService.removeLabelFromPayment(payment, label);
        } catch (Exception e) {
          // skip on error
        }
      }
    }
  }

  private List<Long> extractPaymentIds(Map<String, List<Long>> request) {
    return request.getOrDefault("paymentIds", new ArrayList<>());
  }

  private boolean isValidId(Long id) {
    return id != null && id > 0;
  }

  private boolean isEmptyQuery(String query) {
    return query == null || query.trim().isEmpty();
  }

  private Map<String, Object> buildPaymentsResponse(List<Payment> payments) {
    List<Map<String, Object>> paymentDtos = payments.stream()
        .map(this::buildPaymentDto)
        .toList();
    Map<String, Object> response = new HashMap<>();
    response.put("payments", paymentDtos);
    response.put("total", paymentDtos.size());
    return response;
  }

  private Map<String, Object> buildLabelsResponse(Payment payment, Long paymentId) {
    List<Label> labels = paymentLabelService.getLabelsForPayment(payment);
    Map<String, Object> response = new HashMap<>();
    response.put("paymentId", paymentId);
    response.put("labels", buildLabelDtos(labels));
    return response;
  }

  private Map<String, Object> buildBulkAssignResponse(Long labelId, int assignedCount) {
    Map<String, Object> response = new HashMap<>();
    response.put("labelId", labelId);
    response.put("assignedCount", assignedCount);
    return response;
  }

  private Map<String, Object> buildPaymentDto(Payment payment) {
    Map<String, Object> dto = new HashMap<>();
    dto.put("id", payment.getId());
    dto.put("paymentDate", payment.getPaymentDate());
    dto.put("amount", payment.getAmount());
    dto.put("currency", payment.getCurrency());
    dto.put("reference", payment.getReference());
    dto.put("transactionType", payment.getTransactionType());
    dto.put("counterpartyName", payment.getCounterpartyName());
    dto.put("accountNumber", payment.getAccountNumber());
    dto.put("labels", buildLabelDtos(paymentLabelService.getLabelsForPayment(payment)));
    return dto;
  }

  private List<Map<String, Object>> buildLabelDtos(List<Label> labels) {
    List<Map<String, Object>> labelDtos = new ArrayList<>();
    for (Label label : labels) {
      labelDtos.add(buildLabelDto(label));
    }
    return labelDtos;
  }

  private Map<String, Object> buildLabelDto(Label label) {
    Map<String, Object> dto = new HashMap<>();
    dto.put("id", label.getId());
    dto.put("name", label.getName());
    dto.put("description", label.getDescription() != null ? label.getDescription() : "");
    return dto;
  }

  private Map<String, Object> buildImportSuccessResponse(CsvImportResult importResult, CsvParseResult parseResult) {
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("newPaymentCount", importResult.getNewPaymentCount());
    response.put("duplicateCount", importResult.getDuplicateCount());
    response.put("totalProcessed", importResult.getTotalProcessed());
    response.put("errors", importResult.getErrors() != null ? importResult.getErrors() : new ArrayList<>());
    response.put("importedAt", importResult.getImportedAt());
    return response;
  }

  private Map<String, Object> buildImportErrorResponse(CsvParseResult parseResult) {
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("newPaymentCount", 0);
    response.put("duplicateCount", parseResult.getSkippedDuplicates());
    response.put("totalProcessed", parseResult.getSkippedDuplicates());
    response.put("errors", parseResult.getErrors() != null ? parseResult.getErrors() : new ArrayList<>());
    response.put("importedAt", LocalDateTime.now());
    return response;
  }
}
