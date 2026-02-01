package com.paymentlabeling.controller;

import com.paymentlabeling.model.Aggregate;
import com.paymentlabeling.model.Label;
import com.paymentlabeling.service.AggregateService;
import com.paymentlabeling.service.LabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Web Controller for Aggregate & Reporting pages
 */
@Controller
@RequestMapping("/aggregates")
public class AggregateWebController {

    @Autowired
    private AggregateService aggregateService;

    @Autowired
    private LabelService labelService;

    /**
     * GET /aggregates - Display aggregates page
     */
    @GetMapping
    public String listAggregates(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long labelId,
            Model model) {

        // Similar logic to REST controller, but for web
        List<Aggregate> aggregates;

        if (year != null && month != null && labelId != null) {
            aggregates = aggregateService.getAggregateByLabelYearMonth(labelId, year, month)
                    .map(List::of).orElse(List.of());
        } else if (year != null && month != null) {
            aggregates = aggregateService.getAggregatesByYearAndMonth(year, month);
        } else if (year != null) {
            aggregates = aggregateService.getAggregatesByYear(year);
        } else if (labelId != null) {
            aggregates = aggregateService.getAggregatesByLabel(labelId);
        } else {
            aggregates = aggregateService.getAllAggregates();
        }

        model.addAttribute("pageTitle", "Aggregates");
        model.addAttribute("aggregates", aggregates);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("labelId", labelId);

        return "aggregates/index";
    }

    /**
     * GET /aggregates/add - Display add aggregate form
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        List<Label> labels = labelService.getAllLabels();
        model.addAttribute("pageTitle", "Add Aggregate");
        model.addAttribute("labels", labels);
        return "aggregates/add";
    }

    /**
     * POST /aggregates - Create new aggregate
     */
    @PostMapping
    public String createAggregate(
            @RequestParam Long labelId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam BigDecimal totalAmount,
            @RequestParam Long transactionCount,
            RedirectAttributes redirectAttributes) {

        try {
            Label label = labelService.getLabelById(labelId);
            if (label == null) {
                redirectAttributes.addFlashAttribute("error", "Invalid label selected.");
                return "redirect:/aggregates/add";
            }

            Aggregate aggregate = new Aggregate();
            aggregate.setLabel(label);
            aggregate.setYear(year);
            aggregate.setMonth(month);
            aggregate.setTotalAmount(totalAmount);
            aggregate.setTransactionCount(transactionCount);

            aggregateService.saveAggregate(aggregate);
            redirectAttributes.addFlashAttribute("success", "Aggregate created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create aggregate: " + e.getMessage());
            return "redirect:/aggregates/add";
        }

        return "redirect:/aggregates";
    }
}