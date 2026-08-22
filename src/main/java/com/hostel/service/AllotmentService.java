package com.hostel.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hostel.model.Allotment;
import com.hostel.model.MeritList;
import com.hostel.repository.AllotmentRepository;
import com.hostel.repository.MeritListRepository;

@Service
public class AllotmentService {

    @Autowired
    private MeritListRepository meritListRepository;

    @Autowired
    private AllotmentRepository allotmentRepository;

    @Autowired
    private EmailService emailService;


    // =====================================================
    // GENERATE ALLOTMENT
    // =====================================================

    public List<Allotment> generateAllotment(
            String gender,
            String branch,
            String year) {

        List<MeritList> meritList =
                meritListRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );

        if (meritList == null || meritList.isEmpty()) {

            throw new RuntimeException(
                    "Merit list not found for "
                    + gender + " - "
                    + branch + " - "
                    + year
            );
        }


        // =====================================================
        // CHECK PUBLISHED MERIT LIST
        // =====================================================

        boolean anyPublished = false;

        for (MeritList merit : meritList) {

            if (merit.isPublished()) {

                anyPublished = true;
                break;
            }
        }

        if (!anyPublished) {

            throw new RuntimeException(
                    "Merit list is not published yet for "
                    + gender + " - "
                    + branch + " - "
                    + year
            );
        }


        // =====================================================
        // DELETE PREVIOUS ALLOTMENT
        // =====================================================

        List<Allotment> oldAllotments =
                allotmentRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );

        if (oldAllotments != null
                && !oldAllotments.isEmpty()) {

            allotmentRepository.deleteAll(
                    oldAllotments
            );
        }


        List<Allotment> allotments =
                new ArrayList<>();


        // =====================================================
        // BOYS
        // =====================================================

        if ("BOYS".equalsIgnoreCase(gender)) {

            generateBoysAllotment(
                    meritList,
                    branch,
                    year,
                    allotments
            );
        }


        // =====================================================
        // GIRLS
        // =====================================================

        else if ("GIRLS".equalsIgnoreCase(gender)) {

            generateGirlsAllotment(
                    meritList,
                    branch,
                    year,
                    allotments
            );
        }

        else {

            throw new RuntimeException(
                    "Invalid gender. Use BOYS or GIRLS."
            );
        }


        // =====================================================
        // REMAINING STUDENTS -> WAITING LIST
        // =====================================================

        addWaitingStudents(
                meritList,
                branch,
                year,
                allotments
        );


        // =====================================================
        // SAVE ALLOTMENTS + WAITING LIST
        // =====================================================

        List<Allotment> savedAllotments = allotmentRepository.saveAll(
                allotments
        );

        // Send Email Notifications for allotted students (safe and non-blocking)
        try {
            for (Allotment item : savedAllotments) {
                if ("ALLOTTED".equalsIgnoreCase(item.getAllotmentStatus())) {
                    emailService.sendAllotmentEmail(item);
                }
            }
        } catch (Exception e) {
            // Handled inside EmailService
        }

        return savedAllotments;
    }


    // =====================================================
    // BOYS ALLOTMENT
    // =====================================================
    //
    // SC   = 1
    // ST   = 1
    // OBC  = 2
    // OPEN = 7
    //
    // TOTAL = 11
    // =====================================================

    private void generateBoysAllotment(
            List<MeritList> meritList,
            String branch,
            String year,
            List<Allotment> allotments) {

        List<MeritList> scStudents =
                new ArrayList<>();

        List<MeritList> stStudents =
                new ArrayList<>();

        List<MeritList> obcStudents =
                new ArrayList<>();

        List<MeritList> allStudents =
                new ArrayList<>();


        for (MeritList merit : meritList) {

            if (!merit.isPublished()) {
                continue;
            }

            allStudents.add(merit);

            String category =
                    normalizeCategory(
                            merit.getMeritCategory()
                    );

            if ("SC".equals(category)) {

                scStudents.add(merit);

            } else if ("ST".equals(category)) {

                stStudents.add(merit);

            } else if ("OBC".equals(category)) {

                obcStudents.add(merit);
            }
        }


        // SC = 1
        allotCategory(
                scStudents,
                1,
                "SC",
                "B",
                branch,
                year,
                allotments
        );


        // ST = 1
        allotCategory(
                stStudents,
                1,
                "ST",
                "B",
                branch,
                year,
                allotments
        );


        // OBC = 2
        allotCategory(
                obcStudents,
                2,
                "OBC",
                "B",
                branch,
                year,
                allotments
        );


        // OPEN = 7
        allotRemainingStudents(
                allStudents,
                7,
                "OPEN",
                "B",
                branch,
                year,
                allotments
        );


        // =====================================================
        // FALLBACK
        // =====================================================

        int totalSeats = 11;

        if (allotments.size() < totalSeats) {

            int remaining =
                    totalSeats - allotments.size();

            allotRemainingStudents(
                    allStudents,
                    remaining,
                    "OPEN",
                    "B",
                    branch,
                    year,
                    allotments
            );
        }
    }


    // =====================================================
    // GIRLS ALLOTMENT
    // =====================================================
    //
    // OBC       = 1
    // OPEN      = 1
    // ALL CASTE = 1
    //
    // TOTAL = 3
    // =====================================================

    private void generateGirlsAllotment(
            List<MeritList> meritList,
            String branch,
            String year,
            List<Allotment> allotments) {

        List<MeritList> obcStudents =
                new ArrayList<>();

        List<MeritList> allStudents =
                new ArrayList<>();


        for (MeritList merit : meritList) {

            if (!merit.isPublished()) {
                continue;
            }

            allStudents.add(merit);

            String category =
                    normalizeCategory(
                            merit.getMeritCategory()
                    );

            if ("OBC".equals(category)) {

                obcStudents.add(merit);
            }
        }


        // OBC = 1
        allotCategory(
                obcStudents,
                1,
                "OBC",
                "G",
                branch,
                year,
                allotments
        );


        // OPEN = 1
        allotRemainingStudents(
                allStudents,
                1,
                "OPEN",
                "G",
                branch,
                year,
                allotments
        );


        // ALL CASTE = 1
        allotRemainingStudents(
                allStudents,
                1,
                "ALL CASTE",
                "G",
                branch,
                year,
                allotments
        );
    }


    // =====================================================
    // CATEGORY ALLOTMENT
    // =====================================================

    private void allotCategory(
            List<MeritList> students,
            int numberOfSeats,
            String allotmentCategory,
            String hostelCode,
            String branch,
            String year,
            List<Allotment> allotments) {

        int seatsGiven = 0;

        for (MeritList merit : students) {

            if (seatsGiven >= numberOfSeats) {
                break;
            }

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }

            Allotment allotment =
                    createAllotment(
                            merit,
                            allotmentCategory,
                            hostelCode,
                            branch,
                            year,
                            allotments.size() + 1
                    );

            allotments.add(allotment);

            seatsGiven++;
        }
    }


    // =====================================================
    // REMAINING STUDENTS BY MERIT
    // =====================================================

    private void allotRemainingStudents(
            List<MeritList> students,
            int numberOfSeats,
            String allotmentCategory,
            String hostelCode,
            String branch,
            String year,
            List<Allotment> allotments) {

        int seatsGiven = 0;

        for (MeritList merit : students) {

            if (seatsGiven >= numberOfSeats) {
                break;
            }

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }

            Allotment allotment =
                    createAllotment(
                            merit,
                            allotmentCategory,
                            hostelCode,
                            branch,
                            year,
                            allotments.size() + 1
                    );

            allotments.add(allotment);

            seatsGiven++;
        }
    }


    // =====================================================
    // ADD REMAINING STUDENTS TO WAITING LIST
    // =====================================================

    private void addWaitingStudents(
            List<MeritList> meritList,
            String branch,
            String year,
            List<Allotment> allotments) {

        for (MeritList merit : meritList) {

            if (!merit.isPublished()) {
                continue;
            }

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }

            String hostelCode =
                    "BOYS".equalsIgnoreCase(
                            merit.getGender()
                    ) ? "B" : "G";


            Allotment waiting =
                    createAllotment(
                            merit,
                            "WAITING",
                            hostelCode,
                            branch,
                            year,
                            allotments.size() + 1
                    );


            // Important
            waiting.setAllotmentStatus(
                    "WAITING"
            );


            // Waiting students don't have actual seat
            waiting.setSeatNumber(
                    "WAITING-" +
                    String.format(
                            "%02d",
                            allotments.size() + 1
                    )
            );


            allotments.add(waiting);
        }
    }


    // =====================================================
    // CHECK ALREADY ALLOTTED
    // =====================================================

    private boolean isAlreadyAllotted(
            MeritList merit,
            List<Allotment> allotments) {

        for (Allotment allotment : allotments) {

            if (allotment.getMeritList() != null
                    && allotment.getMeritList()
                            .getId()
                            .equals(merit.getId())) {

                return true;
            }
        }

        return false;
    }


    // =====================================================
    // CREATE ALLOTMENT
    // =====================================================

    private Allotment createAllotment(
            MeritList merit,
            String allotmentCategory,
            String hostelCode,
            String branch,
            String year,
            int seatNumber) {

        Allotment allotment =
                new Allotment();


        // Application
        allotment.setApplication(
                merit.getApplication()
        );


        // Merit
        allotment.setMeritList(
                merit
        );


        // Hostel
        if ("B".equalsIgnoreCase(
                hostelCode)) {

            allotment.setHostelType(
                    "BOYS HOSTEL"
            );

        } else {

            allotment.setHostelType(
                    "GIRLS HOSTEL"
            );
        }


        // Basic information
        allotment.setGender(
                merit.getGender()
        );

        allotment.setBranch(
                branch
        );

        allotment.setYear(
                year
        );


        // Original category
        allotment.setCategory(
                merit.getCategory()
        );


        // Allotment category
        allotment.setAllotmentCategory(
                allotmentCategory
        );


        // Merit
        allotment.setMeritRank(
                merit.getMeritRank()
        );

        allotment.setAggregate(
                merit.getAggregate()
        );


        // Seat number
        allotment.setSeatNumber(
                generateSeatNumber(
                        hostelCode,
                        branch,
                        year,
                        allotmentCategory,
                        seatNumber
                )
        );


        // Initial status
        allotment.setAllotmentStatus(
                "ALLOTTED"
        );


        return allotment;
    }


    // =====================================================
    // SEAT NUMBER
    // =====================================================

    private String generateSeatNumber(
            String hostelCode,
            String branch,
            String year,
            String category,
            int seatNumber) {

        String branchCode =
                getBranchCode(branch);

        String categoryCode =
                getCategoryCode(category);


        return hostelCode
                + "-"
                + branchCode
                + "-Y"
                + year
                + "-"
                + categoryCode
                + "-"
                + String.format(
                        "%02d",
                        seatNumber
                );
    }


    // =====================================================
    // CATEGORY CODE
    // =====================================================

    private String getCategoryCode(
            String category) {

        if (category == null) {
            return "GEN";
        }


        switch (
                category
                .trim()
                .toUpperCase()
        ) {

            case "SC":
                return "SC";

            case "ST":
                return "ST";

            case "OBC":
                return "OBC";

            case "OPEN":
                return "OP";

            case "ALL CASTE":
                return "ALL";

            case "WAITING":
                return "WAIT";

            default:
                return "GEN";
        }
    }


    // =====================================================
    // CATEGORY NORMALIZATION
    // =====================================================

    private String normalizeCategory(
            String category) {

        if (category == null
                || category.trim().isEmpty()) {

            return "OPEN";
        }


        String value =
                category
                .trim()
                .toUpperCase();


        // ST group
        if (value.equals("ST")
                || value.equals("VJ-A")
                || value.equals("NT-B")
                || value.equals("NT-C")
                || value.equals("NT-D")) {

            return "ST";
        }


        // SC
        if (value.equals("SC")) {
            return "SC";
        }


        // OBC
        if (value.equals("OBC")) {
            return "OBC";
        }


        // OPEN + OTHER
        return "OPEN";
    }


    // =====================================================
    // BRANCH CODE
    // =====================================================

    private String getBranchCode(
            String branch) {

        if (branch == null) {
            return "OTHER";
        }


        switch (
                branch
                .trim()
                .toUpperCase()
        ) {

            case "COMPUTER":
                return "COMP";

            case "MECHANICAL":
                return "MECH";

            case "CIVIL":
                return "CIVIL";

            case "ELECTRICAL":
                return "ELEC";

            case "IT":
                return "IT";

            default:
                return "OTHER";
        }
    }


    // =====================================================
    // GET ALLOTMENT
    // =====================================================

    public List<Allotment> getAllotment(
            String gender,
            String branch,
            String year) {

        return allotmentRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );
    }


    // =====================================================
    // GET STUDENT ALLOTMENTS
    // =====================================================

    public List<Allotment> getStudentAllotments(
            Long userId) {

        return allotmentRepository
                .findByApplication_User_Id(
                        userId
                );
    }


    // =====================================================
    // ACCEPT SEAT
    // =====================================================

    public Allotment acceptSeat(
            Long allotmentId) {

        Allotment allotment =
                allotmentRepository
                .findById(allotmentId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Allotment not found"
                        )
                );


        if ("REJECTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "This seat has already been rejected"
            );
        }


        if ("WAITING".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Waiting list student cannot accept a seat"
            );
        }


        if ("ACCEPTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Seat is already accepted"
            );
        }


        allotment.setAllotmentStatus(
                "ACCEPTED"
        );


        return allotmentRepository.save(
                allotment
        );
    }


    // =====================================================
    // REJECT SEAT
    // =====================================================

    public Allotment rejectSeat(
            Long allotmentId) {

        Allotment allotment =
                allotmentRepository
                .findById(allotmentId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Allotment not found"
                        )
                );


        if ("ACCEPTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Accepted seat cannot be rejected"
            );
        }


        if ("REJECTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Seat is already rejected"
            );
        }


        if ("WAITING".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Waiting list student has no allotted seat"
            );
        }


        allotment.setAllotmentStatus(
                "REJECTED"
        );


        return allotmentRepository.save(
                allotment
        );
    }
}
