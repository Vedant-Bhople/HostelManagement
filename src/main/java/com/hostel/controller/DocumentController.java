package com.hostel.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.hostel.model.Document;
import com.hostel.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    // Upload document
    @PostMapping("/upload/{applicationId}")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long applicationId,
            @RequestParam String documentType,
            @RequestParam MultipartFile file) {

        try {

            Document document =
                    documentService.uploadDocument(
                            applicationId,
                            documentType,
                            file
                    );

            return ResponseEntity.ok(document);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // Get application documents
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<?> getDocuments(
            @PathVariable Long applicationId) {

        try {

            List<Document> documents =
                    documentService.getDocuments(
                            applicationId
                    );

            return ResponseEntity.ok(documents);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // Verify document
    @PutMapping("/verify/{id}")
    public ResponseEntity<?> verifyDocument(
            @PathVariable Long id) {

        try {

            return ResponseEntity.ok(
                    documentService.verifyDocument(id)
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // Reject document
    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectDocument(
            @PathVariable Long id,
            @RequestParam String reason) {

        try {

            return ResponseEntity.ok(
                    documentService.rejectDocument(
                            id,
                            reason
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
 // =====================================================
 // VIEW / DOWNLOAD DOCUMENT
 // =====================================================

 @GetMapping("/view/{id}")
 public ResponseEntity<?> viewDocument(
         @PathVariable Long id) {

     try {

         Resource resource =
                 documentService.getDocumentFile(id);

         String fileName =
                 resource.getFilename();

         MediaType mediaType =
                 MediaType.APPLICATION_OCTET_STREAM;

         if (fileName != null) {

             String lowerName =
                     fileName.toLowerCase();

             if (lowerName.endsWith(".pdf")) {

                 mediaType =
                         MediaType.APPLICATION_PDF;

             } else if (lowerName.endsWith(".jpg")
                     || lowerName.endsWith(".jpeg")) {

                 mediaType =
                         MediaType.IMAGE_JPEG;

             } else if (lowerName.endsWith(".png")) {

                 mediaType =
                         MediaType.IMAGE_PNG;
             }
         }

         return ResponseEntity.ok()
                 .contentType(mediaType)
                 .header(
                         HttpHeaders.CONTENT_DISPOSITION,
                         "inline; filename=\"" + fileName + "\""
                 )
                 .body(resource);

     } catch (Exception e) {

         return ResponseEntity
                 .badRequest()
                 .body(e.getMessage());
     }
 }
}