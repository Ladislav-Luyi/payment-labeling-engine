package com.paymentlabeling.controller;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.Payment;
import com.paymentlabeling.service.PaymentLabelService;
import com.paymentlabeling.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * GET /payments - Display payments page
     */
    @GetMapping
    public String listPayments(
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        List<Payment> payments = paymentService.getAllPayments();

        // Basic filtering, can be enhanced
        if (labelId != null) {
            payments = payments.stream()
                    .filter(p -> paymentLabelService.getLabelsForPayment(p).stream().anyMatch(l -> l.getId().equals(labelId)))
                    .toList();
        }

        // Add labels to each payment for the view
        List<PaymentWithLabels> paymentsWithLabels = payments.stream()
                .map(p -> new PaymentWithLabels(p, paymentLabelService.getLabelsForPayment(p)))
                .toList();

        model.addAttribute("pageTitle", "Payments");
        model.addAttribute("payments", paymentsWithLabels);
        model.addAttribute("labelId", labelId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

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