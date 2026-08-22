package com.hostel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.dto.StudentDashboardDTO;
import com.hostel.service.StudentDashboardService;

@RestController
@RequestMapping("/api/student/dashboard")
@CrossOrigin
public class StudentDashboardController {

    @Autowired
    private StudentDashboardService studentDashboardService;


    // =====================================================
    // STUDENT DASHBOARD
    // =====================================================

    @GetMapping("/{userId}")
    public ResponseEntity<?> getStudentDashboard(
            @PathVariable Long userId) {

        try {

            StudentDashboardDTO dashboard =
                    studentDashboardService
                    .getStudentDashboard(userId);

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}