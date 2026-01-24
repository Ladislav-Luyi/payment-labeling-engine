package com.paymentlabeling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for dashboard and home page
 */
@Controller
public class DashboardController {

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard/index";
    }

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("pageTitle", "Home");
        return "home";
    }
}
