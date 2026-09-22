package com.hostel.dto;

import java.util.List;

import com.hostel.model.Allotment;
import com.hostel.model.Application;
import com.hostel.model.Document;

public class StudentDashboardDTO {

    private Application application;

    private List<Document> documents;

    private List<Allotment> allotments;


    // =====================================================
    // DEFAULT CONSTRUCTOR
    // =====================================================

    public StudentDashboardDTO() {
    }


    // =====================================================
    // PARAMETERIZED CONSTRUCTOR
    // =====================================================

    public StudentDashboardDTO(
            Application application,
            List<Document> documents,
            List<Allotment> allotments) {

        this.application = application;
        this.documents = documents;
        this.allotments = allotments;
    }


    // =====================================================
    // APPLICATION
    // =====================================================

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }


    // =====================================================
    // DOCUMENTS
    // =====================================================

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }


    // =====================================================
    // ALLOTMENTS
    // =====================================================

    public List<Allotment> getAllotments() {
        return allotments;
    }

    public void setAllotments(List<Allotment> allotments) {
        this.allotments = allotments;
    }
}