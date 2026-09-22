package com.hostel.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hostel.service.VacancyService;

@RestController
@RequestMapping("/api/vacancy")
@CrossOrigin
public class VacancyController {

    @Autowired
    private VacancyService vacancyService;


    // =====================================================
    // GET VACANCY
    // =====================================================

    @GetMapping
    public ResponseEntity<?> getVacancy(

            @RequestParam String gender,
            @RequestParam String branch,
            @RequestParam String year) {

        try {

            Map<String, Object> result =
                    vacancyService.getVacancy(
                            gender,
                            branch,
                            year
                    );

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}