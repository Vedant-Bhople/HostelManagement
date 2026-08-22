package com.hostel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.model.Allotment;

public interface AllotmentRepository extends JpaRepository<Allotment, Long> {

    // =====================================================
    // GET ALLOTMENT LIST
    // Gender + Branch + Year
    // =====================================================

    List<Allotment> findByGenderAndBranchAndYearOrderByMeritRankAsc(
            String gender,
            String branch,
            String year
    );

    // =====================================================
    // GET ALLOTMENT BY APPLICATION
    // =====================================================

    List<Allotment> findByApplicationId(Long applicationId);

    // =====================================================
    // GET BY ALLOTMENT STATUS
    // =====================================================

    List<Allotment> findByAllotmentStatus(String allotmentStatus);

    // =====================================================
    // GET WAITING LIST
    // =====================================================

    List<Allotment>
    findByGenderAndBranchAndYearAndAllotmentStatusOrderByMeritRankAsc(
            String gender,
            String branch,
            String year,
            String allotmentStatus
    );

    // =====================================================
    // GET SPECIFIC CATEGORY ALLOTMENTS
    // =====================================================

    List<Allotment>
    findByGenderAndBranchAndYearAndAllotmentCategoryOrderByMeritRankAsc(
            String gender,
            String branch,
            String year,
            String allotmentCategory
    );

    // =====================================================
    // CHECK STUDENT ALLOTMENT
    // =====================================================

    boolean existsByApplicationId(Long applicationId);

    // =====================================================
    // CHECK SEAT NUMBER
    // =====================================================

    boolean existsBySeatNumber(String seatNumber);

    // =====================================================
    // GET ACCEPTED ALLOTMENTS
    // =====================================================

    List<Allotment> findByGenderAndBranchAndYearAndAllotmentStatus(
            String gender,
            String branch,
            String year,
            String allotmentStatus
    );

    // =====================================================
    // STUDENT - GET ALLOTMENTS BY USER ID
    // Application -> User -> ID
    // =====================================================

    List<Allotment> findByApplication_User_Id(Long userId);

    // =====================================================
    // GET ALLOTMENTS BY MERIT LIST
    // IMPORTANT FOR DELETING OLD MERIT LIST
    // =====================================================

    List<Allotment> findByMeritListId(Long meritListId);
}