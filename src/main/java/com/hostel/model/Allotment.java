package com.hostel.model;

import javax.persistence.*;

@Entity
@Table(name = "allotments")
public class Allotment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // STUDENT APPLICATION
    // =====================================================

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;


    // =====================================================
    // MERIT LIST RECORD
    // =====================================================

    @ManyToOne
    @JoinColumn(name = "merit_list_id", nullable = false)
    private MeritList meritList;


    // =====================================================
    // HOSTEL INFORMATION
    // =====================================================

    private String hostelType;

    private String gender;

    private String branch;

    private String year;


    // =====================================================
    // CATEGORY
    // =====================================================

    // Student's actual category
    private String category;

    // Category used for seat allotment
    // SC / ST / OBC / OPEN
    private String allotmentCategory;


    // =====================================================
    // MERIT INFORMATION
    // =====================================================

    private Integer meritRank;

    private Double aggregate;


    // =====================================================
    // SEAT
    // =====================================================

    private String seatNumber;


    // =====================================================
    // ALLOTMENT STATUS
    // =====================================================

    // ALLOTTED / ACCEPTED / REJECTED / WAITING
    private String allotmentStatus;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Allotment() {
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


    public MeritList getMeritList() {
        return meritList;
    }

    public void setMeritList(MeritList meritList) {
        this.meritList = meritList;
    }


    public String getHostelType() {
        return hostelType;
    }

    public void setHostelType(String hostelType) {
        this.hostelType = hostelType;
    }


    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
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


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


    public String getAllotmentCategory() {
        return allotmentCategory;
    }

    public void setAllotmentCategory(String allotmentCategory) {
        this.allotmentCategory = allotmentCategory;
    }


    public Integer getMeritRank() {
        return meritRank;
    }

    public void setMeritRank(Integer meritRank) {
        this.meritRank = meritRank;
    }


    public Double getAggregate() {
        return aggregate;
    }

    public void setAggregate(Double aggregate) {
        this.aggregate = aggregate;
    }


    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }


    public String getAllotmentStatus() {
        return allotmentStatus;
    }

    public void setAllotmentStatus(String allotmentStatus) {
        this.allotmentStatus = allotmentStatus;
    }
}