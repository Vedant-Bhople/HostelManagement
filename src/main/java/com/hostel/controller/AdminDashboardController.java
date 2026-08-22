package com.hostel.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
@CrossOrigin
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService dashboardService;


    // =====================================================
    // ADMIN DASHBOARD SUMMARY
    // =====================================================

    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary() {

        try {

            Map<String, Object> dashboard =
                    dashboardService
                    .getDashboardSummary();

            return ResponseEntity.ok(
                    dashboard
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}