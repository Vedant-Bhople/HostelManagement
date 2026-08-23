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

        List<Allotment> savedAllotments =
                allotmentRepository.saveAll(
                        allotments
                );


        // =====================================================
        // SEND EMAIL NOTIFICATIONS
        // =====================================================

        try {

            for (Allotment item : savedAllotments) {

                if ("ALLOTTED".equalsIgnoreCase(
                        item.getAllotmentStatus())) {

                    emailService.sendAllotmentEmail(item);
                }
            }

        } catch (Exception e) {

            // Email failure should not stop allotment
            System.out.println(
                    "Email notification failed: "
                    + e.getMessage()
            );
        }


        return savedAllotments;
    }


    // =====================================================
    // BOYS ALLOTMENT
    // =====================================================
    //
    // IMPORTANT ALLOCATION LOGIC
    //
    // OPEN = 7
    // SC   = 1
    // ST   = 1
    // OBC  = 2
    //
    // TOTAL = 11
    //
    // OPEN SEATS ARE ALLOCATED FIRST BY OVERALL MERIT.
    //
    // SC/ST/OBC STUDENTS CAN ALSO GET OPEN SEATS.
    //
    // AFTER OPEN SEATS ARE ALLOCATED,
    // RESERVED SEATS ARE GIVEN TO REMAINING
    // ELIGIBLE STUDENTS OF THAT CATEGORY.
    // =====================================================

    private void generateBoysAllotment(
            List<MeritList> meritList,
            String branch,
            String year,
            List<Allotment> allotments) {


        // =====================================================
        // GET ONLY PUBLISHED STUDENTS
        // =====================================================

        List<MeritList> publishedStudents =
                new ArrayList<>();

        for (MeritList merit : meritList) {

            if (merit.isPublished()) {

                publishedStudents.add(merit);
            }
        }


        // =====================================================
        // STEP 1
        // OPEN SEATS = 7
        //
        // Overall merit regardless of caste/category
        // =====================================================

        allotRemainingStudents(
                publishedStudents,
                7,
                "OPEN",
                "B",
                branch,
                year,
                allotments
        );


        // =====================================================
        // STEP 2
        // SC RESERVED SEAT = 1
        // =====================================================

        List<MeritList> scStudents =
                new ArrayList<>();

        for (MeritList merit : publishedStudents) {

            if (isCategory(
                    merit,
                    "SC")) {

                scStudents.add(merit);
            }
        }


        allotCategory(
                scStudents,
                1,
                "SC",
                "B",
                branch,
                year,
                allotments
        );


        // =====================================================
        // STEP 3
        // ST RESERVED SEAT = 1
        // =====================================================

        List<MeritList> stStudents =
                new ArrayList<>();

        for (MeritList merit : publishedStudents) {

            if (isCategory(
                    merit,
                    "ST")) {

                stStudents.add(merit);
            }
        }


        allotCategory(
                stStudents,
                1,
                "ST",
                "B",
                branch,
                year,
                allotments
        );


        // =====================================================
        // STEP 4
        // OBC RESERVED SEATS = 2
        // =====================================================

        List<MeritList> obcStudents =
                new ArrayList<>();

        for (MeritList merit : publishedStudents) {

            if (isCategory(
                    merit,
                    "OBC")) {

                obcStudents.add(merit);
            }
        }


        allotCategory(
                obcStudents,
                2,
                "OBC",
                "B",
                branch,
                year,
                allotments
        );
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


        List<MeritList> publishedStudents =
                new ArrayList<>();

        for (MeritList merit : meritList) {

            if (merit.isPublished()) {

                publishedStudents.add(merit);
            }
        }


        // =====================================================
        // STEP 1
        // OPEN = 1
        //
        // Overall merit
        // =====================================================

        allotRemainingStudents(
                publishedStudents,
                1,
                "OPEN",
                "G",
                branch,
                year,
                allotments
        );


        // =====================================================
        // STEP 2
        // OBC = 1
        // =====================================================

        List<MeritList> obcStudents =
                new ArrayList<>();

        for (MeritList merit : publishedStudents) {

            if (isCategory(
                    merit,
                    "OBC")) {

                obcStudents.add(merit);
            }
        }


        allotCategory(
                obcStudents,
                1,
                "OBC",
                "G",
                branch,
                year,
                allotments
        );


        // =====================================================
        // STEP 3
        // ALL CASTE = 1
        // =====================================================

        allotRemainingStudents(
                publishedStudents,
                1,
                "ALL CASTE",
                "G",
                branch,
                year,
                allotments
        );
    }


    // =====================================================
    // CHECK CATEGORY
    // =====================================================

    private boolean isCategory(
            MeritList merit,
            String requiredCategory) {

        String category =
                normalizeCategory(
                        merit.getMeritCategory()
                );

        return requiredCategory.equalsIgnoreCase(
                category
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


            // =================================================
            // DON'T ALLOT SAME STUDENT TWICE
            // =================================================

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


            allotments.add(
                    allotment
            );


            seatsGiven++;
        }
    }


    // =====================================================
    // ALLOT REMAINING STUDENTS BY OVERALL MERIT
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


            // =================================================
            // DON'T ALLOT SAME STUDENT TWICE
            // =================================================

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


            allotments.add(
                    allotment
            );


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


            // =================================================
            // ALREADY ALLOTTED
            // =================================================

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }


            String hostelCode =
                    "BOYS".equalsIgnoreCase(
                            merit.getGender()
                    )
                    ? "B"
                    : "G";


            Allotment waiting =
                    createAllotment(
                            merit,
                            "WAITING",
                            hostelCode,
                            branch,
                            year,
                            allotments.size() + 1
                    );


            // =================================================
            // WAITING STATUS
            // =================================================

            waiting.setAllotmentStatus(
                    "WAITING"
            );


            // =================================================
            // NO ACTUAL SEAT FOR WAITING STUDENT
            // =================================================

            waiting.setSeatNumber(
                    "WAITING-"
                    + String.format(
                            "%02d",
                            allotments.size() + 1
                    )
            );


            allotments.add(
                    waiting
            );
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
                            .equals(
                                    merit.getId()
                            )) {

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


        // =====================================================
        // APPLICATION
        // =====================================================

        allotment.setApplication(
                merit.getApplication()
        );


        // =====================================================
        // MERIT
        // =====================================================

        allotment.setMeritList(
                merit
        );


        // =====================================================
        // HOSTEL
        // =====================================================

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


        // =====================================================
        // BASIC INFORMATION
        // =====================================================

        allotment.setGender(
                merit.getGender()
        );


        allotment.setBranch(
                branch
        );


        allotment.setYear(
                year
        );


        // =====================================================
        // ORIGINAL CATEGORY
        // =====================================================

        allotment.setCategory(
                merit.getCategory()
        );


        // =====================================================
        // ALLOTMENT CATEGORY
        // =====================================================

        allotment.setAllotmentCategory(
                allotmentCategory
        );


        // =====================================================
        // MERIT INFORMATION
        // =====================================================

        allotment.setMeritRank(
                merit.getMeritRank()
        );


        allotment.setAggregate(
                merit.getAggregate()
        );


        // =====================================================
        // SEAT NUMBER
        // =====================================================

        allotment.setSeatNumber(
                generateSeatNumber(
                        hostelCode,
                        branch,
                        year,
                        allotmentCategory,
                        seatNumber
                )
        );


        // =====================================================
        // INITIAL STATUS
        // =====================================================

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


        // =====================================================
        // ST GROUP
        // =====================================================

        if (value.equals("ST")
                || value.equals("VJ-A")
                || value.equals("NT-B")
                || value.equals("NT-C")
                || value.equals("NT-D")) {

            return "ST";
        }


        // =====================================================
        // SC
        // =====================================================

        if (value.equals("SC")) {

            return "SC";
        }


        // =====================================================
        // OBC
        // =====================================================

        if (value.equals("OBC")) {

            return "OBC";
        }


        // =====================================================
        // OPEN / OTHER
        // =====================================================

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


        // =====================================================
        // REJECTED
        // =====================================================

        if ("REJECTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "This seat has already been rejected"
            );
        }


        // =====================================================
        // WAITING
        // =====================================================

        if ("WAITING".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Waiting list student cannot accept a seat"
            );
        }


        // =====================================================
        // ALREADY ACCEPTED
        // =====================================================

        if ("ACCEPTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Seat is already accepted"
            );
        }


        // =====================================================
        // ACCEPT
        // =====================================================

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


        // =====================================================
        // ACCEPTED
        // =====================================================

        if ("ACCEPTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Accepted seat cannot be rejected"
            );
        }


        // =====================================================
        // ALREADY REJECTED
        // =====================================================

        if ("REJECTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Seat is already rejected"
            );
        }


        // =====================================================
        // WAITING
        // =====================================================

        if ("WAITING".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Waiting list student has no allotted seat"
            );
        }


        // =====================================================
        // REJECT
        // =====================================================

        allotment.setAllotmentStatus(
                "REJECTED"
        );


        return allotmentRepository.save(
                allotment
        );
    }
}
