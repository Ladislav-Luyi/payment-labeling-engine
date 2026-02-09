package com.paymentlabeling.controller;

import com.paymentlabeling.model.Aggregate;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.service.AggregateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Web Controller for Aggregate & Reporting pages
 * Handles display and management of aggregates grouped by label sets
 */
@Controller
@RequestMapping("/aggregates")
public class AggregateWebController {

    @Autowired
    private AggregateService aggregateService;

    /**
     * GET /aggregates - Display aggregates page with filtering options
     */
    @GetMapping
    public String listAggregates(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) Integer periodEndDay,
            Model model) {

        List<Aggregate> aggregates;

        if (year != null && month != null) {
            aggregates = aggregateService.getAggregatesByYearAndMonthAndPeriodEndDay(year, month, periodEndDay);
        } else if (year != null) {
            aggregates = aggregateService.getAggregatesByYearAndPeriodEndDay(year, periodEndDay);
        } else if (periodEndDay != null) {
            aggregates = aggregateService.getAggregatesByPeriodEndDay(periodEndDay);
        } else if (labelId != null) {
            aggregates = aggregateService.getAggregatesByLabel(labelId);
        } else {
            aggregates = aggregateService.getAllAggregates();
        }

        if (labelId != null) {
            aggregates = aggregates.stream()
                    .filter(a -> a.getLabels().stream().anyMatch(l -> l.getId().equals(labelId)))
                    .toList();
        }

        model.addAttribute("pageTitle", "Aggregates");
        model.addAttribute("aggregates", aggregates);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("labelId", labelId);
        model.addAttribute("periodEndDay", periodEndDay);

        return "aggregates/index";
    }

    /**
     * GET /aggregates/:id - Display aggregate details with expandable payments
     */
    @GetMapping("/{id}")
    public String viewAggregate(@PathVariable Long id, Model model) {
        return aggregateService.getAggregateById(id)
                .map(aggregate -> {
                    List<Payment> payments = aggregateService.getPaymentsForAggregate(id);
                    model.addAttribute("aggregate", aggregate);
                    model.addAttribute("payments", payments);
                    model.addAttribute("pageTitle", "Aggregate Details");
                    return "aggregates/details";
                })
                .orElseThrow(() -> new RuntimeException("Aggregate not found with ID: " + id));
    }

    /**
     * POST /aggregates/recalculate - Recalculate all aggregates from payment data
     */
    @PostMapping("/recalculate")
    public String recalculateAggregates(
            @RequestParam(required = false) Integer periodEndDay,
            RedirectAttributes redirectAttributes) {
        try {
            aggregateService.recalculateAggregates(periodEndDay);
            redirectAttributes.addFlashAttribute("success", "Aggregates recalculated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to recalculate aggregates: " + e.getMessage());
        }
        return "redirect:/aggregates";
    }

    /**
     * GET /aggregates/add - Display add aggregate form (kept for potential future use)
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("pageTitle", "Manual Aggregate Creation");
        return "aggregates/add";
    }

    /**
     * POST /aggregates - Create new aggregate manually
     */
    @PostMapping
    public String createAggregate(
            @RequestParam Integer year,
            @RequestParam Integer month,
            RedirectAttributes redirectAttributes) {

        try {
            // Aggregates are now calculated automatically from payment labels
            // This endpoint is kept for potential manual creation if needed
            redirectAttributes.addFlashAttribute("info", "Aggregates are calculated automatically from payment labels. Use 'Recalculate' to refresh.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create aggregate: " + e.getMessage());
        }

        return "redirect:/aggregates";
    }
}