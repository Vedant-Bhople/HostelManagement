package com.hostel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.model.Application;
import com.hostel.model.MeritList;

public interface MeritListRepository extends JpaRepository<MeritList, Long> {

    // Get merit records of an application
    List<MeritList> findByApplication(Application application);

    // Delete merit records of an application
    void deleteByApplication(Application application);

    // =====================================================
    // YEAR-WIDE MERIT LIST (COMMON BOYS + GIRLS)
    // =====================================================

    List<MeritList> findByYearOrderByMeritRankAsc(String year);

    List<MeritList> findByYearAndPublishedTrueOrderByMeritRankAsc(String year);

    void deleteByYear(String year);

    List<MeritList> findByYearAndBranchOrderByMeritRankAsc(String year, String branch);

    List<MeritList> findByYearAndBranchAndPublishedTrueOrderByMeritRankAsc(String year, String branch);

    // =====================================================
    // GENDER + BRANCH + YEAR SPECIFIC QUERIES
    // =====================================================

    List<MeritList> findByGenderAndBranchAndYearOrderByMeritRankAsc(
            String gender,
            String branch,
            String year
    );

    void deleteByGenderAndBranchAndYear(
            String gender,
            String branch,
            String year
    );

    List<MeritList> findByGenderAndBranchAndYearAndPublishedTrueOrderByMeritRankAsc(
            String gender,
            String branch,
            String year
    );

    // =====================================================
    // GENDER QUERIES
    // =====================================================

    List<MeritList> findByGenderOrderByMeritRankAsc(String gender);

    List<MeritList> findByGenderAndPublishedTrueOrderByMeritRankAsc(String gender);
}
