package com.hostel.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.model.Allotment;
import com.hostel.service.WaitingListService;

@RestController
@RequestMapping("/api/waiting-list")
@CrossOrigin
public class WaitingListController {

    @Autowired
    private WaitingListService waitingListService;


    // =====================================================
    // GET WAITING LIST
    // =====================================================

    @GetMapping
    public ResponseEntity<?> getWaitingList(

            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            List<Allotment> waitingList =
                    waitingListService.getWaitingList(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(
                    waitingList
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ALLOT NEXT WAITING STUDENT
    // =====================================================

    @PutMapping("/allot-next")
    public ResponseEntity<?> allotNextWaitingStudent(

            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            Allotment allotment =
                    waitingListService
                    .allotNextWaitingStudent(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(
                    allotment
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}