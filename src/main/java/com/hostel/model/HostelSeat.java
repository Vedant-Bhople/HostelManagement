package com.hostel.model;

import javax.persistence.*;

@Entity
@Table(name = "hostel_seats")
public class HostelSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // BOYS / GIRLS
    private String hostelType;


    // COMPUTER / MECHANICAL / CIVIL / ELECTRICAL / IT
    private String branch;


    // 1 / 2 / 3
    private String year;


    // Example:
    // B-COMPUTER-Y1-01
    private String seatNumber;


    // AVAILABLE / ALLOTTED / OCCUPIED
    private String status;


    // SC / ST / OBC / OPEN
    private String reservedCategory;


    @OneToOne
    @JoinColumn(name = "student_id")
    private User student;


    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getHostelType() {
        return hostelType;
    }

    public void setHostelType(String hostelType) {
        this.hostelType = hostelType;
    }


    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }


    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }


    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getReservedCategory() {
        return reservedCategory;
    }

    public void setReservedCategory(
            String reservedCategory) {

        this.reservedCategory = reservedCategory;
    }


    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }
}