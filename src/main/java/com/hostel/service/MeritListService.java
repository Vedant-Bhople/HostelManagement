package com.hostel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class MeritListService {

    private static final Logger logger = LoggerFactory.getLogger(MeritListService.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MeritListRepository meritListRepository;

    @Autowired
    private AllotmentRepository allotmentRepository;


    // =====================================================
    // 1. GENERATE COMMON MERIT LIST (FOR THE WHOLE YEAR)
    // Common Ranking: Boys + Girls together
    // =====================================================

    @Transactional
    public List<MeritList> generateCommonMeritList(String year, String admissionYear) {
        if (year == null || year.trim().isEmpty()) {
            year = "3"; // Default academic year if not passed
        }
        final String effectiveYear = year.trim();

        logger.info("Generating common merit list for Year {} (Admission Year: {})", effectiveYear, admissionYear);

        // Fetch all approved applications for the given academic year
        List<Application> applications = applicationRepository.findByYearAndStatus(effectiveYear, "APPROVED");

        if (admissionYear != null && !admissionYear.trim().isEmpty() && !"ALL".equalsIgnoreCase(admissionYear.trim())) {
            final String filterAdmYear = admissionYear.trim();
            applications = applications.stream()
                    .filter(a -> filterAdmYear.equalsIgnoreCase(a.getAdmissionYear()))
                    .collect(Collectors.toList());
        }

        if (applications == null || applications.isEmpty()) {
            throw new RuntimeException("No approved applications found for Year " + effectiveYear
                    + (admissionYear != null && !admissionYear.isEmpty() && !"ALL".equalsIgnoreCase(admissionYear)
                        ? " and Admission Year " + admissionYear : ""));
        }

        // -------------------------------------------------
        // DETERMINISTIC MULTI-TIER TIE-BREAKING SORTING
        // 1. aggregate DESC
        // 2. sem2Percentage DESC
        // 3. sem1Percentage DESC
        // 4. id ASC
        // -------------------------------------------------
        applications.sort(
                Comparator.comparing(Application::getAggregate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Application::getSem2Percentage, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Application::getSem1Percentage, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Application::getId, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        // Delete existing merit list and allotments for this year to prevent duplicates
        List<MeritList> oldMeritLists = meritListRepository.findByYearOrderByMeritRankAsc(effectiveYear);
        if (oldMeritLists != null && !oldMeritLists.isEmpty()) {
            for (MeritList oldMerit : oldMeritLists) {
                List<Allotment> oldAllotments = allotmentRepository.findByMeritListId(oldMerit.getId());
                if (oldAllotments != null && !oldAllotments.isEmpty()) {
                    allotmentRepository.deleteAll(oldAllotments);
                }
            }
            meritListRepository.deleteAll(oldMeritLists);
            meritListRepository.flush();
        }

        // Create unified common merit list
        List<MeritList> meritList = new ArrayList<>();
        int rank = 1;

        for (Application app : applications) {
            MeritList merit = new MeritList();
            merit.setApplication(app);
            merit.setMeritRank(rank);
            merit.setEnrollmentNo(app.getEnrollmentNumber());
            merit.setStudentName(app.getFullName());
            merit.setGender(app.getGender());
            merit.setBranch(app.getBranch());
            merit.setYear(app.getYear());

            // Actual Category & Normalized Quota Category
            merit.setCategory(app.getCategory());
            merit.setMeritCategory(CategoryNormalizer.normalize(app.getCategory()));

            merit.setAggregate(app.getAggregate());
            merit.setAtktStatus(app.getAtktStatus() != null ? app.getAtktStatus() : "NO");
            merit.setAtktSubjects(app.getAtktSubjects() != null ? app.getAtktSubjects() : 0);
            merit.setMeritStatus("ELIGIBLE");
            merit.setPublished(false);

            meritList.add(merit);
            rank++;
        }

        List<MeritList> saved = meritListRepository.saveAll(meritList);
        logger.info("Successfully generated common merit list for Year {} with {} total students.", effectiveYear, saved.size());
        return saved;
    }


    // =====================================================
    // 2. GET MERIT LIST WITH DYNAMIC DISPLAY FILTERS
    // Filtering by branch/gender only filters display;
    // overall merit rank is strictly preserved!
    // =====================================================

    public List<MeritList> getMeritListWithFilters(
            String year,
            String admissionYear,
            String branch,
            String gender,
            String status,
            Boolean publishedOnly) {

        if (year == null || year.trim().isEmpty() || "ALL".equalsIgnoreCase(year.trim())) {
            year = "3"; // Default academic year
        }

        List<MeritList> list = meritListRepository.findByYearOrderByMeritRankAsc(year.trim());

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        return list.stream()
                .filter(m -> {
                    if (publishedOnly != null && publishedOnly && !m.isPublished()) {
                        return false;
                    }
                    if (branch != null && !branch.trim().isEmpty() && !"ALL".equalsIgnoreCase(branch.trim())) {
                        if (!branch.trim().equalsIgnoreCase(m.getBranch())) {
                            return false;
                        }
                    }
                    if (gender != null && !gender.trim().isEmpty() && !"ALL".equalsIgnoreCase(gender.trim())) {
                        if (!gender.trim().equalsIgnoreCase(m.getGender())) {
                            return false;
                        }
                    }
                    if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                        if (!status.trim().equalsIgnoreCase(m.getMeritStatus())) {
                            return false;
                        }
                    }
                    if (admissionYear != null && !admissionYear.trim().isEmpty() && !"ALL".equalsIgnoreCase(admissionYear.trim())) {
                        if (m.getApplication() != null && m.getApplication().getAdmissionYear() != null) {
                            if (!admissionYear.trim().equalsIgnoreCase(m.getApplication().getAdmissionYear())) {
                                return false;
                            }
                        }
                    }
                    return true;
                })
                .sorted(Comparator.comparing(MeritList::getMeritRank, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }


    // =====================================================
    // 3. BACKWARD COMPATIBLE GENERATE METHOD
    // =====================================================

    @Transactional
    public List<MeritList> generateMeritList(String gender, String branch, String year) {
        // Calls common merit generation and returns the filtered view for backward compatibility
        generateCommonMeritList(year, null);
        return getMeritListWithFilters(year, null, branch, gender, null, null);
    }


    // =====================================================
    // 4. PUBLISH / UNPUBLISH MERIT LIST
    // =====================================================

    @Transactional
    public List<MeritList> publishMeritListForYear(String year, String branch, String gender) {
        if (year == null || year.trim().isEmpty()) {
            year = "3";
        }
        List<MeritList> list = meritListRepository.findByYearOrderByMeritRankAsc(year.trim());

        if (list == null || list.isEmpty()) {
            throw new RuntimeException("Merit list not found for Year " + year);
        }

        for (MeritList merit : list) {
            boolean match = true;
            if (branch != null && !branch.trim().isEmpty() && !"ALL".equalsIgnoreCase(branch.trim())) {
                if (!branch.trim().equalsIgnoreCase(merit.getBranch())) match = false;
            }
            if (gender != null && !gender.trim().isEmpty() && !"ALL".equalsIgnoreCase(gender.trim())) {
                if (!gender.trim().equalsIgnoreCase(merit.getGender())) match = false;
            }
            if (match) {
                merit.setPublished(true);
            }
        }

        return meritListRepository.saveAll(list);
    }

    @Transactional
    public List<MeritList> unpublishMeritListForYear(String year, String branch, String gender) {
        if (year == null || year.trim().isEmpty()) {
            year = "3";
        }
        List<MeritList> list = meritListRepository.findByYearOrderByMeritRankAsc(year.trim());

        if (list == null || list.isEmpty()) {
            throw new RuntimeException("Merit list not found for Year " + year);
        }

        for (MeritList merit : list) {
            boolean match = true;
            if (branch != null && !branch.trim().isEmpty() && !"ALL".equalsIgnoreCase(branch.trim())) {
                if (!branch.trim().equalsIgnoreCase(merit.getBranch())) match = false;
            }
            if (gender != null && !gender.trim().isEmpty() && !"ALL".equalsIgnoreCase(gender.trim())) {
                if (!gender.trim().equalsIgnoreCase(merit.getGender())) match = false;
            }
            if (match) {
                merit.setPublished(false);
            }
        }

        return meritListRepository.saveAll(list);
    }


    // =====================================================
    // 5. EXISTING UTILITY METHODS (FOR BACKWARD COMPATIBILITY)
    // =====================================================

    public List<MeritList> getAllMeritLists() {
        return meritListRepository.findAll();
    }

    public List<MeritList> getMeritList(String gender, String branch, String year) {
        return getMeritListWithFilters(year, null, branch, gender, null, null);
    }

    public List<MeritList> getPublishedMeritList(String gender, String branch, String year) {
        return getMeritListWithFilters(year, null, branch, gender, null, true);
    }

    public List<MeritList> getByGender(String gender) {
        return meritListRepository.findByGenderOrderByMeritRankAsc(gender);
    }

    public List<MeritList> getPublishedByGender(String gender) {
        return meritListRepository.findByGenderAndPublishedTrueOrderByMeritRankAsc(gender);
    }

    @Transactional
    public List<MeritList> publishMeritList(String gender, String branch, String year) {
        return publishMeritListForYear(year, branch, gender);
    }

    @Transactional
    public List<MeritList> unpublishMeritList(String gender, String branch, String year) {
        return unpublishMeritListForYear(year, branch, gender);
    }

    @Transactional
    public MeritList updateMeritStatus(Long meritId, String status) {
        MeritList merit = meritListRepository.findById(meritId)
                .orElseThrow(() -> new RuntimeException("Merit record not found"));

        if (status == null || status.trim().isEmpty()) {
            throw new RuntimeException("Merit status is required");
        }

        merit.setMeritStatus(status.toUpperCase());
        return meritListRepository.save(merit);
    }

    @Transactional
    public void deleteMeritList(String gender, String branch, String year) {
        List<MeritList> meritLists = getMeritListWithFilters(year, null, branch, gender, null, null);
        if (meritLists == null || meritLists.isEmpty()) {
            return;
        }

        for (MeritList merit : meritLists) {
            List<Allotment> allotments = allotmentRepository.findByMeritListId(merit.getId());
            if (allotments != null && !allotments.isEmpty()) {
                allotmentRepository.deleteAll(allotments);
            }
        }
        meritListRepository.deleteAll(meritLists);
    }
}