package com.hostel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.model.Allotment;
import com.hostel.model.Application;
import com.hostel.model.MeritList;
import com.hostel.repository.AllotmentRepository;
import com.hostel.repository.ApplicationRepository;
import com.hostel.repository.MeritListRepository;

@Service
public class MeritListService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MeritListRepository meritListRepository;

    @Autowired
    private AllotmentRepository allotmentRepository;


    // =====================================================
    // GENERATE MERIT LIST
    // =====================================================

    @Transactional
    public List<MeritList> generateMeritList(
            String gender,
            String branch,
            String year) {

        // -------------------------------------------------
        // GET APPROVED APPLICATIONS
        // -------------------------------------------------

        List<Application> applications =
                applicationRepository
                .findByGenderAndBranchAndYearAndStatus(
                        gender,
                        branch,
                        year,
                        "APPROVED"
                );

        if (applications == null || applications.isEmpty()) {

            throw new RuntimeException(
                    "No approved applications found for "
                    + gender + " - "
                    + branch + " - "
                    + year
            );
        }


        // -------------------------------------------------
        // SORT BY AGGREGATE DESCENDING
        // -------------------------------------------------

        applications.sort(
                Comparator.comparing(
                        Application::getAggregate,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );


        // =================================================
        // DELETE OLD ALLOTMENTS FIRST
        // =================================================

        List<MeritList> oldMeritLists =
                meritListRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );

        if (oldMeritLists != null && !oldMeritLists.isEmpty()) {

            for (MeritList oldMerit : oldMeritLists) {

                List<Allotment> oldAllotments =
                        allotmentRepository
                        .findByMeritListId(
                                oldMerit.getId()
                        );

                if (oldAllotments != null
                        && !oldAllotments.isEmpty()) {

                    allotmentRepository.deleteAll(
                            oldAllotments
                    );
                }
            }

            // -------------------------------------------------
            // NOW DELETE OLD MERIT LIST
            // -------------------------------------------------

            meritListRepository.deleteAll(
                    oldMeritLists
            );
        }


        // =================================================
        // CREATE NEW MERIT LIST
        // =================================================

        List<MeritList> meritList =
                new ArrayList<>();

        int rank = 1;


        for (Application application : applications) {

            MeritList merit =
                    new MeritList();


            // -------------------------------------------------
            // APPLICATION
            // -------------------------------------------------

            merit.setApplication(
                    application
            );


            // -------------------------------------------------
            // MERIT RANK
            // -------------------------------------------------

            merit.setMeritRank(
                    rank
            );


            // -------------------------------------------------
            // STUDENT INFORMATION
            // -------------------------------------------------

            merit.setEnrollmentNo(
                    application.getEnrollmentNumber()
            );

            merit.setStudentName(
                    application.getFullName()
            );

            merit.setGender(
                    application.getGender()
            );

            merit.setBranch(
                    application.getBranch()
            );

            merit.setYear(
                    application.getYear()
            );


            // -------------------------------------------------
            // CATEGORY
            // -------------------------------------------------

            merit.setCategory(
                    application.getCategory()
            );

            merit.setMeritCategory(
                    convertMeritCategory(
                            application.getCategory()
                    )
            );


            // -------------------------------------------------
            // AGGREGATE
            // -------------------------------------------------

            merit.setAggregate(
                    application.getAggregate()
            );


            // -------------------------------------------------
            // ATKT
            // -------------------------------------------------

            merit.setAtktStatus(
                    application.getAtktStatus()
            );

            merit.setAtktSubjects(
                    application.getAtktSubjects()
            );


            // -------------------------------------------------
            // INITIAL STATUS
            // -------------------------------------------------

            merit.setMeritStatus(
                    "WAITING"
            );


            // -------------------------------------------------
            // NOT PUBLISHED
            // -------------------------------------------------

            merit.setPublished(
                    false
            );


            meritList.add(
                    merit
            );

            rank++;
        }


        // =================================================
        // SAVE NEW MERIT LIST
        // =================================================

        return meritListRepository.saveAll(
                meritList
        );
    }


    // =====================================================
    // CATEGORY CONVERSION
    // =====================================================

    private String convertMeritCategory(
            String category) {

        if (category == null
                || category.trim().isEmpty()) {

            return "OPEN";
        }

        String value =
                category
                .trim()
                .toUpperCase();


        // ST GROUP

        if (value.equals("ST")
                || value.equals("VJ-A")
                || value.equals("NT-B")
                || value.equals("NT-C")
                || value.equals("NT-D")) {

            return "ST";
        }


        // OBC

        if (value.equals("OBC")) {

            return "OBC";
        }


        // SC

        if (value.equals("SC")) {

            return "SC";
        }


        // OPEN + OTHER

        return "OPEN";
    }


    // =====================================================
    // GET ALL MERIT LIST
    // =====================================================

    public List<MeritList> getAllMeritLists() {

        return meritListRepository.findAll();
    }


    // =====================================================
    // GET MERIT LIST
    // =====================================================

    public List<MeritList> getMeritList(
            String gender,
            String branch,
            String year) {

        return meritListRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );
    }


    // =====================================================
    // GET PUBLISHED MERIT LIST
    // =====================================================

    public List<MeritList> getPublishedMeritList(
            String gender,
            String branch,
            String year) {

        return meritListRepository
                .findByGenderAndBranchAndYearAndPublishedTrueOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );
    }


    // =====================================================
    // GET MERIT LIST BY GENDER
    // =====================================================

    public List<MeritList> getByGender(
            String gender) {

        return meritListRepository
                .findByGenderOrderByMeritRankAsc(
                        gender
                );
    }


    // =====================================================
    // GET PUBLISHED MERIT LIST BY GENDER
    // =====================================================

    public List<MeritList> getPublishedByGender(
            String gender) {

        return meritListRepository
                .findByGenderAndPublishedTrueOrderByMeritRankAsc(
                        gender
                );
    }


    // =====================================================
    // PUBLISH MERIT LIST
    // =====================================================

    @Transactional
    public List<MeritList> publishMeritList(
            String gender,
            String branch,
            String year) {

        List<MeritList> list =
                meritListRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );

        if (list == null || list.isEmpty()) {

            throw new RuntimeException(
                    "Merit list not found for "
                    + gender + " - "
                    + branch + " - "
                    + year
            );
        }

        for (MeritList merit : list) {

            merit.setPublished(true);
        }

        return meritListRepository.saveAll(
                list
        );
    }


    // =====================================================
    // UNPUBLISH MERIT LIST
    // =====================================================

    @Transactional
    public List<MeritList> unpublishMeritList(
            String gender,
            String branch,
            String year) {

        List<MeritList> list =
                meritListRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );

        if (list == null || list.isEmpty()) {

            throw new RuntimeException(
                    "Merit list not found"
            );
        }

        for (MeritList merit : list) {

            merit.setPublished(false);
        }

        return meritListRepository.saveAll(
                list
        );
    }


    // =====================================================
    // UPDATE MERIT STATUS
    // =====================================================

    @Transactional
    public MeritList updateMeritStatus(
            Long meritId,
            String status) {

        MeritList merit =
                meritListRepository
                .findById(meritId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Merit record not found"
                        )
                );

        if (status == null
                || status.trim().isEmpty()) {

            throw new RuntimeException(
                    "Merit status is required"
            );
        }

        merit.setMeritStatus(
                status.toUpperCase()
        );

        return meritListRepository.save(
                merit
        );
    }


    // =====================================================
    // DELETE MERIT LIST
    // =====================================================

    @Transactional
    public void deleteMeritList(
            String gender,
            String branch,
            String year) {

        List<MeritList> meritLists =
                meritListRepository
                .findByGenderAndBranchAndYearOrderByMeritRankAsc(
                        gender,
                        branch,
                        year
                );

        if (meritLists == null
                || meritLists.isEmpty()) {

            return;
        }


        // -------------------------------------------------
        // DELETE ALLOTMENTS FIRST
        // -------------------------------------------------

        for (MeritList merit : meritLists) {

            List<Allotment> allotments =
                    allotmentRepository
                    .findByMeritListId(
                            merit.getId()
                    );

            if (allotments != null
                    && !allotments.isEmpty()) {

                allotmentRepository.deleteAll(
                        allotments
                );
            }
        }


        // -------------------------------------------------
        // DELETE MERIT LIST
        // -------------------------------------------------

        meritListRepository.deleteAll(
                meritLists
        );
    }
}