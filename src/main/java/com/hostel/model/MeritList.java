package com.hostel.model;

import javax.persistence.*;

@Entity
@Table(name = "merit_lists")
public class MeritList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private Application application;
    
    private boolean published;
    
    private Integer meritRank;

    private String enrollmentNo;

    private String studentName;

    private String gender;

    private String branch;

    private String year;

    // Student ki actual category
    private String category;

    // Merit calculation ke liye category
    private String meritCategory;

    private Double aggregate;

    private String atktStatus;

    private Integer atktSubjects;

    // SELECTED / WAITING
    private String meritStatus;


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

    public Integer getMeritRank() {
        return meritRank;
    }

    public void setMeritRank(Integer meritRank) {
        this.meritRank = meritRank;
    }

    public String getEnrollmentNo() {
        return enrollmentNo;
    }

    public void setEnrollmentNo(String enrollmentNo) {
        this.enrollmentNo = enrollmentNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public String getMeritCategory() {
        return meritCategory;
    }

    public void setMeritCategory(String meritCategory) {
        this.meritCategory = meritCategory;
    }

    public Double getAggregate() {
        return aggregate;
    }

    public void setAggregate(Double aggregate) {
        this.aggregate = aggregate;
    }

    public String getAtktStatus() {
        return atktStatus;
    }

    public void setAtktStatus(String atktStatus) {
        this.atktStatus = atktStatus;
    }

    public Integer getAtktSubjects() {
        return atktSubjects;
    }

    public void setAtktSubjects(Integer atktSubjects) {
        this.atktSubjects = atktSubjects;
    }

    public String getMeritStatus() {
        return meritStatus;
    }

    public void setMeritStatus(String meritStatus) {
        this.meritStatus = meritStatus;
    }
    
    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }
}