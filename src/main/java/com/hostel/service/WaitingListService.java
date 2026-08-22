package com.hostel.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hostel.model.Allotment;
import com.hostel.repository.AllotmentRepository;

@Service
public class WaitingListService {

    @Autowired
    private AllotmentRepository allotmentRepository;


    // =====================================================
    // GET WAITING LIST
    // =====================================================

    public List<Allotment> getWaitingList(
            String gender,
            String branch,
            String year) {

        return allotmentRepository
                .findByGenderAndBranchAndYearAndAllotmentStatusOrderByMeritRankAsc(
                        gender,
                        branch,
                        year,
                        "WAITING"
                );
    }


    // =====================================================
    // ALLOT NEXT WAITING STUDENT
    // =====================================================

    public Allotment allotNextWaitingStudent(
            String gender,
            String branch,
            String year) {

        List<Allotment> waitingList =
                getWaitingList(
                        gender,
                        branch,
                        year
                );

        if (waitingList == null
                || waitingList.isEmpty()) {

            throw new RuntimeException(
                    "No waiting students found"
            );
        }


        // Highest merit student
        Allotment nextStudent =
                waitingList.get(0);


        // Change status
        nextStudent.setAllotmentStatus(
                "ALLOTTED"
        );


        return allotmentRepository.save(
                nextStudent
        );
    }
}