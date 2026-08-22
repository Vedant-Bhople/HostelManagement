package com.hostel.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hostel.model.Application;
import com.hostel.model.User;
import com.hostel.repository.ApplicationRepository;
import com.hostel.repository.UserRepository;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

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
    // DELETE APPLICATION
    // =====================================================

    public void deleteApplication(
            Long applicationId) {

        Application application =
                applicationRepository
                .findById(applicationId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Application not found"
                        )
                );

        applicationRepository.delete(application);
    }
}