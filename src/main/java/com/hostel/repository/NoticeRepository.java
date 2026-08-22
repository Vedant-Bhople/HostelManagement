package com.hostel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.model.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByStatus(String status);

}