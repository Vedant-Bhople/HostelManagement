package com.hostel.controller;

import java.util.List;
import java.util.Map;

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
    // STAGE 1: GENERATE NORMAL ALLOTMENT LIST
    // =====================================================

    @PostMapping("/generate")
    public ResponseEntity<?> generateAllotment(
            @RequestParam String gender,
            @RequestParam(required = false, defaultValue = "ALL") String branch,
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
    // STAGE 2: CONVERT UNUSED RESERVED SEATS TO OPEN
    // =====================================================

    @PostMapping("/convert-reserved")
    public ResponseEntity<?> convertReservedSeats(
            @RequestParam String gender,
            @RequestParam(required = false, defaultValue = "ALL") String branch,
            @RequestParam String year) {

        try {
            List<Allotment> allotments =
                    allotmentService.convertUnusedReservedSeats(
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
    // GET ALLOTMENT SUMMARY & QUOTA BREAKDOWN
    // =====================================================

    @GetMapping("/summary")
    public ResponseEntity<?> getAllotmentSummary(
            @RequestParam String gender,
            @RequestParam(required = false, defaultValue = "ALL") String branch,
            @RequestParam String year) {

        try {
            Map<String, Object> summary =
                    allotmentService.getAllotmentSummary(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(summary);

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
            @RequestParam(required = false, defaultValue = "ALL") String branch,
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

    // =====================================================
    // STAGE 3: SPOT ROUND - GET SUMMARY & VACANT POOL
    // =====================================================

    @GetMapping("/spot/summary")
    public ResponseEntity<?> getSpotRoundSummary(
            @RequestParam String gender) {

        try {
            Map<String, Object> summary =
                    allotmentService.getSpotRoundSummary(gender);

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // STAGE 3: SPOT ROUND - GET ELIGIBLE CANDIDATES QUEUE
    // =====================================================

    @GetMapping("/spot/eligible")
    public ResponseEntity<?> getEligibleSpotApplicants(
            @RequestParam String gender) {

        try {
            List<Map<String, Object>> list =
                    allotmentService.getEligibleSpotApplicants(gender);

            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // STAGE 3: SPOT ROUND - GET SPOT ALLOTMENTS LIST
    // =====================================================

    @GetMapping("/spot")
    public ResponseEntity<?> getSpotAllotments(
            @RequestParam String gender) {

        try {
            List<Allotment> list =
                    allotmentService.getSpotAllotments(gender);

            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // STAGE 3: SPOT ROUND - GENERATE COMMON POOL ALLOTMENT
    // =====================================================

    @PostMapping("/spot/generate")
    public ResponseEntity<?> generateSpotRoundAllotment(
            @RequestParam String gender) {

        try {
            List<Allotment> allotments =
                    allotmentService.generateSpotRoundAllotment(gender);

            return ResponseEntity.ok(allotments);

        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // STAGE 3: SPOT ROUND - ALLOT SINGLE CANDIDATE (OPTION C)
    // =====================================================

    @PostMapping("/spot/allot-single")
    public ResponseEntity<?> allotSingleSpotCandidate(
            @RequestParam Long applicationId) {

        try {
            Allotment allotment =
                    allotmentService.allotSingleSpotCandidate(applicationId);

            return ResponseEntity.ok(allotment);

        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}