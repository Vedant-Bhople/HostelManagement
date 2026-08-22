package com.hostel.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hostel.model.Application;
import com.hostel.model.Document;
import com.hostel.repository.ApplicationRepository;
import com.hostel.repository.DocumentRepository;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private final String UPLOAD_DIR = "uploads/";

    // Upload Document
    public Document uploadDocument(
            Long applicationId,
            String documentType,
            MultipartFile file) throws IOException {

        Application application =
                applicationRepository.findById(applicationId)
                .orElseThrow(() ->
                        new RuntimeException("Application not found"));

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Please select a file");
        }

        // Allowed file types
        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new RuntimeException("Invalid file");
        }

        String lowerName = fileName.toLowerCase();

        if (!(lowerName.endsWith(".pdf")
                || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png"))) {

            throw new RuntimeException(
                    "Only PDF, JPG, JPEG and PNG files are allowed"
            );
        }

        // Create uploads folder
        File directory = new File(UPLOAD_DIR);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Unique file name
        String uniqueFileName =
                System.currentTimeMillis() + "_" + fileName;

        Path path = Paths.get(
                UPLOAD_DIR + uniqueFileName
        );

        Files.copy(file.getInputStream(), path);

        // Save document information
        Document document = new Document();

        document.setApplication(application);
        document.setDocumentType(documentType);
        document.setFileName(fileName);
        document.setFilePath(path.toString());
        document.setVerificationStatus("PENDING");

        return documentRepository.save(document);
    }

    // Get documents of application
    public List<Document> getDocuments(Long applicationId) {

        Application application =
                applicationRepository.findById(applicationId)
                .orElseThrow(() ->
                        new RuntimeException("Application not found"));

        return documentRepository.findByApplication(application);
    }

    // Verify document
    public Document verifyDocument(Long id) {

        Document document =
                documentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Document not found"));

        document.setVerificationStatus("VERIFIED");

        return documentRepository.save(document);
    }

    // Reject document
    public Document rejectDocument(
            Long id,
            String reason) {

        Document document =
                documentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Document not found"));

        document.setVerificationStatus(
                "REJECTED: " + reason
        );

        return documentRepository.save(document);
    }
 // =====================================================
 // VIEW / DOWNLOAD DOCUMENT
 // =====================================================

 public org.springframework.core.io.Resource getDocumentFile(Long id) {

     Document document =
             documentRepository.findById(id)
             .orElseThrow(() ->
                     new RuntimeException("Document not found"));

     Path path = Paths.get(document.getFilePath());

     if (!Files.exists(path)) {
         throw new RuntimeException("File not found on server");
     }

     return new org.springframework.core.io.FileSystemResource(path);
 }
}