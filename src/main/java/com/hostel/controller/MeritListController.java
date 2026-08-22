package com.hostel.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.model.MeritList;
import com.hostel.service.MeritListService;

@RestController
@RequestMapping("/api/merit")
@CrossOrigin(origins = "*")
public class MeritListController {

    @Autowired
    private MeritListService meritListService;


    // =====================================================
    // ADMIN - GENERATE MERIT LIST
    // =====================================================

    @PostMapping("/generate")
    public ResponseEntity<?> generateMeritList(
            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            List<MeritList> meritList =
                    meritListService.generateMeritList(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(meritList);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ADMIN - GET MERIT LIST
    // =====================================================

    @GetMapping
    public ResponseEntity<?> getMeritList(
            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            List<MeritList> meritList =
                    meritListService.getMeritList(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(meritList);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ADMIN - GET ALL MERIT LISTS
    // =====================================================

    @GetMapping("/all")
    public ResponseEntity<?> getAllMeritLists() {

        try {

            return ResponseEntity.ok(
                    meritListService
                            .getAllMeritLists()
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ADMIN - PUBLISH MERIT LIST
    // =====================================================

    @PutMapping("/publish")
    public ResponseEntity<?> publishMeritList(
            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            List<MeritList> list =
                    meritListService.publishMeritList(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(list);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // ADMIN - UNPUBLISH MERIT LIST
    // =====================================================

    @PutMapping("/unpublish")
    public ResponseEntity<?> unpublishMeritList(
            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            List<MeritList> list =
                    meritListService.unpublishMeritList(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(list);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // STUDENT - VIEW PUBLISHED MERIT LIST
    // =====================================================

    @GetMapping("/published")
    public ResponseEntity<?> getPublishedMeritList(
            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            List<MeritList> list =
                    meritListService
                            .getPublishedMeritList(
                                    gender,
                                    branch,
                                    year
                            );

            return ResponseEntity.ok(list);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // GET MERIT LIST BY GENDER
    // =====================================================

    @GetMapping("/gender")
    public ResponseEntity<?> getByGender(
            @RequestParam String gender) {

        try {

            return ResponseEntity.ok(
                    meritListService
                            .getByGender(gender)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // STUDENT - GET PUBLISHED BY GENDER
    // =====================================================

    @GetMapping("/gender/published")
    public ResponseEntity<?> getPublishedByGender(
            @RequestParam String gender) {

        try {

            return ResponseEntity.ok(
                    meritListService
                            .getPublishedByGender(
                                    gender
                            )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // UPDATE MERIT STATUS
    // =====================================================

    @PutMapping("/status/{id}")
    public ResponseEntity<?> updateMeritStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        try {

            MeritList merit =
                    meritListService
                            .updateMeritStatus(
                                    id,
                                    status
                            );

            return ResponseEntity.ok(merit);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =====================================================
    // DELETE MERIT LIST
    // =====================================================

    @DeleteMapping
    public ResponseEntity<?> deleteMeritList(
            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            meritListService.deleteMeritList(
                    gender,
                    branch,
                    year
            );

            return ResponseEntity.ok(
                    "Merit list deleted successfully"
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}