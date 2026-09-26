package com.hostel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.model.Allotment;
import com.hostel.model.Application;
import com.hostel.model.MeritList;
import com.hostel.repository.AllotmentRepository;
import com.hostel.repository.ApplicationRepository;
import com.hostel.repository.MeritListRepository;
import com.hostel.service.reservation.CategoryNormalizer;
import com.hostel.service.reservation.ReservationPolicy;
import com.hostel.service.reservation.ReservationQuota;

@Service
public class AllotmentService {

    @Autowired
    private MeritListRepository meritListRepository;

    @Autowired
    private AllotmentRepository allotmentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EmailService emailService;


    // =====================================================
    // STAGE 1: GENERATE ALLOTMENT (GENDER + YEAR or GENDER + BRANCH + YEAR)
    // =====================================================

    @Transactional
    public List<Allotment> generateAllotment(
            String gender,
            String branch,
            String year) {

        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";
        String effectiveYear = (year != null && !year.trim().isEmpty()) ? year.trim() : "1";

        if (branch == null || branch.trim().isEmpty() || "ALL".equalsIgnoreCase(branch.trim())) {
            List<Allotment> allBranchAllotments = new ArrayList<>();
            for (String b : ReservationPolicy.ALL_BRANCHES) {
                List<Allotment> bList = generateBranchAllotment(normalizedGender, b, effectiveYear);
                allBranchAllotments.addAll(bList);
            }
            if (allBranchAllotments.isEmpty()) {
                throw new RuntimeException(
                        "No published merit list found for any branch in "
                        + normalizedGender + " - Year " + effectiveYear
                        + ". Please generate and publish the merit list first."
                );
            }
            return allBranchAllotments;
        }

        List<Allotment> singleBranchList = generateBranchAllotment(normalizedGender, branch.trim().toUpperCase(), effectiveYear);
        if (singleBranchList.isEmpty()) {
            throw new RuntimeException(
                    "Merit list not published or no eligible applicants found for "
                    + normalizedGender + " - " + branch + " - Year " + effectiveYear
            );
        }
        return singleBranchList;
    }


    // =====================================================
    // STAGE 1 HELPER: GENERATE ALLOTMENT FOR SINGLE BRANCH
    // =====================================================

    @Transactional
    public List<Allotment> generateBranchAllotment(
            String gender,
            String branch,
            String year) {

        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";
        String normalizedBranch = branch.trim().toUpperCase();
        String effectiveYear = (year != null && !year.trim().isEmpty()) ? year.trim() : "1";

        List<MeritList> meritList =
                meritListRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        normalizedGender,
                        normalizedBranch,
                        effectiveYear
                );

        // Filter published students in strict merit rank order
        List<MeritList> publishedStudents = (meritList != null) ? meritList.stream()
                .filter(MeritList::isPublished)
                .sorted(Comparator.comparing(MeritList::getMeritRank, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MeritList::getAggregate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList()) : new ArrayList<>();

        // Delete old regular allotments for this wing/branch/year
        List<Allotment> oldAllotments =
                allotmentRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        normalizedGender,
                        normalizedBranch,
                        effectiveYear
                );

        if (oldAllotments != null && !oldAllotments.isEmpty()) {
            allotmentRepository.deleteAll(oldAllotments);
            allotmentRepository.flush();
        }

        if (publishedStudents.isEmpty()) {
            return new ArrayList<>();
        }

        List<Allotment> allotments = new ArrayList<>();
        String hostelCode = "BOYS".equalsIgnoreCase(normalizedGender) ? "B" : "G";
        List<ReservationQuota> quotas = ReservationPolicy.getQuotasForGender(normalizedGender);

        // -------------------------------------------------
        // STEP 1: ALLOCATE OPEN SEATS BASED PURELY ON MERIT
        // OPEN seats are open to ALL students regardless of category
        // -------------------------------------------------
        ReservationQuota openQuota = quotas.stream()
                .filter(q -> !q.isReserved())
                .findFirst()
                .orElse(new ReservationQuota("OPEN", "BOYS".equalsIgnoreCase(normalizedGender) ? 6 : 1, false, "OP", CategoryNormalizer.OPEN));

        int openCapacity = openQuota.getSeatCapacity();
        int openAllottedCount = 0;

        for (MeritList merit : publishedStudents) {
            if (openAllottedCount >= openCapacity) {
                break;
            }

            openAllottedCount++;
            Allotment allotment = createAllotmentRecord(
                    merit,
                    "OPEN",
                    hostelCode,
                    normalizedBranch,
                    effectiveYear,
                    "OP",
                    openAllottedCount,
                    "ALLOTTED",
                    false
            );

            allotments.add(allotment);
        }

        // -------------------------------------------------
        // STEP 2: ALLOCATE RESERVED SEATS
        // To eligible remaining students who did NOT receive OPEN
        // -------------------------------------------------
        for (ReservationQuota quota : quotas) {
            if (!quota.isReserved()) {
                continue;
            }

            int capacity = quota.getSeatCapacity();
            int allocatedForThisQuota = 0;

            for (MeritList merit : publishedStudents) {
                if (allocatedForThisQuota >= capacity) {
                    break;
                }

                // Check if already allotted in OPEN or previous reserved quota
                if (isAlreadyAllotted(merit, allotments)) {
                    continue;
                }

                String commonCat = CategoryNormalizer.normalize(merit.getCategory());

                // Check eligibility for this reserved quota
                if (quota.isEligible(commonCat)) {
                    allocatedForThisQuota++;

                    Allotment allotment = createAllotmentRecord(
                            merit,
                            quota.getName(),
                            hostelCode,
                            normalizedBranch,
                            effectiveYear,
                            quota.getSeatCategoryCode(),
                            allocatedForThisQuota,
                            "ALLOTTED",
                            false
                    );

                    allotments.add(allotment);
                }
            }
            // Note: If allocatedForThisQuota < capacity, the remaining reserved seats stay VACANT!
        }

        // -------------------------------------------------
        // STEP 3: REMAINING STUDENTS -> WAITING LIST
        // -------------------------------------------------
        int waitingCounter = 0;
        for (MeritList merit : publishedStudents) {
            if (isAlreadyAllotted(merit, allotments)) {
                continue;
            }

            waitingCounter++;
            Allotment waiting = createWaitingRecord(
                    merit,
                    hostelCode,
                    normalizedBranch,
                    effectiveYear,
                    waitingCounter
            );

            allotments.add(waiting);
        }

        // Save all allotments to database
        List<Allotment> savedAllotments = allotmentRepository.saveAll(allotments);

        // Send Email notifications asynchronously / safely
        try {
            for (Allotment item : savedAllotments) {
                if ("ALLOTTED".equalsIgnoreCase(item.getAllotmentStatus())) {
                    emailService.sendAllotmentEmail(item);
                }
            }
        } catch (Exception e) {
            System.out.println("Email notification notice: " + e.getMessage());
        }

        return savedAllotments;
    }


    // =====================================================
    // STAGE 2: CONVERT UNUSED RESERVED SEATS TO OPEN
    // =====================================================

    @Transactional
    public List<Allotment> convertUnusedReservedSeats(
            String gender,
            String branch,
            String year) {

        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";
        String effectiveYear = (year != null && !year.trim().isEmpty()) ? year.trim() : "1";

        List<String> branchesToProcess = new ArrayList<>();
        if (branch == null || branch.trim().isEmpty() || "ALL".equalsIgnoreCase(branch.trim())) {
            branchesToProcess.addAll(ReservationPolicy.ALL_BRANCHES);
        } else {
            branchesToProcess.add(branch.trim().toUpperCase());
        }

        List<Allotment> totalUpdatedAllotments = new ArrayList<>();
        int totalConvertedCount = 0;

        for (String b : branchesToProcess) {
            List<Allotment> currentAllotments =
                    allotmentRepository
                    .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                            normalizedGender,
                            b,
                            effectiveYear
                    );

            if (currentAllotments == null || currentAllotments.isEmpty()) {
                continue;
            }

            // Check if conversion was already performed for this branch cycle
            boolean alreadyConverted = currentAllotments.stream()
                    .anyMatch(a -> Boolean.TRUE.equals(a.getIsConverted()));

            if (alreadyConverted) {
                totalUpdatedAllotments.addAll(currentAllotments);
                continue;
            }

            List<ReservationQuota> quotas = ReservationPolicy.getQuotasForGender(normalizedGender);

            // Calculate unused capacity in reserved quotas for this branch
            int totalUnusedReservedSeats = 0;
            for (ReservationQuota quota : quotas) {
                if (quota.isReserved()) {
                    long occupiedCount = currentAllotments.stream()
                            .filter(a -> quota.getName().equalsIgnoreCase(a.getAllotmentCategory()))
                            .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus())
                                    || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                            .count();

                    int unusedInQuota = (int) Math.max(0, quota.getSeatCapacity() - occupiedCount);
                    totalUnusedReservedSeats += unusedInQuota;
                }
            }

            if (totalUnusedReservedSeats <= 0) {
                totalUpdatedAllotments.addAll(currentAllotments);
                continue;
            }

            // Find WAITING students sorted strictly by meritRank ASC
            List<Allotment> waitingStudents = currentAllotments.stream()
                    .filter(a -> "WAITING".equalsIgnoreCase(a.getAllotmentStatus()))
                    .sorted(Comparator.comparing(Allotment::getMeritRank, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Allotment::getAggregate, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            if (waitingStudents.isEmpty()) {
                totalUpdatedAllotments.addAll(currentAllotments);
                continue;
            }

            // Determine starting index for converted OPEN seat numbering
            long currentOpenSeatsCount = currentAllotments.stream()
                    .filter(a -> a.getAllotmentCategory() != null && a.getAllotmentCategory().toUpperCase().contains("OPEN"))
                    .filter(a -> !"WAITING".equalsIgnoreCase(a.getAllotmentStatus()))
                    .count();

            int openSeatIndex = (int) currentOpenSeatsCount;
            String hostelCode = "BOYS".equalsIgnoreCase(normalizedGender) ? "B" : "G";
            int seatsToConvert = Math.min(totalUnusedReservedSeats, waitingStudents.size());

            List<Allotment> newlyAllotted = new ArrayList<>();

            for (int i = 0; i < seatsToConvert; i++) {
                Allotment studentToAllot = waitingStudents.get(i);
                openSeatIndex++;

                studentToAllot.setAllotmentStatus("ALLOTTED");
                studentToAllot.setAllotmentCategory("OPEN");
                studentToAllot.setIsConverted(true);
                studentToAllot.setSeatNumber(
                        generateSeatNumber(
                                hostelCode,
                                b,
                                effectiveYear,
                                "OP",
                                openSeatIndex
                        )
                );

                newlyAllotted.add(studentToAllot);
                totalConvertedCount++;
            }

            // Update waiting numbers for remaining waiting students
            int remainingWaitingNumber = 1;
            for (int i = seatsToConvert; i < waitingStudents.size(); i++) {
                Allotment remainingStudent = waitingStudents.get(i);
                remainingStudent.setSeatNumber(String.format("WAITING-%02d", remainingWaitingNumber++));
            }

            // Save updated allotment state for this branch
            List<Allotment> updated = allotmentRepository.saveAll(currentAllotments);
            totalUpdatedAllotments.addAll(updated);

            // Send email notifications to converted students
            try {
                for (Allotment item : newlyAllotted) {
                    emailService.sendAllotmentEmail(item);
                }
            } catch (Exception e) {
                System.out.println("Email notification notice: " + e.getMessage());
            }
        }

        if (totalConvertedCount == 0 && totalUpdatedAllotments.isEmpty()) {
            throw new RuntimeException(
                    "No regular allotments found to convert for "
                    + normalizedGender + " - Year " + effectiveYear
                    + ". Please run Normal Allotment first."
            );
        }

        return totalUpdatedAllotments;
    }


    // =====================================================
    // GET ALLOTMENT SUMMARY & RESERVATION BREAKDOWN
    // =====================================================

    public Map<String, Object> getAllotmentSummary(
            String gender,
            String branch,
            String year) {

        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";
        String effectiveYear = (year != null && !year.trim().isEmpty()) ? year.trim() : "1";
        boolean isAllBranches = (branch == null || branch.trim().isEmpty() || "ALL".equalsIgnoreCase(branch.trim()));

        List<Allotment> allotments;
        if (isAllBranches) {
            allotments = allotmentRepository.findByGenderAndYearOrderByMeritRankAsc(normalizedGender, effectiveYear);
        } else {
            allotments = allotmentRepository.findByGenderAndBranchAndYearOrderByMeritRankAsc(normalizedGender, branch.trim().toUpperCase(), effectiveYear);
        }

        if (allotments != null) {
            allotments = allotments.stream()
                    .filter(a -> !"SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                    .collect(Collectors.toList());
        } else {
            allotments = new ArrayList<>();
        }

        int multiplier = isAllBranches ? ReservationPolicy.ALL_BRANCHES.size() : 1;
        int totalCapacity = ReservationPolicy.getTotalCapacity(normalizedGender) * multiplier;
        int reservedCapacity = ReservationPolicy.getReservedCapacity(normalizedGender) * multiplier;
        int openCapacity = ReservationPolicy.getOpenCapacity(normalizedGender) * multiplier;

        List<ReservationQuota> quotas = ReservationPolicy.getQuotasForGender(normalizedGender);

        boolean isConverted = false;
        int allottedSeats = 0;
        int acceptedSeats = 0;
        int waitingCount = 0;

        if (!allotments.isEmpty()) {
            for (Allotment a : allotments) {
                if (Boolean.TRUE.equals(a.getIsConverted())) {
                    isConverted = true;
                }
                String status = a.getAllotmentStatus();
                if ("ALLOTTED".equalsIgnoreCase(status) || "ACCEPTED".equalsIgnoreCase(status)) {
                    allottedSeats++;
                }
                if ("ACCEPTED".equalsIgnoreCase(status)) {
                    acceptedSeats++;
                }
                if ("WAITING".equalsIgnoreCase(status)) {
                    waitingCount++;
                }
            }
        }

        List<Map<String, Object>> quotaBreakdown = new ArrayList<>();
        int unusedReservedSeats = 0;

        for (ReservationQuota quota : quotas) {
            int quotaCap = quota.getSeatCapacity() * multiplier;
            long occupied = allotments.stream()
                    .filter(a -> quota.getName().equalsIgnoreCase(a.getAllotmentCategory()))
                    .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus())
                            || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                    .count();

            int unused = (int) Math.max(0, quotaCap - occupied);

            Map<String, Object> qMap = new HashMap<>();
            qMap.put("quotaName", quota.getName());
            qMap.put("capacity", quotaCap);
            qMap.put("occupied", occupied);
            qMap.put("unused", unused);
            qMap.put("isReserved", quota.isReserved());
            quotaBreakdown.add(qMap);

            if (quota.isReserved()) {
                unusedReservedSeats += unused;
            }
        }

        // Branch-wise summary breakdown
        List<Map<String, Object>> branchSummaries = new ArrayList<>();
        if (isAllBranches) {
            for (String b : ReservationPolicy.ALL_BRANCHES) {
                List<Allotment> bAllotments = allotments.stream()
                        .filter(a -> b.equalsIgnoreCase(a.getBranch()))
                        .collect(Collectors.toList());

                int bCapacity = ReservationPolicy.getTotalCapacity(normalizedGender);
                int bAllotted = 0;
                int bWaiting = 0;
                boolean bConverted = false;
                for (Allotment ba : bAllotments) {
                    if (Boolean.TRUE.equals(ba.getIsConverted())) bConverted = true;
                    if ("ALLOTTED".equalsIgnoreCase(ba.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(ba.getAllotmentStatus())) {
                        bAllotted++;
                    }
                    if ("WAITING".equalsIgnoreCase(ba.getAllotmentStatus())) {
                        bWaiting++;
                    }
                }

                int bUnusedReserved = 0;
                for (ReservationQuota q : quotas) {
                    if (q.isReserved()) {
                        long occ = bAllotments.stream()
                                .filter(a -> q.getName().equalsIgnoreCase(a.getAllotmentCategory()))
                                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                                .count();
                        bUnusedReserved += (int) Math.max(0, q.getSeatCapacity() - occ);
                    }
                }

                Map<String, Object> bMap = new HashMap<>();
                bMap.put("branch", b);
                bMap.put("totalCapacity", bCapacity);
                bMap.put("allottedSeats", bAllotted);
                bMap.put("waitingCount", bWaiting);
                bMap.put("unusedReservedSeats", bUnusedReserved);
                bMap.put("isConverted", bConverted);
                branchSummaries.add(bMap);
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("gender", normalizedGender);
        summary.put("branch", isAllBranches ? "ALL" : branch.trim().toUpperCase());
        summary.put("year", effectiveYear);
        summary.put("totalCapacity", totalCapacity);
        summary.put("reservedCapacity", reservedCapacity);
        summary.put("openCapacity", openCapacity);
        summary.put("allottedSeats", allottedSeats);
        summary.put("acceptedSeats", acceptedSeats);
        summary.put("availableSeats", Math.max(0, totalCapacity - allottedSeats));
        summary.put("waitingCount", waitingCount);
        summary.put("unusedReservedSeats", unusedReservedSeats);
        summary.put("isConverted", isConverted);
        summary.put("canConvert", !isConverted && unusedReservedSeats > 0 && waitingCount > 0);
        summary.put("quotaBreakdown", quotaBreakdown);
        summary.put("branchSummaries", branchSummaries);

        return summary;
    }


    // =====================================================
    // HELPER: CREATE ALLOTMENT RECORD
    // =====================================================

    private Allotment createAllotmentRecord(
            MeritList merit,
            String allotmentCategory,
            String hostelCode,
            String branch,
            String year,
            String seatCategoryCode,
            int seatNumber,
            String status,
            boolean isConverted) {

        Allotment allotment = new Allotment();
        allotment.setApplication(merit.getApplication());
        allotment.setMeritList(merit);

        if ("B".equalsIgnoreCase(hostelCode)) {
            allotment.setHostelType("BOYS HOSTEL");
        } else {
            allotment.setHostelType("GIRLS HOSTEL");
        }

        allotment.setGender(merit.getGender());
        allotment.setBranch(branch);
        allotment.setYear(year);

        // Original Category (Permanent & Unchanged)
        allotment.setCategory(merit.getCategory());

        // Normalized Common Category
        allotment.setCommonCategory(CategoryNormalizer.normalize(merit.getCategory()));

        // Allotment Quota
        allotment.setAllotmentCategory(allotmentCategory);

        allotment.setMeritRank(merit.getMeritRank());
        allotment.setAggregate(merit.getAggregate());
        allotment.setAllotmentStatus(status);
        allotment.setIsConverted(isConverted);

        allotment.setSeatNumber(
                generateSeatNumber(
                        hostelCode,
                        branch,
                        year,
                        seatCategoryCode,
                        seatNumber
                )
        );

        return allotment;
    }


    // =====================================================
    // HELPER: CREATE WAITING RECORD
    // =====================================================

    private Allotment createWaitingRecord(
            MeritList merit,
            String hostelCode,
            String branch,
            String year,
            int queuePosition) {

        Allotment waiting = new Allotment();
        waiting.setApplication(merit.getApplication());
        waiting.setMeritList(merit);

        if ("B".equalsIgnoreCase(hostelCode)) {
            waiting.setHostelType("BOYS HOSTEL");
        } else {
            waiting.setHostelType("GIRLS HOSTEL");
        }

        waiting.setGender(merit.getGender());
        waiting.setBranch(branch);
        waiting.setYear(year);
        waiting.setCategory(merit.getCategory());
        waiting.setCommonCategory(CategoryNormalizer.normalize(merit.getCategory()));
        waiting.setAllotmentCategory("WAITING");
        waiting.setMeritRank(merit.getMeritRank());
        waiting.setAggregate(merit.getAggregate());
        waiting.setAllotmentStatus("WAITING");
        waiting.setIsConverted(false);
        waiting.setSeatNumber("WAITING-" + String.format("%02d", queuePosition));

        return waiting;
    }


    // =====================================================
    // HELPER: CHECK IF STUDENT ALREADY ALLOTTED
    // =====================================================

    private boolean isAlreadyAllotted(
            MeritList merit,
            List<Allotment> allotments) {

        for (Allotment allotment : allotments) {
            if (allotment.getMeritList() != null
                    && allotment.getMeritList().getId().equals(merit.getId())) {
                return true;
            }
            if (allotment.getApplication() != null
                    && merit.getApplication() != null
                    && allotment.getApplication().getId().equals(merit.getApplication().getId())) {
                return true;
            }
        }
        return false;
    }


    // =====================================================
    // SEAT NUMBER GENERATION
    // Format: B-COMP-Y1-OP-01 / G-MECH-Y2-OBC-01
    // =====================================================

    private String generateSeatNumber(
            String hostelCode,
            String branch,
            String year,
            String categoryCode,
            int seatNumber) {

        String branchCode = getBranchCode(branch);

        return hostelCode
                + "-"
                + branchCode
                + "-Y"
                + year
                + "-"
                + categoryCode
                + "-"
                + String.format("%02d", seatNumber);
    }


    // =====================================================
    // BRANCH CODE
    // =====================================================

    private String getBranchCode(String branch) {
        if (branch == null) {
            return "OTHER";
        }

        switch (branch.trim().toUpperCase()) {
            case "COMPUTER":
            case "CO":
                return "COMP";
            case "MECHANICAL":
            case "ME":
                return "MECH";
            case "CIVIL":
            case "CE":
                return "CIVIL";
            case "ELECTRICAL":
            case "EE":
                return "ELEC";
            case "INFORMATION TECHNOLOGY":
            case "IT":
            case "IF":
                return "IT";
            default:
                return branch.length() > 4 ? branch.substring(0, 4).toUpperCase() : branch.toUpperCase();
        }
    }


    // =====================================================
    // GET ALLOTMENT
    // =====================================================

    public List<Allotment> getAllotment(
            String gender,
            String branch,
            String year) {

        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";
        String effectiveYear = (year != null && !year.trim().isEmpty()) ? year.trim() : "1";

        if (branch == null || branch.trim().isEmpty() || "ALL".equalsIgnoreCase(branch.trim())) {
            List<Allotment> list = allotmentRepository
                    .findByGenderAndYearOrderByMeritRankAsc(normalizedGender, effectiveYear);
            if (list == null) return new ArrayList<>();
            return list.stream()
                    .filter(a -> !"SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                    .sorted(Comparator.comparing(Allotment::getBranch, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Allotment::getMeritRank, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
        }

        return allotmentRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        normalizedGender,
                        branch.trim().toUpperCase(),
                        effectiveYear
                ).stream()
                .filter(a -> !"SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .collect(Collectors.toList());
    }


    // =====================================================
    // GET STUDENT ALLOTMENTS
    // =====================================================

    public List<Allotment> getStudentAllotments(Long userId) {
        return allotmentRepository.findByApplication_User_Id(userId);
    }


    // =====================================================
    // ACCEPT SEAT
    // =====================================================

    public Allotment acceptSeat(Long allotmentId) {
        Allotment allotment = allotmentRepository.findById(allotmentId)
                .orElseThrow(() -> new RuntimeException("Allotment record not found"));

        if ("REJECTED".equalsIgnoreCase(allotment.getAllotmentStatus())) {
            throw new RuntimeException("This seat has already been rejected");
        }

        if ("WAITING".equalsIgnoreCase(allotment.getAllotmentStatus())) {
            throw new RuntimeException("Waiting list candidate cannot accept a seat until allotted");
        }

        if ("ACCEPTED".equalsIgnoreCase(allotment.getAllotmentStatus())) {
            throw new RuntimeException("Seat is already accepted");
        }

        allotment.setAllotmentStatus("ACCEPTED");
        return allotmentRepository.save(allotment);
    }


    // =====================================================
    // REJECT SEAT
    // =====================================================

    public Allotment rejectSeat(Long allotmentId) {
        Allotment allotment = allotmentRepository.findById(allotmentId)
                .orElseThrow(() -> new RuntimeException("Allotment record not found"));

        if ("ACCEPTED".equalsIgnoreCase(allotment.getAllotmentStatus())) {
            throw new RuntimeException("Accepted seat cannot be rejected");
        }

        if ("REJECTED".equalsIgnoreCase(allotment.getAllotmentStatus())) {
            throw new RuntimeException("Seat is already rejected");
        }

        if ("WAITING".equalsIgnoreCase(allotment.getAllotmentStatus())) {
            throw new RuntimeException("Waiting list student has no allotted seat to reject");
        }

        allotment.setAllotmentStatus("REJECTED");
        return allotmentRepository.save(allotment);
    }


    // =====================================================
    // STAGE 3: GET SPOT ROUND SUMMARY & VACANT POOL
    // =====================================================

    public Map<String, Object> getSpotRoundSummary(String gender) {
        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";
        int totalHostelCapacity = ReservationPolicy.getTotalInstitutionalCapacity(normalizedGender);

        List<Allotment> allAllotments = allotmentRepository.findAll();

        long regularAllottedCount = allAllotments.stream()
                .filter(a -> normalizedGender.equalsIgnoreCase(a.getGender()))
                .filter(a -> !"SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .count();

        long regularConvertedCount = allAllotments.stream()
                .filter(a -> normalizedGender.equalsIgnoreCase(a.getGender()))
                .filter(a -> !"SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> Boolean.TRUE.equals(a.getIsConverted()))
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .count();

        long spotAllottedCount = allAllotments.stream()
                .filter(a -> normalizedGender.equalsIgnoreCase(a.getGender()))
                .filter(a -> "SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .count();

        long spotWaitingCount = allAllotments.stream()
                .filter(a -> normalizedGender.equalsIgnoreCase(a.getGender()))
                .filter(a -> "SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> "WAITING".equalsIgnoreCase(a.getAllotmentStatus()))
                .count();

        long totalOccupied = regularAllottedCount + spotAllottedCount;
        int vacantSpotBeds = (int) Math.max(0, totalHostelCapacity - totalOccupied);

        // Count eligible applicants for this gender
        List<Application> approvedApps = applicationRepository.findByGenderAndStatus(normalizedGender, "APPROVED");

        // Filter those who do NOT have an active REGULAR allotment
        long activeRegularAppCount = allAllotments.stream()
                .filter(a -> normalizedGender.equalsIgnoreCase(a.getGender()))
                .filter(a -> !"SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .map(a -> a.getApplication() != null ? a.getApplication().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .count();

        int eligibleApplicantsCount = (int) Math.max(0, approvedApps.size() - activeRegularAppCount);

        Map<String, Object> summary = new HashMap<>();
        summary.put("gender", normalizedGender);
        summary.put("hostelName", "BOYS".equalsIgnoreCase(normalizedGender) ? "Boys Hostel" : "Girls Hostel");
        summary.put("totalCapacity", totalHostelCapacity);
        summary.put("regularAllottedCount", regularAllottedCount);
        summary.put("regularConvertedCount", regularConvertedCount);
        summary.put("spotAllottedCount", spotAllottedCount);
        summary.put("totalOccupiedSeats", totalOccupied);
        summary.put("availableSpotSeats", vacantSpotBeds);
        summary.put("spotWaitingCount", spotWaitingCount);
        summary.put("totalApprovedApplicants", approvedApps.size());
        summary.put("eligibleApplicantsCount", eligibleApplicantsCount);
        summary.put("isSpotOpen", vacantSpotBeds > 0);

        return summary;
    }


    // =====================================================
    // STAGE 3: GET ELIGIBLE SPOT APPLICANTS QUEUE
    // =====================================================

    public List<Map<String, Object>> getEligibleSpotApplicants(String gender) {
        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";

        List<Application> approvedApps = applicationRepository.findByGenderAndStatus(normalizedGender, "APPROVED");
        List<Allotment> allAllotments = allotmentRepository.findAll();

        Map<Long, Allotment> regularActiveMap = allAllotments.stream()
                .filter(a -> normalizedGender.equalsIgnoreCase(a.getGender()))
                .filter(a -> !"SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .filter(a -> a.getApplication() != null)
                .collect(Collectors.toMap(a -> a.getApplication().getId(), a -> a, (k1, k2) -> k1));

        Map<Long, Allotment> spotAllotmentMap = allAllotments.stream()
                .filter(a -> normalizedGender.equalsIgnoreCase(a.getGender()))
                .filter(a -> "SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> a.getApplication() != null)
                .collect(Collectors.toMap(a -> a.getApplication().getId(), a -> a, (k1, k2) -> k1));

        List<Map<String, Object>> result = new ArrayList<>();

        for (Application app : approvedApps) {
            // Check if already active in regular allotment
            if (regularActiveMap.containsKey(app.getId())) {
                continue; // Student has active regular allotment
            }

            Map<String, Object> item = new HashMap<>();
            item.put("applicationId", app.getId());
            item.put("fullName", app.getFullName());
            item.put("enrollmentNumber", app.getEnrollmentNumber());
            item.put("gender", app.getGender());
            item.put("branch", app.getBranch());
            item.put("year", app.getYear());
            item.put("category", app.getCategory());
            item.put("aggregate", app.getAggregate());
            item.put("mobileNumber", app.getMobileNumber());

            Allotment spot = spotAllotmentMap.get(app.getId());
            if (spot != null) {
                item.put("spotStatus", spot.getAllotmentStatus());
                item.put("seatNumber", spot.getSeatNumber());
                item.put("allotmentId", spot.getId());
            } else {
                item.put("spotStatus", "NOT_PROCESSED");
                item.put("seatNumber", null);
                item.put("allotmentId", null);
            }

            result.add(item);
        }

        // Sort strictly by aggregate DESC, then applicationId ASC
        result.sort((a, b) -> {
            Double aggA = (Double) a.get("aggregate");
            Double aggB = (Double) b.get("aggregate");
            if (aggA == null && aggB == null) return 0;
            if (aggA == null) return 1;
            if (aggB == null) return -1;
            int comp = aggB.compareTo(aggA);
            if (comp != 0) return comp;
            Long idA = (Long) a.get("applicationId");
            Long idB = (Long) b.get("applicationId");
            return idA.compareTo(idB);
        });

        return result;
    }


    // =====================================================
    // STAGE 3: GENERATE COMMON POOL SPOT ROUND ALLOTMENT
    // =====================================================

    @Transactional
    public List<Allotment> generateSpotRoundAllotment(String gender) {
        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";
        int totalHostelCapacity = ReservationPolicy.getTotalInstitutionalCapacity(normalizedGender);

        List<Allotment> allAllotments = allotmentRepository.findAll();

        // Count active regular occupied seats
        long regularOccupied = allAllotments.stream()
                .filter(a -> normalizedGender.equalsIgnoreCase(a.getGender()))
                .filter(a -> !"SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .count();

        int availableVacantBeds = (int) Math.max(0, totalHostelCapacity - regularOccupied);

        if (availableVacantBeds <= 0) {
            throw new RuntimeException(
                    "All " + totalHostelCapacity + " beds in "
                    + ("BOYS".equalsIgnoreCase(normalizedGender) ? "Boys" : "Girls")
                    + " Hostel are currently occupied. There are 0 vacant beds available for Spot Round."
            );
        }

        // Find active regular application IDs
        Map<Long, Boolean> activeRegularIds = allAllotments.stream()
                .filter(a -> normalizedGender.equalsIgnoreCase(a.getGender()))
                .filter(a -> !"SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .filter(a -> a.getApplication() != null)
                .collect(Collectors.toMap(a -> a.getApplication().getId(), a -> true, (k1, k2) -> k1));

        // Fetch approved applications of this gender
        List<Application> approvedApps = applicationRepository.findByGenderAndStatus(normalizedGender, "APPROVED");

        List<Application> eligibleCandidates = approvedApps.stream()
                .filter(app -> !activeRegularIds.containsKey(app.getId()))
                .sorted(Comparator.comparing(Application::getAggregate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Application::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (eligibleCandidates.isEmpty()) {
            throw new RuntimeException(
                    "There are " + availableVacantBeds + " vacant beds in "
                    + ("BOYS".equalsIgnoreCase(normalizedGender) ? "Boys" : "Girls")
                    + " Hostel, but no eligible approved student applications found for Spot Round."
            );
        }

        // Delete existing Spot Round allotments for this gender to recalculate cleanly
        allotmentRepository.deleteByGenderAndAllotmentRound(normalizedGender, "SPOT");
        allotmentRepository.flush();

        List<Allotment> spotAllotments = new ArrayList<>();
        String hostelPrefix = "BOYS".equalsIgnoreCase(normalizedGender) ? "B" : "G";
        String hostelTypeName = "BOYS".equalsIgnoreCase(normalizedGender) ? "BOYS HOSTEL" : "GIRLS HOSTEL";

        int allottedCount = 0;
        int waitingCount = 0;

        for (int i = 0; i < eligibleCandidates.size(); i++) {
            Application app = eligibleCandidates.get(i);

            Allotment spot = new Allotment();
            spot.setApplication(app);
            spot.setHostelType(hostelTypeName);
            spot.setGender(app.getGender());
            spot.setBranch(app.getBranch());
            spot.setYear(app.getYear());
            spot.setCategory(app.getCategory());
            spot.setCommonCategory(CategoryNormalizer.normalize(app.getCategory()));
            spot.setAllotmentRound("SPOT");
            spot.setMeritRank(i + 1);
            spot.setAggregate(app.getAggregate());
            spot.setIsConverted(false);

            if (allottedCount < availableVacantBeds) {
                allottedCount++;
                spot.setAllotmentCategory("SPOT");
                spot.setAllotmentStatus("ALLOTTED");
                spot.setSeatNumber(String.format("%s-SPOT-%02d", hostelPrefix, allottedCount));
            } else {
                waitingCount++;
                spot.setAllotmentCategory("SPOT_WAITING");
                spot.setAllotmentStatus("WAITING");
                spot.setSeatNumber(String.format("SPOT-WAITING-%02d", waitingCount));
            }

            spotAllotments.add(spot);
        }

        List<Allotment> saved = allotmentRepository.saveAll(spotAllotments);

        // Send Email notifications to allotted candidates
        try {
            for (Allotment item : saved) {
                if ("ALLOTTED".equalsIgnoreCase(item.getAllotmentStatus())) {
                    emailService.sendAllotmentEmail(item);
                }
            }
        } catch (Exception e) {
            System.out.println("Spot email notice: " + e.getMessage());
        }

        return saved;
    }


    // =====================================================
    // STAGE 3: GET SPOT ALLOTMENTS LIST
    // =====================================================

    public List<Allotment> getSpotAllotments(String gender) {
        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";
        return allotmentRepository.findByGenderAndAllotmentRoundOrderByAggregateDesc(normalizedGender, "SPOT");
    }


    // =====================================================
    // STAGE 3: ALLOT SINGLE SPOT CANDIDATE (MANUAL / OPTION C)
    // =====================================================

    @Transactional
    public Allotment allotSingleSpotCandidate(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application #" + applicationId + " not found"));

        if (!"APPROVED".equalsIgnoreCase(app.getStatus())) {
            throw new RuntimeException("Application must be in APPROVED status before spot allotment.");
        }

        String gender = "GIRLS".equalsIgnoreCase(app.getGender()) ? "GIRLS" : "BOYS";
        int totalHostelCapacity = ReservationPolicy.getTotalInstitutionalCapacity(gender);

        List<Allotment> allAllotments = allotmentRepository.findAll();

        // Check if student already has active allotment
        boolean alreadyActive = allAllotments.stream()
                .filter(a -> a.getApplication() != null && a.getApplication().getId().equals(app.getId()))
                .anyMatch(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()));

        if (alreadyActive) {
            throw new RuntimeException("Student already holds an active hostel allotment.");
        }

        // Count current occupied seats
        long totalOccupied = allAllotments.stream()
                .filter(a -> gender.equalsIgnoreCase(a.getGender()))
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .count();

        if (totalOccupied >= totalHostelCapacity) {
            throw new RuntimeException("No vacant beds available in " + gender + " hostel.");
        }

        // Check existing spot allotment for this student to update or create new
        Allotment spot = allAllotments.stream()
                .filter(a -> "SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> a.getApplication() != null && a.getApplication().getId().equals(app.getId()))
                .findFirst()
                .orElse(new Allotment());

        String hostelPrefix = "BOYS".equalsIgnoreCase(gender) ? "B" : "G";
        String hostelTypeName = "BOYS".equalsIgnoreCase(gender) ? "BOYS HOSTEL" : "GIRLS HOSTEL";

        long currentSpotAllottedCount = allAllotments.stream()
                .filter(a -> gender.equalsIgnoreCase(a.getGender()))
                .filter(a -> "SPOT".equalsIgnoreCase(a.getAllotmentRound()))
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .count();

        spot.setApplication(app);
        spot.setHostelType(hostelTypeName);
        spot.setGender(app.getGender());
        spot.setBranch(app.getBranch());
        spot.setYear(app.getYear());
        spot.setCategory(app.getCategory());
        spot.setCommonCategory(CategoryNormalizer.normalize(app.getCategory()));
        spot.setAllotmentRound("SPOT");
        spot.setAllotmentCategory("SPOT");
        spot.setAllotmentStatus("ALLOTTED");
        spot.setIsConverted(false);
        spot.setAggregate(app.getAggregate());
        spot.setSeatNumber(String.format("%s-SPOT-%02d", hostelPrefix, (currentSpotAllottedCount + 1)));

        Allotment saved = allotmentRepository.save(spot);

        try {
            emailService.sendAllotmentEmail(saved);
        } catch (Exception e) {
            System.out.println("Spot email notice: " + e.getMessage());
        }

        return saved;
    }
}
