package com.paymentlabeling.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check controller for debugging
 */
@RestController
public class HealthCheckController {

    @GetMapping("/api/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/api/test")
    public String test() {
        return "{\"status\": \"application running\"}";
    }
}

