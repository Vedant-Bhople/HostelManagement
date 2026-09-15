
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

        List<MeritList> students =
                meritListRepository
                        .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                                gender, branch, year)
                        .stream()
                        .filter(MeritList::isPublished)
                        .filter(m -> m.getApplication() != null)
                        .sorted(Comparator.comparing(MeritList::getMeritRank))
                        .collect(Collectors.toList());

        if (students.isEmpty()) {
            return new ArrayList<>();
        }

        // --------------------------------------------------------
        // Delete previous allotment for this branch/year/gender
        // --------------------------------------------------------

        List<Allotment> oldAllotments =
                allotmentRepository
                        .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                                gender, branch, year);

        if (!oldAllotments.isEmpty()) {
            allotmentRepository.deleteAll(oldAllotments);
        }

        List<Allotment> result;

        if (isBoys(gender)) {
            result = generateBoysAllotment(
                    students, gender, branch, year);
        } else {
            result = generateGirlsAllotment(
                    students, gender, branch, year);
        }

        List<Allotment> saved =
                allotmentRepository.saveAll(result);

        // --------------------------------------------------------
        // Send email only to allotted students
        // --------------------------------------------------------

        for (Allotment allotment : saved) {

            if ("ALLOTTED".equalsIgnoreCase(
                    allotment.getAllotmentStatus())) {

                try {
                    emailService.sendAllotmentEmail(allotment);
                } catch (Exception e) {
                    System.out.println(
                            "Unable to send allotment email for: "
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

        Set<Long> allottedStudents = new HashSet<>();

        // ========================================================
        // BOYS:
        //
        // OPEN/SEBC                  = 6
        // OBC/SBC                    = 2
        // SC/ST                      = 2
        // VJ/NT-B/NT-C/NT-D         = 1
        //
        // TOTAL                     = 11
        // ========================================================

        // --------------------------------------------------------
        // STEP 1
        // OPEN/SEBC - 6 SEATS
        //
        // IMPORTANT:
        // ALL categories are eligible.
        // Merit rank decides who gets these seats.
        // --------------------------------------------------------

        int openSeatNo = 1;

        for (MeritList student : students) {

            if (openSeatNo > 6) {
                break;
            }

            Allotment allotment = createAllotment(
                    student,
                    gender,
                    branch,
                    year,
                    "OPEN/SEBC",
                    openSeatNo
            );

            allotments.add(allotment);

            allottedStudents.add(student.getId());

            openSeatNo++;
        }

        // --------------------------------------------------------
        // STEP 2
        // OBC/SBC - 2 SEATS
        //
        // Only remaining OBC/SBC students.
        // --------------------------------------------------------

        int obcSeatNo = 1;

        for (MeritList student : students) {

            if (obcSeatNo > 2) {
                break;
            }

            if (allottedStudents.contains(student.getId())) {
                continue;
            }

            if (!isOBCorSBC(student.getCategory())) {
                continue;
            }

            Allotment allotment = createAllotment(
                    student,
                    gender,
                    branch,
                    year,
                    "OBC/SBC",
                    obcSeatNo
            );

            allotments.add(allotment);

            allottedStudents.add(student.getId());

            obcSeatNo++;
        }

        // --------------------------------------------------------
        // STEP 3
        // SC/ST - 2 SEATS
        // --------------------------------------------------------

        int scStSeatNo = 1;

        for (MeritList student : students) {

            if (scStSeatNo > 2) {
                break;
            }

            if (allottedStudents.contains(student.getId())) {
                continue;
            }

            if (!isSCorST(student.getCategory())) {
                continue;
            }

            Allotment allotment = createAllotment(
                    student,
                    gender,
                    branch,
                    year,
                    "SC/ST",
                    scStSeatNo
            );

            allotments.add(allotment);

            allottedStudents.add(student.getId());

            scStSeatNo++;
        }

        // --------------------------------------------------------
        // STEP 4
        // VJ/NT-B/NT-C/NT-D - 1 SEAT
        // --------------------------------------------------------

        int ntSeatNo = 1;

        for (MeritList student : students) {

            if (ntSeatNo > 1) {
                break;
            }

            if (allottedStudents.contains(student.getId())) {
                continue;
            }

            if (!isNTCategory(student.getCategory())) {
                continue;
            }

            Allotment allotment = createAllotment(
                    student,
                    gender,
                    branch,
                    year,
                    "VJ/NT-B/NT-C/NT-D",
                    ntSeatNo
            );

            allotments.add(allotment);

            allottedStudents.add(student.getId());

            ntSeatNo++;
        }

        // --------------------------------------------------------
        // STEP 5
        // WAITING LIST
        // --------------------------------------------------------

        int waitingNo = 1;

        for (MeritList student : students) {

            if (allottedStudents.contains(student.getId())) {
                continue;
            }

            Allotment waiting = createWaitingAllotment(
                    student,
                    gender,
                    branch,
                    year,
                    waitingNo,
                    "B"
            );

            allotments.add(waiting);

            waitingNo++;
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

        Set<Long> allottedStudents = new HashSet<>();

        // ========================================================
        // GIRLS:
        //
        // OPEN                       = 1
        // OBC/SBC                    = 1
        // VJ/NT-B/NT-C/NT-D/SC/ST  = 1
        //
        // TOTAL                      = 3
        // ========================================================

        // --------------------------------------------------------
        // STEP 1
        // OPEN - 1 SEAT
        //
        // ALL categories can take OPEN based on merit.
        // --------------------------------------------------------

        for (MeritList student : students) {

            Allotment allotment = createAllotment(
                    student,
                    gender,
                    branch,
                    year,
                    "OPEN",
                    1
            );

            allotments.add(allotment);

            allottedStudents.add(student.getId());

            break;
        }

        // --------------------------------------------------------
        // STEP 2
        // OBC/SBC - 1 SEAT
        // --------------------------------------------------------

        for (MeritList student : students) {

            if (allottedStudents.contains(student.getId())) {
                continue;
            }

            if (!isOBCorSBC(student.getCategory())) {
                continue;
            }

            Allotment allotment = createAllotment(
                    student,
                    gender,
                    branch,
                    year,
                    "OBC/SBC",
                    1
            );

            allotments.add(allotment);

            allottedStudents.add(student.getId());

            break;
        }

        // --------------------------------------------------------
        // STEP 3
        // AGAINST VJ/NT/SC/ST - 1 SEAT
        // --------------------------------------------------------

        for (MeritList student : students) {

            if (allottedStudents.contains(student.getId())) {
                continue;
            }

            if (!isGirlsAgainstCategory(student.getCategory())) {
                continue;
            }

            Allotment allotment = createAllotment(
                    student,
                    gender,
                    branch,
                    year,
                    "Against VJ/NT-B/NT-C/NT-D/SC/ST",
                    1
            );

            allotments.add(allotment);

            allottedStudents.add(student.getId());

            break;
        }

        // --------------------------------------------------------
        // STEP 4
        // WAITING LIST
        // --------------------------------------------------------

        int waitingNo = 1;

        for (MeritList student : students) {

            if (allottedStudents.contains(student.getId())) {
                continue;
            }

            Allotment waiting = createWaitingAllotment(
                    student,
                    gender,
                    branch,
                    year,
                    waitingNo,
                    "G"
            );

            allotments.add(waiting);

            waitingNo++;
        }

        return allotments;
    }

    // ============================================================
    // CREATE ALLOTMENT
    // ============================================================

    private Allotment createAllotment(
            MeritList student,
            String gender,
            String branch,
            Integer year,
            String allotmentCategory,
            int seatNo) {

        Allotment allotment = new Allotment();

        allotment.setApplication(student.getApplication());
        allotment.setMeritList(student);

        allotment.setGender(gender);
        allotment.setBranch(branch);
        allotment.setYear(year);

        // Student's ORIGINAL category
        allotment.setCategory(student.getCategory());

        // Seat type allotted
        allotment.setAllotmentCategory(allotmentCategory);

        allotment.setMeritRank(student.getMeritRank());
        allotment.setAggregate(student.getAggregate());

        String prefix = isBoys(gender) ? "B" : "G";

        String seatCode = getSeatCode(allotmentCategory);

        allotment.setSeatNumber(
                prefix
                        + "-"
                        + branch
                        + "-Y"
                        + year
                        + "-"
                        + seatCode
                        + "-"
                        + String.format("%02d", seatNo)
        );

        allotment.setAllotmentStatus("ALLOTTED");

        return allotment;
    }

    // ============================================================
    // CREATE WAITING ALLOTMENT
    // ============================================================

    private Allotment createWaitingAllotment(
            MeritList student,
            String gender,
            String branch,
            Integer year,
            int waitingNo,
            String prefix) {

        Allotment waiting = new Allotment();

        waiting.setApplication(student.getApplication());
        waiting.setMeritList(student);

        waiting.setGender(gender);
        waiting.setBranch(branch);
        waiting.setYear(year);

        waiting.setCategory(student.getCategory());

        waiting.setAllotmentCategory("WAITING");

        waiting.setMeritRank(student.getMeritRank());
        waiting.setAggregate(student.getAggregate());

        waiting.setSeatNumber(
                prefix
                        + "-WAIT-"
                        + String.format("%02d", waitingNo)
        );

        waiting.setAllotmentStatus("WAITING");

        return waiting;
    }

    // ============================================================
    // OBC / SBC
    // ============================================================

    private boolean isOBCorSBC(String category) {

        if (category == null) {
            return false;
        }

        String c = category.trim().toUpperCase();

        return c.equals("OBC")
                || c.equals("SBC");
    }

    // ============================================================
    // SC / ST
    // ============================================================

    private boolean isSCorST(String category) {

        if (category == null) {
            return false;
        }

        String c = category.trim().toUpperCase();

        return c.equals("SC")
                || c.equals("ST");
    }

    // ============================================================
    // VJ / NT CATEGORY
    // ============================================================

    private boolean isNTCategory(String category) {

        if (category == null) {
            return false;
        }

        String c = category
                .trim()
                .toUpperCase()
                .replace(" ", "");

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

    // ============================================================
    // GIRLS AGAINST CATEGORY
    // ============================================================

    private boolean isGirlsAgainstCategory(String category) {

        return isNTCategory(category)
                || isSCorST(category);
    }

    // ============================================================
    // SEAT CODE
    // ============================================================

    private String getSeatCode(String allotmentCategory) {

        if (allotmentCategory == null) {
            return "GENERAL";
        }

        if (allotmentCategory.equalsIgnoreCase("OPEN")
                || allotmentCategory.equalsIgnoreCase("OPEN/SEBC")) {

            return "OPEN";
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
                        "Against VJ/NT-B/NT-C/NT-D/SC/ST")) {

            return "AGAINST";
        }

        return "GENERAL";
    }

    // ============================================================
    // GENDER
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

