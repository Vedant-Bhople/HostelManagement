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


    // =========================================================
    // GENERATE ALLOTMENT
    // =========================================================

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


        // =====================================================
        // NEW ALLOTMENT LIST
        // =====================================================

        List<Allotment> allotments =
                new ArrayList<>();


        // =====================================================
        // VALIDATE GENDER
        // =====================================================

        if ("BOYS".equalsIgnoreCase(gender)) {

            generateBoysAllotment(
                    meritList,
                    branch,
                    year,
                    allotments
            );

        } else if ("GIRLS".equalsIgnoreCase(gender)) {

            generateGirlsAllotment(
                    meritList,
                    branch,
                    year,
                    allotments
            );

        } else {

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
        // SAVE
        // =====================================================

        List<Allotment> savedAllotments =
                allotmentRepository.saveAll(
                        allotments
                );


        // =====================================================
        // SEND EMAIL
        // =====================================================

        try {

            for (Allotment item : savedAllotments) {

                if ("ALLOTTED".equalsIgnoreCase(
                        item.getAllotmentStatus())) {

                    emailService.sendAllotmentEmail(item);
                }
            }

        } catch (Exception e) {

            // Email failure must not stop allotment
            System.out.println(
                    "Email notification failed: "
                            + e.getMessage()
            );
        }


        return savedAllotments;
    }


    // =========================================================
    // BOYS ALLOTMENT
    // =========================================================
    //
    // TOTAL BOYS SEATS = 11
    //
    // OPEN / SEBC = 6
    // OBC / SBC    = 2
    // SC / ST      = 2
    // NT           = 1
    //
    // IMPORTANT:
    // OPEN/SEBC seats are first filled by overall merit
    // according to the OPEN/SEBC eligibility rules.
    //
    // Then reserved pools are filled by category merit.
    // =========================================================

    private void generateBoysAllotment(
            List<MeritList> meritList,
            String branch,
            String year,
            List<Allotment> allotments) {


        // =====================================================
        // ONLY PUBLISHED STUDENTS
        // =====================================================

        List<MeritList> students =
                getPublishedStudents(
                        meritList
                );


        // =====================================================
        // BOYS SEAT POOL
        // =====================================================

        int openSeats = 6;
        int obcSbcSeats = 2;
        int scStSeats = 2;
        int ntSeats = 1;


        // =====================================================
        // STEP 1
        // OPEN / SEBC
        //
        // Merit-wise
        // =====================================================

        for (MeritList merit : students) {

            if (openSeats <= 0) {
                break;
            }

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }


            String commonCategory =
                    normalizeCategory(
                            merit.getCategory()
                    );


            /*
             * OPEN / SEBC pool.
             *
             * OPEN and SEBC students are directly eligible.
             *
             * Other reserved-category students are NOT
             * automatically moved here.
             *
             * If your college rule allows reserved students
             * to consume OPEN seats on merit, this method can
             * be changed separately without changing
             * category normalization.
             */

            if ("OPEN".equalsIgnoreCase(
                    commonCategory)
                    || "SEBC".equalsIgnoreCase(
                    commonCategory)) {


                createAndAddAllotment(
                        merit,
                        "OPEN/SEBC",
                        "B",
                        branch,
                        year,
                        allotments,
                        "OPEN-SEBC"
                );

                openSeats--;
            }
        }


        // =====================================================
        // STEP 2
        // OBC / SBC
        // =====================================================

        for (MeritList merit : students) {

            if (obcSbcSeats <= 0) {
                break;
            }

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }


            String commonCategory =
                    normalizeCategory(
                            merit.getCategory()
                    );


            if ("OBC".equalsIgnoreCase(
                    commonCategory)
                    || "SBC".equalsIgnoreCase(
                    commonCategory)) {


                createAndAddAllotment(
                        merit,
                        "OBC/SBC",
                        "B",
                        branch,
                        year,
                        allotments,
                        "OBC-SBC"
                );

                obcSbcSeats--;
            }
        }


        // =====================================================
        // STEP 3
        // SC / ST
        // =====================================================

        for (MeritList merit : students) {

            if (scStSeats <= 0) {
                break;
            }

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }


            String commonCategory =
                    normalizeCategory(
                            merit.getCategory()
                    );


            if ("SC".equalsIgnoreCase(
                    commonCategory)
                    || "ST".equalsIgnoreCase(
                    commonCategory)) {


                createAndAddAllotment(
                        merit,
                        "SC/ST",
                        "B",
                        branch,
                        year,
                        allotments,
                        "SC-ST"
                );

                scStSeats--;
            }
        }


        // =====================================================
        // STEP 4
        // NT
        // =====================================================

        for (MeritList merit : students) {

            if (ntSeats <= 0) {
                break;
            }

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }


            String commonCategory =
                    normalizeCategory(
                            merit.getCategory()
                    );


            if ("NT".equalsIgnoreCase(
                    commonCategory)) {


                createAndAddAllotment(
                        merit,
                        "NT",
                        "B",
                        branch,
                        year,
                        allotments,
                        "NT"
                );

                ntSeats--;
            }
        }
    }


    // =========================================================
    // GIRLS ALLOTMENT
    // =========================================================
    //
    // TOTAL GIRLS SEATS = 3
    //
    // OPEN                  = 1
    // OBC / SBC             = 1
    // AGAINST NT/SC/ST      = 1
    //
    // =========================================================

    private void generateGirlsAllotment(
            List<MeritList> meritList,
            String branch,
            String year,
            List<Allotment> allotments) {


        // =====================================================
        // ONLY PUBLISHED STUDENTS
        // =====================================================

        List<MeritList> students =
                getPublishedStudents(
                        meritList
                );


        // =====================================================
        // STEP 1
        // OPEN = 1
        //
        // Only OPEN students
        // =====================================================

        for (MeritList merit : students) {

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }


            String commonCategory =
                    normalizeCategory(
                            merit.getCategory()
                    );


            if ("OPEN".equalsIgnoreCase(
                    commonCategory)) {


                createAndAddAllotment(
                        merit,
                        "OPEN",
                        "G",
                        branch,
                        year,
                        allotments,
                        "OPEN"
                );

                break;
            }
        }


        // =====================================================
        // STEP 2
        // OBC / SBC = 1
        // =====================================================

        for (MeritList merit : students) {

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }


            String commonCategory =
                    normalizeCategory(
                            merit.getCategory()
                    );


            if ("OBC".equalsIgnoreCase(
                    commonCategory)
                    || "SBC".equalsIgnoreCase(
                    commonCategory)) {


                createAndAddAllotment(
                        merit,
                        "OBC/SBC",
                        "G",
                        branch,
                        year,
                        allotments,
                        "OBC-SBC"
                );

                break;
            }
        }


        // =====================================================
        // STEP 3
        // AGAINST NT / SC / ST = 1
        // =====================================================

        for (MeritList merit : students) {

            if (isAlreadyAllotted(
                    merit,
                    allotments)) {

                continue;
            }


            String commonCategory =
                    normalizeCategory(
                            merit.getCategory()
                    );


            if ("NT".equalsIgnoreCase(
                    commonCategory)
                    || "SC".equalsIgnoreCase(
                    commonCategory)
                    || "ST".equalsIgnoreCase(
                    commonCategory)) {


                createAndAddAllotment(
                        merit,
                        "AGAINST NT/SC/ST",
                        "G",
                        branch,
                        year,
                        allotments,
                        "AGAINST"
                );

                break;
            }
        }
    }


    // =========================================================
    // GET PUBLISHED STUDENTS
    // =========================================================

    private List<MeritList> getPublishedStudents(
            List<MeritList> meritList) {


        List<MeritList> students =
                new ArrayList<>();


        for (MeritList merit : meritList) {

            if (merit.isPublished()) {

                students.add(merit);
            }
        }


        return students;
    }


    // =========================================================
    // ADD ALLOTMENT
    // =========================================================

    private void createAndAddAllotment(
            MeritList merit,
            String allotmentCategory,
            String hostelCode,
            String branch,
            String year,
            List<Allotment> allotments,
            String seatPrefix) {


        int seatNumber =
                getNextSeatNumber(
                        allotments,
                        hostelCode,
                        seatPrefix
                );


        Allotment allotment =
                createAllotment(
                        merit,
                        allotmentCategory,
                        hostelCode,
                        branch,
                        year,
                        seatNumber,
                        seatPrefix
                );


        allotments.add(
                allotment
        );
    }


    // =========================================================
    // GET NEXT AVAILABLE SEAT NUMBER
    // =========================================================

    private int getNextSeatNumber(
            List<Allotment> allotments,
            String hostelCode,
            String seatPrefix) {


        int highest = 0;


        for (Allotment allotment : allotments) {

            if (allotment.getSeatNumber() == null) {
                continue;
            }


            String seat =
                    allotment.getSeatNumber();


            String prefix =
                    hostelCode
                            + "-"
                            + seatPrefix
                            + "-";


            if (!seat.startsWith(prefix)) {
                continue;
            }


            try {

                String numberPart =
                        seat.substring(
                                prefix.length()
                        );


                int number =
                        Integer.parseInt(
                                numberPart
                        );


                if (number > highest) {
                    highest = number;
                }

            } catch (Exception ignored) {

                // Ignore invalid seat number
            }
        }


        return highest + 1;
    }


    // =========================================================
    // ADD WAITING STUDENTS
    // =========================================================

    private void addWaitingStudents(
            List<MeritList> meritList,
            String branch,
            String year,
            List<Allotment> allotments) {


        int waitingNumber = 1;


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
                            waitingNumber,
                            "WAIT"
                    );


            waiting.setAllotmentStatus(
                    "WAITING"
            );


            waiting.setSeatNumber(
                    hostelCode
                            + "-WAIT-"
                            + String.format(
                            "%02d",
                            waitingNumber
                    )
            );


            allotments.add(
                    waiting
            );


            waitingNumber++;
        }
    }


    // =========================================================
    // CHECK ALREADY ALLOTTED
    // =========================================================

    private boolean isAlreadyAllotted(
            MeritList merit,
            List<Allotment> allotments) {


        if (merit == null
                || merit.getId() == null) {

            return false;
        }


        for (Allotment allotment : allotments) {

            if (allotment.getMeritList() == null) {
                continue;
            }


            if (allotment.getMeritList()
                    .getId()
                    .equals(
                            merit.getId()
                    )) {

                return true;
            }
        }


        return false;
    }


    // =========================================================
    // CREATE ALLOTMENT
    // =========================================================

    private Allotment createAllotment(
            MeritList merit,
            String allotmentCategory,
            String hostelCode,
            String branch,
            String year,
            int seatNumber,
            String seatPrefix) {


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
        // HOSTEL TYPE
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
                        seatPrefix,
                        seatNumber
                )
        );


        // =====================================================
        // STATUS
        // =====================================================

        allotment.setAllotmentStatus(
                "ALLOTTED"
        );


        return allotment;
    }


    // =========================================================
    // GENERATE SEAT NUMBER
    // =========================================================

    private String generateSeatNumber(
            String hostelCode,
            String branch,
            String year,
            String seatPrefix,
            int seatNumber) {


        String branchCode =
                getBranchCode(
                        branch
                );


        return hostelCode
                + "-"
                + branchCode
                + "-Y"
                + year
                + "-"
                + seatPrefix
                + "-"
                + String.format(
                        "%02d",
                        seatNumber
                );
    }


    // =========================================================
    // CATEGORY NORMALIZATION
    // =========================================================
    //
    // ORIGINAL CATEGORY IS NOT CHANGED.
    //
    // Only COMMON CATEGORY is returned.
    //
    // =========================================================

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
        // OPEN
        // =====================================================

        if (value.equals("OPEN")) {

            return "OPEN";
        }


        // =====================================================
        // OBC
        // =====================================================

        if (value.equals("OBC")) {

            return "OBC";
        }


        // =====================================================
        // SBC
        // =====================================================

        if (value.equals("SBC")) {

            return "SBC";
        }


        // =====================================================
        // SC
        // =====================================================

        if (value.equals("SC")) {

            return "SC";
        }


        // =====================================================
        // ST
        // =====================================================

        if (value.equals("ST")) {

            return "ST";
        }


        // =====================================================
        // NT GROUP
        // =====================================================
        //
        // VJ
        // VJ-A
        // DT/VJ NT-A
        // NT-1 NT-B
        // NT-2 NT-C
        // NT-3 NT-D
        // NT-B
        // NT-C
        // NT-D
        //
        // ALL -> NT
        // =====================================================

        if (value.equals("VJ")
                || value.equals("VJ-A")
                || value.equals("DT/VJ NT-A")
                || value.equals("NT-1 NT-B")
                || value.equals("NT-2 NT-C")
                || value.equals("NT-3 NT-D")
                || value.equals("NT-B")
                || value.equals("NT-C")
                || value.equals("NT-D")) {

            return "NT";
        }


        // =====================================================
        // SEBC
        // =====================================================

        if (value.equals("SEBC")) {

            return "SEBC";
        }


        // =====================================================
        // UNKNOWN CATEGORY
        // =====================================================

        return "OPEN";
    }


    // =========================================================
    // CATEGORY CODE
    // =========================================================

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

            case "SBC":
                return "SBC";

            case "NT":
                return "NT";

            case "OPEN":
                return "OP";

            case "SEBC":
                return "SEBC";

            case "OBC/SBC":
                return "OBC";

            case "OPEN/SEBC":
                return "OP";

            case "SC/ST":
                return "SCST";

            case "AGAINST NT/SC/ST":
                return "AGAINST";

            case "WAITING":
                return "WAIT";

            default:
                return "GEN";
        }
    }


    // =========================================================
    // BRANCH CODE
    // =========================================================

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


    // =========================================================
    // GET ALLOTMENT
    // =========================================================

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


    // =========================================================
    // GET STUDENT ALLOTMENTS
    // =========================================================

    public List<Allotment> getStudentAllotments(
            Long userId) {


        return allotmentRepository
                .findByApplication_User_Id(
                        userId
                );
    }


    // =========================================================
    // ACCEPT SEAT
    // =========================================================

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


    // =========================================================
    // REJECT SEAT
    // =========================================================

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
