package com.hostel.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hostel.dto.StudentDashboardDTO;
import com.hostel.model.Allotment;
import com.hostel.model.Application;
import com.hostel.model.Document;
import com.hostel.model.User;
import com.hostel.repository.AllotmentRepository;
import com.hostel.repository.ApplicationRepository;
import com.hostel.repository.DocumentRepository;
import com.hostel.repository.UserRepository;

@Service
public class StudentDashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AllotmentRepository allotmentRepository;


    // =====================================================
    // GET STUDENT DASHBOARD
    // =====================================================

    public StudentDashboardDTO getStudentDashboard(Long userId) {

        // =================================================
        // FIND USER
        // =================================================

        User user =
                userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"
                        )
                );


        // =================================================
        // FIND STUDENT APPLICATIONS
        // =================================================

        List<Application> applications =
                applicationRepository.findByUser(user);

        if (applications == null ||
                applications.isEmpty()) {

            throw new RuntimeException(
                    "No application found for this student"
            );
        }


        // =================================================
        // GET LATEST APPLICATION
        // =================================================

        Application application =
                applications.get(
                        applications.size() - 1
                );


        // =================================================
        // GET DOCUMENTS
        // =================================================

        List<Document> documents =
                documentRepository.findByApplication(
                        application
                );


        // =================================================
        // GET ALLOTMENTS
        // =================================================

        List<Allotment> allotments =
                allotmentRepository.findByApplicationId(
                        application.getId()
                );


        // =================================================
        // RETURN DASHBOARD
        // =================================================

        return new StudentDashboardDTO(
                application,
                documents,
                allotments
        );
    }
}