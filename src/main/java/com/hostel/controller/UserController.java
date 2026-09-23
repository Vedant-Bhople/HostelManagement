package com.hostel.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.model.User;
import com.hostel.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // Student Registration
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        try {

            User savedUser = userService.registerStudent(user);

            return ResponseEntity.ok(savedUser);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam String email,
            @RequestParam String password) {

        try {
            logger.info("LOGIN DEBUG email received='{}'", email);

            String loginEmail = email != null ? email.trim() : "";
            String loginPassword = password != null ? password : "";

            logger.info("LOGIN DEBUG trimmed email='{}'", loginEmail);

            User user = userService.login(loginEmail, loginPassword);

            logger.info("LOGIN DEBUG user found={}, role={}", user != null, user != null ? user.getRole() : null);

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            String sanitizedEmail = email != null ? email.trim() : "null";
            logger.warn("LOGIN DEBUG failed for email='{}': {}", sanitizedEmail, e.getMessage());

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // Get User
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {

        try {

            return ResponseEntity.ok(
                    userService.getUserById(id)
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}