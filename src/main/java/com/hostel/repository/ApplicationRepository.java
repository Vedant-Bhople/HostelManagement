package com.hostel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.model.Application;
import com.hostel.model.User;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    // =====================================================
    // GET APPLICATIONS BY USER
    // =====================================================

    List<Application> findByUser(User user);


    // =====================================================
    // GET APPLICATIONS BY STATUS
    // =====================================================

    List<Application> findByStatus(String status);


    // =====================================================
    // GET APPROVED APPLICATIONS
    // Gender + Branch + Year + Status
    // =====================================================

    List<Application> findByGenderAndBranchAndYearAndStatus(
            String gender,
            String branch,
            String year,
            String status
    );


    // =====================================================
    // GET APPLICATIONS BY GENDER
    // =====================================================

    List<Application> findByGender(String gender);


    // =====================================================
    // GET APPLICATIONS BY BRANCH
    // =====================================================

    List<Application> findByBranch(String branch);


    // =====================================================
    // GET APPLICATIONS BY YEAR
    // =====================================================

    List<Application> findByYear(String year);


    // =====================================================
    // GET APPLICATIONS BY GENDER + BRANCH + YEAR
    // =====================================================

    List<Application> findByGenderAndBranchAndYear(
            String gender,
            String branch,
            String year
    );
}