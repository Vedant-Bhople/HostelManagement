package com.hostel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hostel.model.User;
import com.hostel.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Value("${INITIAL_ADMIN_EMAIL:}")
    private String initialAdminEmail;

    @Value("${INITIAL_ADMIN_PASSWORD:}")
    private String initialAdminPassword;

    @Value("${INITIAL_ADMIN_NAME:Hostel Administrator}")
    private String initialAdminName;

    @Override
    public void run(String... args) {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        try {
            // 1. Check whether an ADMIN already exists in the database
            boolean adminExists = userRepository.existsByRole("ADMIN");

            if (adminExists) {
                logger.info("Admin account already exists in database. Initial admin creation skipped.");
                return;
            }

            // 2. Validate environment variables (no hardcoded password fallback)
            if (initialAdminEmail == null || initialAdminEmail.trim().isEmpty() ||
                initialAdminPassword == null || initialAdminPassword.trim().isEmpty()) {
                logger.warn("INITIAL_ADMIN_EMAIL or INITIAL_ADMIN_PASSWORD environment variable is not configured. Initial admin creation skipped.");
                return;
            }

            String email = initialAdminEmail.trim();

            // 3. Check if a user with this email already exists
            User existingUser = userRepository.findByEmail(email);
            if (existingUser != null) {
                logger.warn("A user with email '{}' already exists with role '{}'. Skipping initial admin creation.", email, existingUser.getRole());
                return;
            }

            // 4. Create exactly one initial admin account
            User admin = new User();
            admin.setName(initialAdminName != null && !initialAdminName.trim().isEmpty()
                    ? initialAdminName.trim()
                    : "Hostel Administrator");
            admin.setEmail(email);
            admin.setPassword(initialAdminPassword.trim());
            admin.setRole("ADMIN");

            userRepository.save(admin);
            logger.info("Initial admin account created successfully with configured email: {}", email);

        } catch (Exception e) {
            logger.error("Error during initial admin initialization: {}", e.getMessage(), e);
        }
    }
}
