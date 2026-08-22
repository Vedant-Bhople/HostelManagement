package com.hostel.model;

import javax.persistence.*;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Application
    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    // Document type
    // ADMISSION_RECEIPT
    // PASSPORT_PHOTO
    // SEMESTER_MARKSHEET
    // CAP_LETTER
    // OTHER
    private String documentType;

    // Original file name
    private String fileName;

    // Stored file path
    private String filePath;

    // PENDING / VERIFIED / REJECTED
    private String verificationStatus;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Document() {
    }

    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}