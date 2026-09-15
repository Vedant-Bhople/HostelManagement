package com.hostelmanagement.service;

import com.hostelmanagement.model.Allotment;
import com.hostelmanagement.model.MeritList;
import com.hostelmanagement.repository.AllotmentRepository;
import com.hostelmanagement.repository.MeritListRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AllotmentService {

    private final MeritListRepository meritListRepository;
    private final AllotmentRepository allotmentRepository;
    private final EmailService emailService;

    public AllotmentService(
            MeritListRepository meritListRepository,
            AllotmentRepository allotmentRepository,
            EmailService emailService) {

        this.meritListRepository = meritListRepository;
        this.allotmentRepository = allotmentRepository;
        this.emailService = emailService;
    }

    // ============================================================
    // MAIN ALLOTMENT METHOD
    // ============================================================

    public List<Allotment> generateAllotment(
            String gender,
            String branch,
            Integer year) {

        // --------------------------------------------------------
        // 1. Get published merit list
        // --------------------------------------------------------

        List<MeritList> meritStudents =
                meritListRepository
                        .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                                gender, branch, year)
                        .stream()
                        .filter(MeritList::isPublished)
                        .sorted(Comparator.comparing(MeritList::getMeritRank))
                        .collect(Collectors.toList());

        if (meritStudents.isEmpty()) {
            return new ArrayList<>();
        }

        // --------------------------------------------------------
        // 2. Delete old allotments for same gender/branch/year
        // --------------------------------------------------------

        List<Allotment> oldAllotments =
                allotmentRepository
                        .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                                gender, branch, year);

        if (!oldAllotments.isEmpty()) {
            allotmentRepository.deleteAll(oldAllotments);
        }

        // --------------------------------------------------------
        // 3. Separate boys / girls logic
        // --------------------------------------------------------

        List<Allotment> result;

        if ("MALE".equalsIgnoreCase(gender)
                || "BOYS".equalsIgnoreCase(gender)
                || "M".equalsIgnoreCase(gender)) {

            result = generateBoysAllotment(
                    meritStudents, gender, branch, year);

        } else {

            result = generateGirlsAllotment(
                    meritStudents, gender, branch, year);
        }

        // --------------------------------------------------------
        // 4. Save allotments
        // --------------------------------------------------------

        List<Allotment> saved =
                allotmentRepository.saveAll(result);

        // --------------------------------------------------------
        // 5. Send emails only to allotted students
        // --------------------------------------------------------

        for (Allotment allotment : saved) {

            if ("ALLOTTED".equalsIgnoreCase(
                    allotment.getAllotmentStatus())) {

                try {
                    emailService.sendAllotmentEmail(allotment);
                } catch (Exception e) {
                    System.out.println(
                            "Email failed for allotment: "
                                    + allotment.getId());
                }
            }
        }

        return saved;
    }

    // ============================================================
    // BOYS ALLOTMENT
    // ============================================================

    private List<Allotment> generateBoysAllotment(
            List<MeritList> students,
            String gender,
            String branch,
            Integer year) {

        List<Allotment> allotments = new ArrayList<>();

        Set<Long> alreadyAllotted = new HashSet<>();

        // ========================================================
        // BOYS SEATS
        //
        // OPEN/SEBC = 6
        // OBC/SBC   = 2
        // SC/ST     = 2
        // NT        = 1
        //
        // TOTAL = 11
        // ========================================================

        // --------------------------------------------------------
        // STEP 1: OPEN/SEBC - 6 SEATS
        //
        // IMPORTANT:
        // Any category can take OPEN/SEBC seat based on merit.
        // --------------------------------------------------------

        int openSeats = 6;

        for (MeritList merit : students) {

            if (openSeats <= 0) {
                break;
            }

            if (merit.getApplication() == null) {
                continue;
            }

            Allotment allotment =
                    createAllotment(
                            merit,
                            gender,
                            branch,
                            year,
                            "OPEN/SEBC",
                            openSeats);

            allotments.add(allotment);

            alreadyAllotted.add(merit.getId());

            openSeats--;
        }

        // --------------------------------------------------------
        // STEP 2: OBC/SBC - 2 SEATS
        //
        // Only remaining OBC/SBC students.
        // --------------------------------------------------------

        int obcSeats = 2;

        for (MeritList merit : students) {

            if (obcSeats <= 0) {
                break;
            }

            if (alreadyAllotted.contains(merit.getId())) {
                continue;
            }

            if (!isOBCorSBC(merit.getCategory())) {
                continue;
            }

            Allotment allotment =
                    createAllotment(
                            merit,
                            gender,
                            branch,
                            year,
                            "OBC/SBC",
                            obcSeats);

            allotments.add(allotment);

            alreadyAllotted.add(merit.getId());

            obcSeats--;
        }

        // --------------------------------------------------------
        // STEP 3: SC/ST - 2 SEATS
        // --------------------------------------------------------

        int scStSeats = 2;

        for (MeritList merit : students) {

            if (scStSeats <= 0) {
                break;
            }

            if (alreadyAllotted.contains(merit.getId())) {
                continue;
            }

            if (!isSCorST(merit.getCategory())) {
                continue;
            }

            Allotment allotment =
                    createAllotment(
                            merit,
                            gender,
                            branch,
                            year,
                            "SC/ST",
                            scStSeats);

            allotments.add(allotment);

            alreadyAllotted.add(merit.getId());

            scStSeats--;
        }

        // --------------------------------------------------------
        // STEP 4: VJ/NT-B/NT-C/NT-D - 1 SEAT
        // --------------------------------------------------------

        int ntSeats = 1;

        for (MeritList merit : students) {

            if (ntSeats <= 0) {
                break;
            }

            if (alreadyAllotted.contains(merit.getId())) {
                continue;
            }

            if (!isNTCategory(merit.getCategory())) {
                continue;
            }

            Allotment allotment =
                    createAllotment(
                            merit,
                            gender,
                            branch,
                            year,
                            "VJ/NT-B/NT-C/NT-D",
                            ntSeats);

            allotments.add(allotment);

            alreadyAllotted.add(merit.getId());

            ntSeats--;
        }

        // --------------------------------------------------------
        // STEP 5: WAITING LIST
        // --------------------------------------------------------

        int waitingNumber = 1;

        for (MeritList merit : students) {

            if (alreadyAllotted.contains(merit.getId())) {
                continue;
            }

            if (merit.getApplication() == null) {
                continue;
            }

            Allotment waiting = new Allotment();

            waiting.setApplication(merit.getApplication());
            waiting.setMeritList(merit);
            waiting.setGender(gender);
            waiting.setBranch(branch);
            waiting.setYear(year);
            waiting.setCategory(merit.getCategory());
            waiting.setAllotmentCategory("WAITING");
            waiting.setMeritRank(merit.getMeritRank());
            waiting.setAggregate(merit.getAggregate());
            waiting.setSeatNumber(
                    "B-WAIT-" +
                            String.format("%02d", waitingNumber));
            waiting.setAllotmentStatus("WAITING");

            allotments.add(waiting);

            waitingNumber++;
        }

        return allotments;
    }

    // ============================================================
    // GIRLS ALLOTMENT
    // ============================================================

    private List<Allotment> generateGirlsAllotment(
            List<MeritList> students,
            String gender,
            String branch,
            Integer year) {

        List<Allotment> allotments = new ArrayList<>();

        Set<Long> alreadyAllotted = new HashSet<>();

        // ========================================================
        // GIRLS SEATS
        //
        // OPEN/SEBC = 1
        // OBC/SBC   = 1
        // NT/SC/ST  = 1
        //
        // TOTAL = 3
        // ========================================================

        // --------------------------------------------------------
        // STEP 1: OPEN/SEBC - 1 SEAT
        //
        // Any category can get this seat based on merit.
        // --------------------------------------------------------

        int openSeats = 1;

        for (MeritList merit : students) {

            if (openSeats <= 0) {
                break;
            }

            if (merit.getApplication() == null) {
                continue;
            }

            Allotment allotment =
                    createAllotment(
                            merit,
                            gender,
                            branch,
                            year,
                            "OPEN/SEBC",
                            openSeats);

            allotments.add(allotment);

            alreadyAllotted.add(merit.getId());

            openSeats--;
        }

        // --------------------------------------------------------
        // STEP 2: OBC/SBC - 1 SEAT
        // --------------------------------------------------------

        int obcSeats = 1;

        for (MeritList merit : students) {

            if (obcSeats <= 0) {
                break;
            }

            if (alreadyAllotted.contains(merit.getId())) {
                continue;
            }

            if (!isOBCorSBC(merit.getCategory())) {
                continue;
            }

            Allotment allotment =
                    createAllotment(
                            merit,
                            gender,
                            branch,
                            year,
                            "OBC/SBC",
                            obcSeats);

            allotments.add(allotment);

            alreadyAllotted.add(merit.getId());

            obcSeats--;
        }

        // --------------------------------------------------------
        // STEP 3: VJ/NT-B/NT-C/NT-D / SC/ST - 1 SEAT
        // --------------------------------------------------------

        int againstSeats = 1;

        for (MeritList merit : students) {

            if (againstSeats <= 0) {
                break;
            }

            if (alreadyAllotted.contains(merit.getId())) {
                continue;
            }

            if (!isGirlsAgainstCategory(merit.getCategory())) {
                continue;
            }

            Allotment allotment =
                    createAllotment(
                            merit,
                            gender,
                            branch,
                            year,
                            "VJ/NT-B/NT-C/NT-D / SC/ST",
                            againstSeats);

            allotments.add(allotment);

            alreadyAllotted.add(merit.getId());

            againstSeats--;
        }

        // --------------------------------------------------------
        // STEP 4: WAITING LIST
        // --------------------------------------------------------

        int waitingNumber = 1;

        for (MeritList merit : students) {

            if (alreadyAllotted.contains(merit.getId())) {
                continue;
            }

            if (merit.getApplication() == null) {
                continue;
            }

            Allotment waiting = new Allotment();

            waiting.setApplication(merit.getApplication());
            waiting.setMeritList(merit);
            waiting.setGender(gender);
            waiting.setBranch(branch);
            waiting.setYear(year);
            waiting.setCategory(merit.getCategory());
            waiting.setAllotmentCategory("WAITING");
            waiting.setMeritRank(merit.getMeritRank());
            waiting.setAggregate(merit.getAggregate());
            waiting.setSeatNumber(
                    "G-WAIT-" +
                            String.format("%02d", waitingNumber));
            waiting.setAllotmentStatus("WAITING");

            allotments.add(waiting);

            waitingNumber++;
        }

        return allotments;
    }

    // ============================================================
    // CREATE ALLOTMENT
    // ============================================================

    private Allotment createAllotment(
            MeritList merit,
            String gender,
            String branch,
            Integer year,
            String allotmentCategory,
            int seatNumber) {

        Allotment allotment = new Allotment();

        allotment.setApplication(merit.getApplication());
        allotment.setMeritList(merit);

        allotment.setGender(gender);
        allotment.setBranch(branch);
        allotment.setYear(year);

        // Keep student's ORIGINAL category
        allotment.setCategory(merit.getCategory());

        allotment.setAllotmentCategory(allotmentCategory);

        allotment.setMeritRank(merit.getMeritRank());
        allotment.setAggregate(merit.getAggregate());

        // --------------------------------------------------------
        // Seat Number
        // --------------------------------------------------------

        String prefix =
                isBoys(gender) ? "B" : "G";

        String categoryCode =
                getCategoryCode(allotmentCategory);

        allotment.setSeatNumber(
                prefix
                        + "-"
                        + branch
                        + "-Y"
                        + year
                        + "-"
                        + categoryCode
                        + "-"
                        + String.format("%02d", seatNumber)
        );

        allotment.setAllotmentStatus("ALLOTTED");

        return allotment;
    }

    // ============================================================
    // CATEGORY CHECKS
    // ============================================================

    private boolean isOBCorSBC(String category) {

        if (category == null) {
            return false;
        }

        String c = category
                .trim()
                .toUpperCase();

        return c.equals("OBC")
                || c.equals("SBC");
    }

    // ------------------------------------------------------------

    private boolean isSCorST(String category) {

        if (category == null) {
            return false;
        }

        String c = category
                .trim()
                .toUpperCase();

        return c.equals("SC")
                || c.equals("ST");
    }

    // ------------------------------------------------------------

    private boolean isNTCategory(String category) {

        if (category == null) {
            return false;
        }

        String c = category
                .trim()
                .toUpperCase();

        return c.equals("VJ")
                || c.equals("VJ-A")
                || c.equals("DT")
                || c.equals("NT")
                || c.equals("NT-1")
                || c.equals("NT-2")
                || c.equals("NT-3")
                || c.equals("NT-4")
                || c.equals("NT-A")
                || c.equals("NT-B")
                || c.equals("NT-C")
                || c.equals("NT-D")
                || c.contains("VJ")
                || c.contains("NT-1")
                || c.contains("NT-2")
                || c.contains("NT-3")
                || c.contains("NT-4")
                || c.contains("NT-A")
                || c.contains("NT-B")
                || c.contains("NT-C")
                || c.contains("NT-D");
    }

    // ------------------------------------------------------------

    private boolean isGirlsAgainstCategory(String category) {

        return isNTCategory(category)
                || isSCorST(category);
    }

    // ============================================================
    // CATEGORY CODE FOR SEAT NUMBER
    // ============================================================

    private String getCategoryCode(String allotmentCategory) {

        if (allotmentCategory == null) {
            return "GENERAL";
        }

        if (allotmentCategory.equalsIgnoreCase("OPEN/SEBC")) {
            return "OPEN-SEBC";
        }

        if (allotmentCategory.equalsIgnoreCase("OBC/SBC")) {
            return "OBC-SBC";
        }

        if (allotmentCategory.equalsIgnoreCase("SC/ST")) {
            return "SC-ST";
        }

        if (allotmentCategory
                .equalsIgnoreCase("VJ/NT-B/NT-C/NT-D")) {
            return "VJ-NT";
        }

        if (allotmentCategory
                .equalsIgnoreCase(
                        "VJ/NT-B/NT-C/NT-D / SC/ST")) {
            return "AGAINST";
        }

        return "GENERAL";
    }

    // ============================================================
    // GENDER CHECK
    // ============================================================

    private boolean isBoys(String gender) {

        return "MALE".equalsIgnoreCase(gender)
                || "BOYS".equalsIgnoreCase(gender)
                || "M".equalsIgnoreCase(gender);
    }

    // ============================================================
    // ACCEPT ALLOTMENT
    // ============================================================

    public Allotment acceptAllotment(Long allotmentId) {

        Allotment allotment =
                allotmentRepository
                        .findById(allotmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Allotment not found"));

        if ("WAITING".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Waiting list student cannot accept a seat");
        }

        if ("REJECTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Rejected allotment cannot be accepted");
        }

        if ("ACCEPTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Allotment is already accepted");
        }

        allotment.setAllotmentStatus("ACCEPTED");

        return allotmentRepository.save(allotment);
    }

    // ============================================================
    // REJECT ALLOTMENT
    // ============================================================

    public Allotment rejectAllotment(Long allotmentId) {

        Allotment allotment =
                allotmentRepository
                        .findById(allotmentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Allotment not found"));

        if ("WAITING".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Waiting list student cannot reject a seat");
        }

        if ("ACCEPTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Accepted allotment cannot be rejected");
        }

        if ("REJECTED".equalsIgnoreCase(
                allotment.getAllotmentStatus())) {

            throw new RuntimeException(
                    "Allotment is already rejected");
        }

        allotment.setAllotmentStatus("REJECTED");

        return allotmentRepository.save(allotment);
    }

    // ============================================================
    // GET ALLOTMENT BY USER
    // ============================================================

    public Optional<Allotment> getAllotmentByUserId(Long userId) {

        return allotmentRepository
                .findByApplication_User_Id(userId);
    }
}
