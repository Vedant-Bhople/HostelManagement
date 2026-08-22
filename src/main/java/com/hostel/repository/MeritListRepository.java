
package com.hostel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.model.MeritList;

public interface MeritListRepository
        extends JpaRepository<MeritList, Long> {

    // =====================================================
    // GET MERIT LIST
    // Gender + Branch + Year
    // =====================================================

    List<MeritList>
    findByGenderAndBranchAndYearOrderByMeritRankAsc(
            String gender,
            String branch,
            String year
    );


    // =====================================================
    // DELETE MERIT LIST
    // Gender + Branch + Year
    // =====================================================

    void deleteByGenderAndBranchAndYear(
            String gender,
            String branch,
            String year
    );


    // =====================================================
    // GET PUBLISHED MERIT LIST
    // Gender + Branch + Year
    // =====================================================

    List<MeritList>
    findByGenderAndBranchAndYearAndPublishedTrueOrderByMeritRankAsc(
            String gender,
            String branch,
            String year
    );


    // =====================================================
    // GET MERIT LIST BY GENDER
    // =====================================================

    List<MeritList>
    findByGenderOrderByMeritRankAsc(
            String gender
    );


    // =====================================================
    // GET PUBLISHED MERIT LIST BY GENDER
    // =====================================================

    List<MeritList>
    findByGenderAndPublishedTrueOrderByMeritRankAsc(
            String gender
    );

}

