package com.hostel.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.model.Application;
import com.hostel.service.ApplicationService;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;


    // =====================================================
    // STUDENT - CREATE APPLICATION
    // =====================================================

    @PostMapping("/create/{userId}")
    public ResponseEntity<?> createApplication(
            @PathVariable Long userId,
            @RequestBody Application application) {

        try {

            Application savedApplication =
                    applicationService.submitApplication(
                            application,
                            userId
                    );

            return ResponseEntity.ok(
                    savedApplication
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // GET APPLICATION BY ID
    // =====================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getApplication(
            @PathVariable Long id) {

        try {

            return ResponseEntity.ok(
                    applicationService
                            .getApplicationById(id)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // GET STUDENT APPLICATIONS
    // =====================================================

    @GetMapping("/student/{userId}")
    public ResponseEntity<?> getStudentApplications(
            @PathVariable Long userId) {

        try {

            return ResponseEntity.ok(
                    applicationService
                            .getStudentApplications(userId)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ADMIN - GET ALL APPLICATIONS
    // =====================================================

    @GetMapping
    public ResponseEntity<List<Application>>
            getAllApplications() {

        return ResponseEntity.ok(
                applicationService
                        .getAllApplications()
        );
    }


    // =====================================================
    // ADMIN - GET APPLICATIONS BY STATUS
    // =====================================================

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getApplicationsByStatus(
            @PathVariable String status) {

        try {

            return ResponseEntity.ok(
                    applicationService
                            .getApplicationsByStatus(status)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ADMIN - APPROVE APPLICATION
    // =====================================================

    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveApplication(
            @PathVariable Long id) {

        try {

            Application application =
                    applicationService
                            .approveApplication(id);

            return ResponseEntity.ok(
                    application
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ADMIN - REJECT APPLICATION
    // =====================================================

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectApplication(
            @PathVariable Long id,
            @RequestParam String reason) {

        try {

            Application application =
                    applicationService
                            .rejectApplication(
                                    id,
                                    reason
                            );

            return ResponseEntity.ok(
                    application
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ADMIN - RESET TO PENDING
    // =====================================================

    @PutMapping("/reset/{id}")
    public ResponseEntity<?> resetToPending(
            @PathVariable Long id) {

        try {

            Application application =
                    applicationService
                            .resetToPending(id);

            return ResponseEntity.ok(
                    application
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // DELETE APPLICATION
    // =====================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApplication(
            @PathVariable Long id) {

        try {

            applicationService
                    .deleteApplication(id);

            return ResponseEntity.ok(
                    "Application deleted successfully"
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}