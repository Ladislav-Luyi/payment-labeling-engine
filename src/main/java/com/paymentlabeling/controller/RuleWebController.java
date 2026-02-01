package com.paymentlabeling.controller;

import com.paymentlabeling.model.Label;
import com.paymentlabeling.service.LabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Web Controller for Labeling Rules pages
 */
@Controller
@RequestMapping("/rules")
public class RuleWebController {

    @Autowired
    private LabelService labelService;

    /**
     * GET /rules - Display labeling rules page
     */
    @GetMapping
    public String listRules(Model model) {
        List<Label> labels = labelService.getAllLabels();

        model.addAttribute("pageTitle", "Labeling Rules");
        model.addAttribute("labels", labels);

        return "rules/index";
    }
}