package com.hostel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.dto.EmailRequestDTO;
import com.hostel.service.EmailService;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailController {

    @Autowired
    private EmailService emailService;

    // =====================================================
    // SEND CUSTOM / MANUAL EMAIL
    // =====================================================

    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestBody EmailRequestDTO request) {
        if (request == null || request.getTo() == null || request.getTo().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Recipient email address is required.");
        }

        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email subject is required.");
        }

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email message content is required.");
        }

        boolean sent = emailService.sendEmail(
                request.getTo().trim(),
                request.getSubject().trim(),
                request.getMessage().trim()
        );

        if (sent) {
            return ResponseEntity.ok("Email dispatched successfully to " + request.getTo());
        } else {
            return ResponseEntity.ok("Email queued / attempted. (Check server logs if SMTP is offline).");
        }
    }
}
