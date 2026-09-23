package com.hostel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.model.User;
import com.hostel.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

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

    // Admin Registration
    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody User user) {

        try {

            User savedUser = userService.registerAdmin(user);

            return ResponseEntity.ok(savedUser);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String password,
            @RequestBody(required = false) java.util.Map<String, String> body) {

        try {
            String loginEmail = (email != null && !email.trim().isEmpty()) 
                    ? email.trim() 
                    : (body != null ? body.get("email") : null);

            String loginPassword = (password != null && !password.trim().isEmpty()) 
                    ? password.trim() 
                    : (body != null ? body.get("password") : null);

            if (loginEmail == null || loginPassword == null) {
                return ResponseEntity.badRequest()
                        .body("Email and password are required");
            }

            User user = userService.login(loginEmail, loginPassword);

            return ResponseEntity.ok(user);

        } catch (Exception e) {

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