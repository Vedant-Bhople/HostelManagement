package com.hostel.dto;

public class AdminDashboardDTO {

    private long totalApplications;
    private long pendingApplications;
    private long approvedApplications;
    private long rejectedApplications;

    private long totalAllotments;
    private long acceptedAllotments;
    private long rejectedAllotments;


    public AdminDashboardDTO() {
    }


    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }


    public long getPendingApplications() {
        return pendingApplications;
    }

    public void setPendingApplications(long pendingApplications) {
        this.pendingApplications = pendingApplications;
    }


    public long getApprovedApplications() {
        return approvedApplications;
    }

    public void setApprovedApplications(long approvedApplications) {
        this.approvedApplications = approvedApplications;
    }


    public long getRejectedApplications() {
        return rejectedApplications;
    }

    public void setRejectedApplications(long rejectedApplications) {
        this.rejectedApplications = rejectedApplications;
    }


    public long getTotalAllotments() {
        return totalAllotments;
    }

    public void setTotalAllotments(long totalAllotments) {
        this.totalAllotments = totalAllotments;
    }


    public long getAcceptedAllotments() {
        return acceptedAllotments;
    }

    public void setAcceptedAllotments(long acceptedAllotments) {
        this.acceptedAllotments = acceptedAllotments;
    }


    public long getRejectedAllotments() {
        return rejectedAllotments;
    }

    public void setRejectedAllotments(long rejectedAllotments) {
        this.rejectedAllotments = rejectedAllotments;
    }
}