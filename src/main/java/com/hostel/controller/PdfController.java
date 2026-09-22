package com.hostel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hostel.service.PdfGeneratorService;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin
public class PdfController {

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    // =====================================================
    // GENERATE INDIVIDUAL ALLOTMENT PDF (GENDER + YEAR + ROUND)
    // =====================================================

    @GetMapping("/allotment")
    public ResponseEntity<byte[]> generateAllotmentPdf(
            @RequestParam(defaultValue = "BOYS") String gender,
            @RequestParam(defaultValue = "1") String year,
            @RequestParam(defaultValue = "REGULAR") String round,
            @RequestParam(required = false) String refNo) {

        try {
            byte[] pdfBytes = pdfGeneratorService.generateAllotmentPdf(gender, year, round, refNo);
            String fileName = pdfGeneratorService.generatePdfFileName(gender, year, round);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", fileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // =====================================================
    // GENERATE COMPLETE MASTER HOSTEL PDF (ALL YEARS & WINGS)
    // =====================================================

    @GetMapping("/allotment/complete")
    public ResponseEntity<byte[]> generateCompleteHostelPdf(
            @RequestParam(required = false) String refNo) {

        try {
            byte[] pdfBytes = pdfGeneratorService.generateCompleteHostelPdf(refNo);
            String fileName = "Complete_Hostel_Allotment_List_2026-27.pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", fileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
