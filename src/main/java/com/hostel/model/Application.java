package com.hostel.model;

import javax.persistence.*;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // STUDENT
    // =====================================================

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // =====================================================
    // PERSONAL INFORMATION
    // =====================================================

    private String fullName;
    private String dateOfBirth;
    private String gender;
    private String address;
    private String mobileNumber;

    // =====================================================
    // ACADEMIC INFORMATION
    // =====================================================

    private String enrollmentNumber;
    private String collegeName;
    private String branch;
    private String year;
    private String admissionYear;

    // =====================================================
    // CATEGORY
    // =====================================================

    // SC / ST / OBC / VJ-A / NT-B / NT-C / NT-D / OTHER
    private String category;

    private String otherCategory;

    // =====================================================
    // SEMESTER 1
    // =====================================================

    private Double sem1Obtained;
    private Double sem1Total;
    private Double sem1Percentage;

    // =====================================================
    // SEMESTER 2
    // =====================================================

    private Double sem2Obtained;
    private Double sem2Total;
    private Double sem2Percentage;

    // =====================================================
    // FINAL AGGREGATE
    // =====================================================

    private Double aggregate;

    // =====================================================
    // ATKT
    // =====================================================

    // YES / NO
    private String atktStatus;

    private Integer atktSubjects;

    private String atktSubjectDetails;

    // =====================================================
    // APPLICATION STATUS
    // =====================================================

    // PENDING / APPROVED / REJECTED
    private String status;

    private String rejectionReason;

    // =====================================================
    // MERIT
    // =====================================================

    private Integer meritRank;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Application() {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getEnrollmentNumber() {
        return enrollmentNumber;
    }

    public void setEnrollmentNumber(String enrollmentNumber) {
        this.enrollmentNumber = enrollmentNumber;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
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

    public String getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(String admissionYear) {
        this.admissionYear = admissionYear;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOtherCategory() {
        return otherCategory;
    }

    public void setOtherCategory(String otherCategory) {
        this.otherCategory = otherCategory;
    }

    public Double getSem1Obtained() {
        return sem1Obtained;
    }

    public void setSem1Obtained(Double sem1Obtained) {
        this.sem1Obtained = sem1Obtained;
    }

    public Double getSem1Total() {
        return sem1Total;
    }

    public void setSem1Total(Double sem1Total) {
        this.sem1Total = sem1Total;
    }

    public Double getSem1Percentage() {
        return sem1Percentage;
    }

    public void setSem1Percentage(Double sem1Percentage) {
        this.sem1Percentage = sem1Percentage;
    }

    public Double getSem2Obtained() {
        return sem2Obtained;
    }

    public void setSem2Obtained(Double sem2Obtained) {
        this.sem2Obtained = sem2Obtained;
    }

    public Double getSem2Total() {
        return sem2Total;
    }

    public void setSem2Total(Double sem2Total) {
        this.sem2Total = sem2Total;
    }

    public Double getSem2Percentage() {
        return sem2Percentage;
    }

    public void setSem2Percentage(Double sem2Percentage) {
        this.sem2Percentage = sem2Percentage;
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

    public String getAtktSubjectDetails() {
        return atktSubjectDetails;
    }

    public void setAtktSubjectDetails(String atktSubjectDetails) {
        this.atktSubjectDetails = atktSubjectDetails;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Integer getMeritRank() {
        return meritRank;
    }

    public void setMeritRank(Integer meritRank) {
        this.meritRank = meritRank;
    }
}