package com.paymentlabeling.controller;

import com.paymentlabeling.model.Aggregate;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.service.AggregateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST API Controller for Aggregate & Reporting endpoints
 * Provides endpoints for retrieving, filtering, and reporting on aggregated payment data
 * Aggregates now represent groups of payments with the same label set for a given month/year
 */
@RestController
@RequestMapping("/api/aggregates")
public class AggregateController {

    @Autowired
    private AggregateService aggregateService;

    /**
     * GET /api/aggregates - Retrieve aggregates with optional filtering
     * Query parameters:
     *   - year: Filter by year
     *   - month: Filter by month (requires year)
     *   - labelId: Filter by aggregates containing this label
     */
    @GetMapping
    public ResponseEntity<List<Aggregate>> getAggregates(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long labelId) {

        if (month != null && year == null) {
            return ResponseEntity.badRequest().build();
        }

        if (month != null && (month < 1 || month > 12)) {
            return ResponseEntity.badRequest().build();
        }

        List<Aggregate> aggregates;

        if (year != null && month != null) {
            aggregates = aggregateService.getAggregatesByYearAndMonth(year, month);
            if (labelId != null) {
                aggregates = aggregates.stream()
                    .filter(a -> a.getLabels().stream().anyMatch(l -> l.getId().equals(labelId)))
                    .toList();
            }
        } else if (year != null) {
            aggregates = aggregateService.getAggregatesByYear(year);
            if (labelId != null) {
                aggregates = aggregates.stream()
                    .filter(a -> a.getLabels().stream().anyMatch(l -> l.getId().equals(labelId)))
                    .toList();
            }
        } else if (labelId != null) {
            aggregates = aggregateService.getAggregatesByLabel(labelId);
        } else {
            aggregates = aggregateService.getAllAggregates();
        }

        return ResponseEntity.ok(aggregates);
    }

    /**
     * GET /api/aggregates/{id} - Get aggregate by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Aggregate> getAggregateById(@PathVariable Long id) {
        Optional<Aggregate> aggregate = aggregateService.getAggregateById(id);
        return aggregate.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/aggregates/{id}/payments - Get all payments for a specific aggregate
     */
    @GetMapping("/{id}/payments")
    public ResponseEntity<List<Payment>> getPaymentsForAggregate(@PathVariable Long id) {
        List<Payment> payments = aggregateService.getPaymentsForAggregate(id);
        return ResponseEntity.ok(payments);
    }

    /**
     * POST /api/aggregates/recalculate - Recalculate all aggregates from payment data
     */
    @PostMapping("/recalculate")
    public ResponseEntity<String> recalculateAggregates() {
        try {
            aggregateService.recalculateAggregates();
            return ResponseEntity.ok("{\"message\": \"Aggregates recalculated successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * POST /api/aggregates - Create a new aggregate
     */
    @PostMapping
    public ResponseEntity<Aggregate> createAggregate(@RequestBody Aggregate aggregate) {
        Aggregate saved = aggregateService.saveAggregate(aggregate);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /api/aggregates/{id} - Update an existing aggregate
     */
    @PutMapping("/{id}")
    public ResponseEntity<Aggregate> updateAggregate(
            @PathVariable Long id,
            @RequestBody Aggregate aggregateDetails) {
        Optional<Aggregate> existing = aggregateService.getAggregateById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Aggregate aggregate = existing.get();
        if (aggregateDetails.getTotalAmount() != null) {
            aggregate.setTotalAmount(aggregateDetails.getTotalAmount());
        }
        if (aggregateDetails.getTransactionCount() != null) {
            aggregate.setTransactionCount(aggregateDetails.getTransactionCount());
        }

        Aggregate updated = aggregateService.saveAggregate(aggregate);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/aggregates/{id} - Delete an aggregate
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAggregate(@PathVariable Long id) {
        Optional<Aggregate> existing = aggregateService.getAggregateById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        aggregateService.deleteAggregate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/aggregates/summary/monthly - Get monthly summary (aggregates by month)
     * Query parameters:
     *   - labelId: Filter by label ID (optional)
     *   - year: Filter by year (optional)
     */
    @GetMapping("/summary/monthly")
    public ResponseEntity<List<Aggregate>> getMonthlySummary(
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) Integer year) {

        List<Aggregate> aggregates;

        if (labelId != null) {
            aggregates = aggregateService.getAggregatesByLabel(labelId);
            if (year != null) {
                aggregates = aggregates.stream()
                    .filter(a -> a.getYear().equals(year))
                    .toList();
            }
        } else if (year != null) {
            aggregates = aggregateService.getAggregatesByYear(year);
        } else {
            aggregates = aggregateService.getAllAggregates();
        }

        return ResponseEntity.ok(aggregates);
    }

    /**
     * GET /api/aggregates/export/csv - Export aggregates as CSV
     * Query parameters:
     *   - year: Filter by year (optional)
     *   - month: Filter by month (optional)
     */
    @GetMapping("/export/csv")
    public ResponseEntity<String> exportAsCSV(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        List<Aggregate> aggregates;

        if (year != null && month != null) {
            aggregates = aggregateService.getAggregatesByYearAndMonth(year, month);
        } else if (year != null) {
            aggregates = aggregateService.getAggregatesByYear(year);
        } else {
            aggregates = aggregateService.getAllAggregates();
        }

        // Build CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Labels,Year,Month,Total Amount,Transaction Count\n");

        for (Aggregate agg : aggregates) {
            csv.append(agg.getId()).append(",");
            csv.append("\"").append(agg.getLabelNamesAsString()).append("\"").append(",");
            csv.append(agg.getYear()).append(",");
            csv.append(agg.getMonth()).append(",");
            csv.append(agg.getTotalAmount()).append(",");
            csv.append(agg.getTransactionCount()).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=aggregates.csv")
                .header("Content-Type", "text/csv")
                .body(csv.toString());
    }
}
