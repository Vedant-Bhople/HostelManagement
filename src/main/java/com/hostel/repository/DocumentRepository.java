package com.hostel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.model.Application;
import com.hostel.model.Document;

public interface DocumentRepository
        extends JpaRepository<Document, Long> {

    // Get all documents of an application
    List<Document> findByApplication(Application application);
}