package com.hostel.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hostel.model.Allotment;
import com.hostel.repository.AllotmentRepository;

@Service
public class VacancyService {

    @Autowired
    private AllotmentRepository allotmentRepository;


    // =====================================================
    // GET VACANCY
    // =====================================================

    public Map<String, Object> getVacancy(
            String gender,
            String branch,
            String year) {

        int totalSeats;

        // Boys = 11 seats
        if ("BOYS".equalsIgnoreCase(gender)) {

            totalSeats = 11;

        }
        // Girls = 3 seats
        else if ("GIRLS".equalsIgnoreCase(gender)) {

            totalSeats = 3;

        }
        else {

            throw new RuntimeException(
                    "Invalid gender. Use BOYS or GIRLS."
            );
        }


        // Get allotments
        List<Allotment> allotments =
                allotmentRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );


        int allottedSeats = 0;
        int acceptedSeats = 0;


        for (Allotment allotment : allotments) {

            String status =
                    allotment.getAllotmentStatus();

            if ("ALLOTTED".equalsIgnoreCase(status)
                    || "ACCEPTED".equalsIgnoreCase(status)) {

                allottedSeats++;
            }

            if ("ACCEPTED".equalsIgnoreCase(status)) {

                acceptedSeats++;
            }
        }


        int availableSeats =
                totalSeats - allottedSeats;


        if (availableSeats < 0) {
            availableSeats = 0;
        }


        Map<String, Object> result =
                new HashMap<>();

        result.put("gender", gender);
        result.put("branch", branch);
        result.put("year", year);

        result.put(
                "totalSeats",
                totalSeats
        );

        result.put(
                "allottedSeats",
                allottedSeats
        );

        result.put(
                "acceptedSeats",
                acceptedSeats
        );

        result.put(
                "availableSeats",
                availableSeats
        );


        return result;
    }
}