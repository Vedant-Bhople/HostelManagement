package com.hostel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
}