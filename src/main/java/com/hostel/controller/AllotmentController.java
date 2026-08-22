package com.hostel.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.model.Allotment;
import com.hostel.service.AllotmentService;

@RestController
@RequestMapping("/api/allotment")
@CrossOrigin
public class AllotmentController {

    @Autowired
    private AllotmentService allotmentService;

    // =====================================================
    // GENERATE ALLOTMENT LIST
    // =====================================================

    @PostMapping("/generate")
    public ResponseEntity<?> generateAllotment(
            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            List<Allotment> allotments =
                    allotmentService.generateAllotment(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(allotments);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // GET ALLOTMENT LIST
    // =====================================================

    @GetMapping
    public ResponseEntity<?> getAllotment(
            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            List<Allotment> allotments =
                    allotmentService.getAllotment(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(allotments);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // STUDENT - VIEW MY ALLOTMENT
    // =====================================================

    @GetMapping("/student/{userId}")
    public ResponseEntity<?> getStudentAllotments(
            @PathVariable Long userId) {

        try {

            List<Allotment> allotments =
                    allotmentService.getStudentAllotments(userId);

            return ResponseEntity.ok(allotments);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // ACCEPT SEAT
    // =====================================================

    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptSeat(
            @PathVariable Long id) {

        try {

            Allotment allotment =
                    allotmentService.acceptSeat(id);

            return ResponseEntity.ok(allotment);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // REJECT SEAT
    // =====================================================

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectSeat(
            @PathVariable Long id) {

        try {

            Allotment allotment =
                    allotmentService.rejectSeat(id);

            return ResponseEntity.ok(allotment);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}