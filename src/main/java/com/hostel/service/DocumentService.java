package com.hostel.service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
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

    @Value("${SUPABASE_URL}")
    private String supabaseUrl;

    @Value("${SUPABASE_SERVICE_KEY}")
    private String supabaseServiceKey;

    private final String BUCKET_NAME = "documents";

    private final RestTemplate restTemplate = new RestTemplate();

    // =====================================================
    // UPLOAD DOCUMENT
    // =====================================================

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

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new RuntimeException("Invalid file");
        }

        String lowerName = originalFileName.toLowerCase();

        if (!(lowerName.endsWith(".pdf")
                || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png"))) {

            throw new RuntimeException(
                    "Only PDF, JPG, JPEG and PNG files are allowed");
        }

        // Create unique file name
        String cleanFileName =
                Paths.get(originalFileName)
                .getFileName()
                .toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        String storagePath =
                UUID.randomUUID() + "_" + cleanFileName;

        // Supabase Storage upload URL
        String uploadUrl =
                supabaseUrl
                + "/storage/v1/object/"
                + BUCKET_NAME
                + "/"
                + storagePath;

        HttpHeaders headers = new HttpHeaders();

        headers.set("apikey", supabaseServiceKey);
        headers.set(
                "Authorization",
                "Bearer " + supabaseServiceKey
        );

        MediaType contentType =
                file.getContentType() != null
                ? MediaType.parseMediaType(file.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        headers.setContentType(contentType);

        // Prevent overwriting another file
        headers.set("x-upsert", "false");

        HttpEntity<byte[]> request =
                new HttpEntity<>(file.getBytes(), headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        uploadUrl,
                        HttpMethod.POST,
                        request,
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Failed to upload file to Supabase Storage");
        }

        // Save document information in PostgreSQL
        Document document = new Document();

        document.setApplication(application);
        document.setDocumentType(documentType);
        document.setFileName(originalFileName);

        // Store Supabase Storage path instead of Render local path
        document.setFilePath(storagePath);

        document.setVerificationStatus("PENDING");

        return documentRepository.save(document);
    }

    // =====================================================
    // GET DOCUMENTS OF APPLICATION
    // =====================================================

    public List<Document> getDocuments(Long applicationId) {

        Application application =
                applicationRepository.findById(applicationId)
                .orElseThrow(() ->
                        new RuntimeException("Application not found"));

        return documentRepository.findByApplication(application);
    }

    // =====================================================
    // VERIFY DOCUMENT
    // =====================================================

    public Document verifyDocument(Long id) {

        Document document =
                documentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Document not found"));

        document.setVerificationStatus("VERIFIED");

        return documentRepository.save(document);
    }

    // =====================================================
    // REJECT DOCUMENT
    // =====================================================

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

    public Resource getDocumentFile(Long id) {

        Document document =
                documentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Document not found"));

        String storagePath = document.getFilePath();

        if (storagePath == null || storagePath.trim().isEmpty()) {
            throw new RuntimeException("Document file path is missing");
        }

        // Supabase Storage download URL
        String downloadUrl =
                supabaseUrl
                + "/storage/v1/object/"
                + BUCKET_NAME
                + "/"
                + storagePath;

        HttpHeaders headers = new HttpHeaders();

        headers.set("apikey", supabaseServiceKey);

        headers.set(
                "Authorization",
                "Bearer " + supabaseServiceKey
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        downloadUrl,
                        HttpMethod.GET,
                        request,
                        byte[].class
                );

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null) {

            throw new RuntimeException(
                    "File not found in Supabase Storage");
        }

        byte[] fileBytes = response.getBody();

        return new ByteArrayResource(fileBytes) {

            @Override
            public String getFilename() {
                return document.getFileName();
            }
        };
    }
}
