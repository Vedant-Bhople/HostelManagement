package com.hostel.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.model.HostelSeat;
import com.hostel.service.HostelSeatService;

@RestController
@RequestMapping("/api/seats")
@CrossOrigin
public class HostelSeatController {

    @Autowired
    private HostelSeatService hostelSeatService;


    // =====================================================
    // GENERATE 210 SEATS
    // =====================================================

    @PostMapping("/generate")
    public ResponseEntity<?> generateSeats() {

        try {

            return ResponseEntity.ok(
                    hostelSeatService.generateSeats()
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ALL SEATS
    // =====================================================

    @GetMapping
    public ResponseEntity<List<HostelSeat>> getAllSeats() {

        return ResponseEntity.ok(
                hostelSeatService.getAllSeats()
        );
    }


    // =====================================================
    // BOYS
    // =====================================================

    @GetMapping("/boys")
    public ResponseEntity<List<HostelSeat>> getBoysSeats() {

        return ResponseEntity.ok(
                hostelSeatService.getBoysSeats()
        );
    }


    // =====================================================
    // GIRLS
    // =====================================================

    @GetMapping("/girls")
    public ResponseEntity<List<HostelSeat>> getGirlsSeats() {

        return ResponseEntity.ok(
                hostelSeatService.getGirlsSeats()
        );
    }


    // =====================================================
    // AVAILABLE SEATS
    // =====================================================

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableSeats(

            @RequestParam String hostelType,

            @RequestParam String branch,

            @RequestParam String year) {

        try {

            return ResponseEntity.ok(
                    hostelSeatService.getAvailableSeats(
                            hostelType,
                            branch,
                            year
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}