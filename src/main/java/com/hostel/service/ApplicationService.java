package com.hostel.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.model.Allotment;
import com.hostel.model.Application;
import com.hostel.model.Document;
import com.hostel.model.MeritList;
import com.hostel.model.User;
import com.hostel.repository.AllotmentRepository;
import com.hostel.repository.ApplicationRepository;
import com.hostel.repository.DocumentRepository;
import com.hostel.repository.MeritListRepository;
import com.hostel.repository.UserRepository;

@Service
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AllotmentRepository allotmentRepository;

    @Autowired
    private MeritListRepository meritListRepository;

    @Autowired
    private EmailService emailService;


    // =====================================================
    // STUDENT - SUBMIT APPLICATION
    // =====================================================

    public Application submitApplication(
            Application application,
            Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Student not found"
                        )
                );

        application.setUser(user);

        // Validate Category
        if (application.getCategory() == null || application.getCategory().trim().isEmpty()) {
            throw new RuntimeException("Category is required. Allowed categories: OPEN, OBC, SC, ST, VJNT, NT, SEBC");
        }

        String inputCategory = application.getCategory().trim().toUpperCase();
        if ("EWS".equals(inputCategory)) {
            throw new RuntimeException("Invalid category: EWS is not accepted. Allowed categories: OPEN, OBC, SC, ST, VJNT, NT, SEBC");
        }

        if (!com.hostel.service.reservation.CategoryNormalizer.isValidCategory(inputCategory)) {
            throw new RuntimeException("Invalid category: '" + application.getCategory() + "'. Allowed categories: OPEN, OBC, SC, ST, VJNT, NT, SEBC");
        }
        application.setCategory(inputCategory);

        // Calculate Semester 1 percentage
        if (application.getSem1Obtained() != null
                && application.getSem1Total() != null
                && application.getSem1Total() > 0) {

            double percentage =
                    (application.getSem1Obtained()
                    / application.getSem1Total()) * 100;

            application.setSem1Percentage(percentage);
        }

        // Calculate Semester 2 percentage
        if (application.getSem2Obtained() != null
                && application.getSem2Total() != null
                && application.getSem2Total() > 0) {

            double percentage =
                    (application.getSem2Obtained()
                    / application.getSem2Total()) * 100;

            application.setSem2Percentage(percentage);
        }

        // Calculate aggregate
        calculateAggregate(application);

        // New application is PENDING
        application.setStatus("PENDING");

        // Clear old rejection reason
        application.setRejectionReason(null);

        return applicationRepository.save(application);
    }


    // =====================================================
    // CALCULATE AGGREGATE
    // =====================================================

    private void calculateAggregate(
            Application application) {

        Double sem1 =
                application.getSem1Percentage();

        Double sem2 =
                application.getSem2Percentage();

        if (sem1 != null && sem2 != null) {

            double aggregate =
                    (sem1 + sem2) / 2;

            application.setAggregate(aggregate);

        } else if (sem1 != null) {

            application.setAggregate(sem1);

        } else if (sem2 != null) {

            application.setAggregate(sem2);

        } else {

            application.setAggregate(0.0);
        }
    }


    // =====================================================
    // GET APPLICATION BY ID
    // =====================================================

    public Application getApplicationById(
            Long id) {

        return applicationRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Application not found"
                        )
                );
    }


    // =====================================================
    // GET ALL APPLICATIONS
    // =====================================================

    public List<Application> getAllApplications() {

        return applicationRepository.findAll();
    }


    // =====================================================
    // GET APPLICATIONS BY STATUS
    // =====================================================

    public List<Application> getApplicationsByStatus(
            String status) {

        return applicationRepository
                .findByStatus(status);
    }


    // =====================================================
    // GET STUDENT APPLICATIONS
    // =====================================================

    public List<Application> getStudentApplications(
            Long userId) {

        User user =
                userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Student not found"
                        )
                );

        return applicationRepository
                .findByUser(user);
    }


    // =====================================================
    // ADMIN - APPROVE APPLICATION
    // =====================================================

    public Application approveApplication(
            Long applicationId) {

        Application application =
                applicationRepository
                .findById(applicationId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Application not found"
                        )
                );

        // Already rejected
        if ("REJECTED".equalsIgnoreCase(
                application.getStatus())) {

            throw new RuntimeException(
                    "Rejected application cannot be approved"
            );
        }

        // Approve application
        application.setStatus("APPROVED");

        // Clear rejection reason
        application.setRejectionReason(null);

        Application saved = applicationRepository.save(
                application
        );

        // Send Email Notification (safe and non-blocking)
        try {
            emailService.sendApplicationApprovedEmail(saved);
        } catch (Exception e) {
            // Logged inside EmailService
        }

        return saved;
    }


    // =====================================================
    // ADMIN - REJECT APPLICATION
    // =====================================================

    public Application rejectApplication(
            Long applicationId,
            String rejectionReason) {

        Application application =
                applicationRepository
                .findById(applicationId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Application not found"
                        )
                );

        // Already approved
        if ("APPROVED".equalsIgnoreCase(
                application.getStatus())) {

            throw new RuntimeException(
                    "Approved application cannot be rejected"
            );
        }

        // Rejection reason required
        if (rejectionReason == null
                || rejectionReason.trim().isEmpty()) {

            throw new RuntimeException(
                    "Rejection reason is required"
            );
        }

        // Reject application
        application.setStatus("REJECTED");

        application.setRejectionReason(
                rejectionReason
        );

        Application saved = applicationRepository.save(
                application
        );

        // Send Email Notification (safe and non-blocking)
        try {
            emailService.sendApplicationRejectedEmail(saved);
        } catch (Exception e) {
            // Logged inside EmailService
        }

        return saved;
    }


    // =====================================================
    // RESET APPLICATION TO PENDING
    // =====================================================

    public Application resetToPending(
            Long applicationId) {

        Application application =
                applicationRepository
                .findById(applicationId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Application not found"
                        )
                );

        application.setStatus("PENDING");

        application.setRejectionReason(null);

        return applicationRepository.save(
                application
        );
    }


    // =====================================================
    // SAFE TRANSACTIONAL DELETE APPLICATION
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public void deleteApplication(Long applicationId) {
        logger.info("Initiating safe transactional deletion for Application #{}", applicationId);

        Application application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application #" + applicationId + " not found"));

        // 1. Delete all associated Documents
        List<Document> documents = documentRepository.findByApplication(application);
        if (documents != null && !documents.isEmpty()) {
            logger.info("Removing {} dependent documents for Application #{}", documents.size(), applicationId);
            documentRepository.deleteAll(documents);
        }

        // 2. Delete all associated Allotments (Regular and Spot)
        List<Allotment> allotments = allotmentRepository.findByApplicationId(applicationId);
        if (allotments != null && !allotments.isEmpty()) {
            logger.info("Removing {} dependent allotments for Application #{}", allotments.size(), applicationId);
            allotmentRepository.deleteAll(allotments);
        }

        // 3. Delete all associated Merit List entries
        List<MeritList> meritEntries = meritListRepository.findByApplication(application);
        if (meritEntries != null && !meritEntries.isEmpty()) {
            logger.info("Removing {} dependent merit list entries for Application #{}", meritEntries.size(), applicationId);
            meritListRepository.deleteAll(meritEntries);
        }

        // 4. Delete Application
        applicationRepository.delete(application);
        logger.info("Application #{} and all dependencies safely deleted", applicationId);
    }
}