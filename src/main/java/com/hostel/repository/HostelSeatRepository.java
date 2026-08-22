package com.hostel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.model.HostelSeat;

public interface HostelSeatRepository
        extends JpaRepository<HostelSeat, Long> {


    List<HostelSeat>
    findByHostelTypeAndBranchAndYear(
            String hostelType,
            String branch,
            String year
    );


    List<HostelSeat>
    findByHostelType(
            String hostelType
    );


    List<HostelSeat>
    findByStatus(
            String status
    );


    List<HostelSeat>
    findByHostelTypeAndBranchAndYearAndStatus(
            String hostelType,
            String branch,
            String year,
            String status
    );


    List<HostelSeat>
    findByHostelTypeAndBranchAndYearAndReservedCategoryAndStatus(
            String hostelType,
            String branch,
            String year,
            String reservedCategory,
            String status
    );
}