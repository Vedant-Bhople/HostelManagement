package com.hostel.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hostel.repository.AllotmentRepository;
import com.hostel.repository.ApplicationRepository;
import com.hostel.repository.DocumentRepository;
import com.hostel.repository.MeritListRepository;

@Service
public class AdminDashboardService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AllotmentRepository allotmentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private MeritListRepository meritListRepository;


    // =====================================================
    // DASHBOARD SUMMARY
    // =====================================================

    public Map<String, Object> getDashboardSummary() {

        Map<String, Object> dashboard =
                new HashMap<>();

        // Applications
        List<?> applications =
                applicationRepository.findAll();

        dashboard.put(
                "totalApplications",
                applications.size()
        );


        // Approved applications
        long approved =
                applicationRepository
                .findByStatus("APPROVED")
                .size();

        dashboard.put(
                "approvedApplications",
                approved
        );


        // Pending applications
        long pending =
                applicationRepository
                .findByStatus("PENDING")
                .size();

        dashboard.put(
                "pendingApplications",
                pending
        );


        // Rejected applications
        long rejected =
                applicationRepository
                .findByStatus("REJECTED")
                .size();

        dashboard.put(
                "rejectedApplications",
                rejected
        );


        // Allotments
        List<?> allotments =
                allotmentRepository.findAll();

        dashboard.put(
                "totalAllotments",
                allotments.size()
        );


        // Accepted seats
        long accepted =
                allotmentRepository
                .findByAllotmentStatus("ACCEPTED")
                .size();

        dashboard.put(
                "acceptedSeats",
                accepted
        );


        // Allotted seats
        long allotted =
                allotmentRepository
                .findByAllotmentStatus("ALLOTTED")
                .size();

        dashboard.put(
                "allottedSeats",
                allotted
        );


        // Rejected seats
        long rejectedSeats =
                allotmentRepository
                .findByAllotmentStatus("REJECTED")
                .size();

        dashboard.put(
                "rejectedSeats",
                rejectedSeats
        );


        // Documents
        long totalDocuments =
                documentRepository
                .findAll()
                .size();

        dashboard.put(
                "totalDocuments",
                totalDocuments
        );


        // Merit lists
        long totalMeritLists =
                meritListRepository
                .findAll()
                .size();

        dashboard.put(
                "totalMeritLists",
                totalMeritLists
        );


        return dashboard;
    }
}