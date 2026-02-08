package com.paymentlabeling.controller;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.service.LabelService;
import com.paymentlabeling.service.PaymentLabelService;
import com.paymentlabeling.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Web Controller for Payment pages
 */
@Controller
@RequestMapping("/payments")
public class PaymentWebController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentLabelService paymentLabelService;

    @Autowired
    private LabelService labelService;

    /**
     * GET /payments - Display payments page
     */
    @GetMapping
    public String listPayments(
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "all") String markFilter,
            Model model) {

        List<Payment> payments = paymentService.getAllPayments();

        // Add labels to each payment for the view once.
        List<PaymentWithLabels> paymentsWithLabels = payments.stream()
                .map(p -> new PaymentWithLabels(p, paymentLabelService.getLabelsForPayment(p)))
                .toList();

        if (labelId != null) {
            paymentsWithLabels = paymentsWithLabels.stream()
                    .filter(p -> p.getLabels().stream().anyMatch(l -> l.getId().equals(labelId)))
                    .toList();
        }

        if ("marked".equalsIgnoreCase(markFilter)) {
            paymentsWithLabels = paymentsWithLabels.stream()
                    .filter(p -> !p.getLabels().isEmpty())
                    .toList();
        } else if ("unmarked".equalsIgnoreCase(markFilter)) {
            paymentsWithLabels = paymentsWithLabels.stream()
                    .filter(p -> p.getLabels().isEmpty())
                    .toList();
        }

        // Get all available labels for the assignment dropdown
        List<Label> allLabels = labelService.getAllLabels();

        model.addAttribute("pageTitle", "Payments");
        model.addAttribute("payments", paymentsWithLabels);
        model.addAttribute("allLabels", allLabels);
        model.addAttribute("labelId", labelId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("markFilter", markFilter);

        return "payments/index";
    }

    /**
     * GET /payments/upload - Display upload page
     */
    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("pageTitle", "Upload Payments");
        return "payments/upload";
    }

    // Helper class to hold payment with its labels
    public static class PaymentWithLabels {
        private final Payment payment;
        private final List<Label> labels;

        public PaymentWithLabels(Payment payment, List<Label> labels) {
            this.payment = payment;
            this.labels = labels;
        }

        public Payment getPayment() {
            return payment;
        }

        public List<Label> getLabels() {
            return labels;
        }
    }
}
