package com.paymentlabeling.controller;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.model.LabelingRule;
import com.paymentlabeling.service.LabelService;
import com.paymentlabeling.service.LabelingRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Web Controller for Labeling Rules pages
 */
@Controller
@RequestMapping("/rules")
public class RuleWebController {

    private static final Logger logger = LoggerFactory.getLogger(RuleWebController.class);

    @Autowired
    private LabelService labelService;

    @Autowired
    private LabelingRuleService labelingRuleService;

    /**
     * GET /rules - Display labeling rules page
     */
    @GetMapping
    public String listRules(Model model) {
        logger.info("Accessing /rules page");

        try {
            logger.debug("LabelService instance: {}", labelService);
            logger.debug("LabelingRuleService instance: {}", labelingRuleService);

            // Get labels with null safety
            List<Label> labels = new ArrayList<>();
            try {
                List<Label> fetchedLabels = labelService.getAllLabels();
                if (fetchedLabels != null) {
                    labels = fetchedLabels;
                    logger.debug("Loaded {} labels", labels.size());
                } else {
                    logger.warn("LabelService.getAllLabels() returned null");
                }
            } catch (Exception e) {
                logger.error("Error fetching labels", e);
                labels = new ArrayList<>();
            }

            // Get rules with null safety
            List<LabelingRule> rules = new ArrayList<>();
            try {
                List<LabelingRule> fetchedRules = labelingRuleService.getAllRules();
                if (fetchedRules != null) {
                    rules = fetchedRules;
                    logger.debug("Loaded {} rules", rules.size());
                } else {
                    logger.warn("LabelingRuleService.getAllRules() returned null");
                }
            } catch (Exception e) {
                logger.error("Error fetching rules", e);
                rules = new ArrayList<>();
            }

            // Available payment fields for matching
            List<Map<String, String>> paymentFields = List.of(
                Map.of("value", "counterpartyName", "label", "Counterparty Name"),
                Map.of("value", "reference", "label", "Reference"),
                Map.of("value", "transactionType", "label", "Transaction Type"),
                Map.of("value", "counterpartyBank", "label", "Counterparty Bank"),
                Map.of("value", "counterpartyAccount", "label", "Counterparty Account"),
                Map.of("value", "receiverInfo", "label", "Receiver Info"),
                Map.of("value", "additionalInfo", "label", "Additional Info")
            );

            logger.debug("Adding {} payment fields to model", paymentFields.size());

            model.addAttribute("pageTitle", "Labeling Rules");
            model.addAttribute("labels", labels);
            model.addAttribute("rules", rules);
            model.addAttribute("paymentFields", paymentFields);

            logger.info("Successfully loaded /rules page with {} labels and {} rules", labels.size(), rules.size());
            return "rules/index";

        } catch (Exception e) {
            logger.error("Unexpected error in /rules page", e);
            e.printStackTrace();
            // Add empty lists to model to prevent template errors
            model.addAttribute("pageTitle", "Labeling Rules");
            model.addAttribute("labels", new ArrayList<>());
            model.addAttribute("rules", new ArrayList<>());
            model.addAttribute("paymentFields", List.of(
                Map.of("value", "counterpartyName", "label", "Counterparty Name"),
                Map.of("value", "reference", "label", "Reference"),
                Map.of("value", "transactionType", "label", "Transaction Type"),
                Map.of("value", "counterpartyBank", "label", "Counterparty Bank"),
                Map.of("value", "counterpartyAccount", "label", "Counterparty Account"),
                Map.of("value", "receiverInfo", "label", "Receiver Info"),
                Map.of("value", "additionalInfo", "label", "Additional Info")
            ));
            model.addAttribute("error", "Error loading page: " + e.getMessage());
            // Return the template anyway with error message
            return "rules/index";
        }
    }
}