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
    // STAGE 1: GENERATE NORMAL ALLOTMENT
    // =====================================================

    @Transactional
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

        // Check for published students
        List<MeritList> publishedStudents = meritList.stream()
                .filter(MeritList::isPublished)
                .sorted(Comparator.comparing(MeritList::getMeritRank, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MeritList::getAggregate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        if (publishedStudents.isEmpty()) {
            throw new RuntimeException(
                    "Merit list is not published yet for "
                    + gender + " - "
                    + branch + " - "
                    + year
            );
        }

        // Delete old allotments for this wing/branch/year
        List<Allotment> oldAllotments =
                allotmentRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );

        if (oldAllotments != null && !oldAllotments.isEmpty()) {
            allotmentRepository.deleteAll(oldAllotments);
            allotmentRepository.flush();
        }

        List<Allotment> allotments = new ArrayList<>();
        String hostelCode = "BOYS".equalsIgnoreCase(gender) ? "B" : "G";
        List<ReservationQuota> quotas = ReservationPolicy.getQuotasForGender(gender);

        int totalSeatsAllottedCounter = 0;

        // Stage 1: Allocate quota-wise according to reservation policy
        for (ReservationQuota quota : quotas) {
            int capacity = quota.getSeatCapacity();
            int allocatedForThisQuota = 0;

            for (MeritList merit : publishedStudents) {
                if (allocatedForThisQuota >= capacity) {
                    break;
                }

                // Check if already allotted
                if (isAlreadyAllotted(merit, allotments)) {
                    continue;
                }

                String commonCat = CategoryNormalizer.normalize(merit.getCategory());

                // Check eligibility for this quota
                if (quota.isEligible(commonCat)) {
                    totalSeatsAllottedCounter++;
                    allocatedForThisQuota++;

                    Allotment allotment = createAllotmentRecord(
                            merit,
                            quota.getName(),
                            hostelCode,
                            branch,
                            year,
                            quota.getSeatCategoryCode(),
                            allocatedForThisQuota,
                            "ALLOTTED",
                            false
                    );

                    allotments.add(allotment);
                }
            }
        }

        // Stage 1: Add remaining unallocated students to WAITING list in strict merit order
        int waitingCounter = 0;
        for (MeritList merit : publishedStudents) {
            if (isAlreadyAllotted(merit, allotments)) {
                continue;
            }

            waitingCounter++;
            Allotment waiting = createWaitingRecord(
                    merit,
                    hostelCode,
                    branch,
                    year,
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

        List<Allotment> currentAllotments =
                allotmentRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );

        if (currentAllotments == null || currentAllotments.isEmpty()) {
            throw new RuntimeException(
                    "No existing allotment found for "
                    + gender + " - " + branch + " - " + year
                    + ". Please run Normal Allotment first."
            );
        }

        // Check if conversion was already performed for this cycle
        boolean alreadyConverted = currentAllotments.stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getIsConverted()));

        if (alreadyConverted) {
            throw new RuntimeException(
                    "Unused reserved seats have already been converted to OPEN for this allotment cycle."
            );
        }

        List<ReservationQuota> quotas = ReservationPolicy.getQuotasForGender(gender);

        // Calculate unused capacity in reserved quotas
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
            throw new RuntimeException(
                    "All reserved seats are currently occupied. There are 0 unused reserved seats to convert."
            );
        }

        // Find WAITING students sorted strictly by meritRank ASC
        List<Allotment> waitingStudents = currentAllotments.stream()
                .filter(a -> "WAITING".equalsIgnoreCase(a.getAllotmentStatus()))
                .sorted(Comparator.comparing(Allotment::getMeritRank, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Allotment::getAggregate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        if (waitingStudents.isEmpty()) {
            throw new RuntimeException(
                    "There are " + totalUnusedReservedSeats
                    + " unused reserved seats, but no waiting students in the queue to allocate."
            );
        }

        // Determine starting index for converted OPEN seat numbering
        long currentOpenSeatsCount = currentAllotments.stream()
                .filter(a -> a.getAllotmentCategory() != null && a.getAllotmentCategory().toUpperCase().contains("OPEN"))
                .filter(a -> !"WAITING".equalsIgnoreCase(a.getAllotmentStatus()))
                .count();

        int openSeatIndex = (int) currentOpenSeatsCount;
        String hostelCode = "BOYS".equalsIgnoreCase(gender) ? "B" : "G";
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
                            branch,
                            year,
                            "OP",
                            openSeatIndex
                    )
            );

            newlyAllotted.add(studentToAllot);
        }

        // Save updated allotment state
        List<Allotment> updatedAllotments = allotmentRepository.saveAll(currentAllotments);

        // Send email notifications to converted students
        try {
            for (Allotment item : newlyAllotted) {
                emailService.sendAllotmentEmail(item);
            }
        } catch (Exception e) {
            System.out.println("Email notification notice: " + e.getMessage());
        }

        return updatedAllotments;
    }


    // =====================================================
    // GET ALLOTMENT SUMMARY & RESERVATION BREAKDOWN
    // =====================================================

    public Map<String, Object> getAllotmentSummary(
            String gender,
            String branch,
            String year) {

        List<Allotment> allotments =
                allotmentRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );

        int totalCapacity = ReservationPolicy.getTotalCapacity(gender);
        int reservedCapacity = ReservationPolicy.getReservedCapacity(gender);
        int openCapacity = ReservationPolicy.getOpenCapacity(gender);

        List<ReservationQuota> quotas = ReservationPolicy.getQuotasForGender(gender);

        boolean isConverted = false;
        int allottedSeats = 0;
        int acceptedSeats = 0;
        int waitingCount = 0;

        List<Map<String, Object>> quotaBreakdown = new ArrayList<>();

        if (allotments != null && !allotments.isEmpty()) {
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

            for (ReservationQuota quota : quotas) {
                long occupied = allotments.stream()
                        .filter(a -> quota.getName().equalsIgnoreCase(a.getAllotmentCategory()))
                        .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus())
                                || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()))
                        .count();

                int unused = (int) Math.max(0, quota.getSeatCapacity() - occupied);

                Map<String, Object> qMap = new HashMap<>();
                qMap.put("quotaName", quota.getName());
                qMap.put("capacity", quota.getSeatCapacity());
                qMap.put("occupied", occupied);
                qMap.put("unused", unused);
                qMap.put("isReserved", quota.isReserved());
                quotaBreakdown.add(qMap);
            }
        }

        int unusedReservedSeats = 0;
        for (Map<String, Object> q : quotaBreakdown) {
            if (Boolean.TRUE.equals(q.get("isReserved"))) {
                unusedReservedSeats += (int) q.get("unused");
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("gender", gender);
        summary.put("branch", branch);
        summary.put("year", year);
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
                return "GEN";
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
