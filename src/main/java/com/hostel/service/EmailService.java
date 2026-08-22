package com.hostel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.hostel.model.Allotment;
import com.hostel.model.Application;
import com.hostel.model.MeritList;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@hostel.edu}")
    private String fromEmail;

    // =====================================================
    // GENERIC SEND EMAIL METHOD
    // =====================================================

    public boolean sendEmail(String to, String subject, String messageText) {
        if (to == null || to.trim().isEmpty()) {
            logger.warn("Cannot send email: Recipient address is empty.");
            return false;
        }

        if (mailSender == null) {
            logger.warn("JavaMailSender bean is not configured. Email to '{}' skipped.", to);
            return false;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(to.trim());
            mailMessage.setSubject(subject);
            mailMessage.setText(messageText);

            mailSender.send(mailMessage);
            logger.info("Email successfully sent to: {} | Subject: {}", to, subject);
            return true;

        } catch (Exception e) {
            logger.error("Failed to send email to '{}' | Subject: '{}'. Reason: {}", to, subject, e.getMessage());
            return false;
        }
    }

    // =====================================================
    // 1. APPLICATION APPROVED EMAIL
    // =====================================================

    public void sendApplicationApprovedEmail(Application application) {
        if (application == null || application.getUser() == null) {
            return;
        }

        String recipientEmail = application.getUser().getEmail();
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return;
        }

        String studentName = application.getFullName() != null ? application.getFullName() : application.getUser().getName();
        String enrollmentNo = application.getEnrollmentNumber() != null ? application.getEnrollmentNumber() : "N/A";
        String branch = application.getBranch() != null ? application.getBranch() : "N/A";
        String year = application.getYear() != null ? application.getYear() : "N/A";
        String category = application.getCategory() != null ? application.getCategory() : "N/A";

        String subject = "Hostel Admission Application Approved - " + enrollmentNo;

        String body = "Dear " + studentName + ",\n\n"
                + "Congratulations! Your Hostel Admission Application has been APPROVED.\n\n"
                + "--------------------------------------------------\n"
                + "APPLICATION DETAILS:\n"
                + "--------------------------------------------------\n"
                + "Student Name   : " + studentName + "\n"
                + "Enrollment No  : " + enrollmentNo + "\n"
                + "Branch         : " + branch + "\n"
                + "Year           : " + year + "\n"
                + "Category       : " + category + "\n"
                + "Status         : APPROVED\n"
                + "--------------------------------------------------\n\n"
                + "Next Steps:\n"
                + "Please login to your Student Portal to check your application status.\n"
                + "Stay tuned for the official Merit List publication and Seat Allotment.\n\n"
                + "Portal Link: http://localhost:8082/student/login.html\n\n"
                + "Warm Regards,\n"
                + "Hostel Administration & Warden Office\n"
                + "Campus Hostel Management System";

        sendEmail(recipientEmail, subject, body);
    }

    // =====================================================
    // 2. APPLICATION REJECTED EMAIL
    // =====================================================

    public void sendApplicationRejectedEmail(Application application) {
        if (application == null || application.getUser() == null) {
            return;
        }

        String recipientEmail = application.getUser().getEmail();
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return;
        }

        String studentName = application.getFullName() != null ? application.getFullName() : application.getUser().getName();
        String enrollmentNo = application.getEnrollmentNumber() != null ? application.getEnrollmentNumber() : "N/A";
        String branch = application.getBranch() != null ? application.getBranch() : "N/A";
        String year = application.getYear() != null ? application.getYear() : "N/A";
        String reason = application.getRejectionReason() != null ? application.getRejectionReason() : "Incomplete or inaccurate documents.";

        String subject = "Update: Hostel Admission Application Status - " + enrollmentNo;

        String body = "Dear " + studentName + ",\n\n"
                + "We regret to inform you that your Hostel Admission Application has been REJECTED.\n\n"
                + "--------------------------------------------------\n"
                + "APPLICATION DETAILS:\n"
                + "--------------------------------------------------\n"
                + "Student Name   : " + studentName + "\n"
                + "Enrollment No  : " + enrollmentNo + "\n"
                + "Branch         : " + branch + "\n"
                + "Year           : " + year + "\n"
                + "Status         : REJECTED\n"
                + "Rejection Note : " + reason + "\n"
                + "--------------------------------------------------\n\n"
                + "If you need further clarification or wish to rectify your details, please contact the Hostel Administration Office or log in to your Student Dashboard.\n\n"
                + "Portal Link: http://localhost:8082/student/login.html\n\n"
                + "Warm Regards,\n"
                + "Hostel Administration & Warden Office\n"
                + "Campus Hostel Management System";

        sendEmail(recipientEmail, subject, body);
    }

    // =====================================================
    // 3. MERIT LIST PUBLISHED EMAIL
    // =====================================================

    public void sendMeritListPublishedEmail(MeritList meritList) {
        if (meritList == null || meritList.getApplication() == null || meritList.getApplication().getUser() == null) {
            return;
        }

        String recipientEmail = meritList.getApplication().getUser().getEmail();
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return;
        }

        String studentName = meritList.getStudentName() != null ? meritList.getStudentName() : "Student";
        String enrollmentNo = meritList.getEnrollmentNo() != null ? meritList.getEnrollmentNo() : "N/A";
        String branch = meritList.getBranch() != null ? meritList.getBranch() : "N/A";
        String year = meritList.getYear() != null ? meritList.getYear() : "N/A";
        Integer meritRank = meritList.getMeritRank();
        Double aggregate = meritList.getAggregate();
        String meritCategory = meritList.getMeritCategory() != null ? meritList.getMeritCategory() : "OPEN";

        String subject = "Hostel Merit List Published - Rank #" + (meritRank != null ? meritRank : "N/A");

        String body = "Dear " + studentName + ",\n\n"
                + "The official Hostel Admission Merit List for " + branch + " - Year " + year + " has been PUBLISHED.\n\n"
                + "--------------------------------------------------\n"
                + "YOUR MERIT RANKING:\n"
                + "--------------------------------------------------\n"
                + "Student Name   : " + studentName + "\n"
                + "Enrollment No  : " + enrollmentNo + "\n"
                + "Branch / Year  : " + branch + " / Year " + year + "\n"
                + "Merit Rank     : #" + (meritRank != null ? meritRank : "N/A") + "\n"
                + "Aggregate %    : " + (aggregate != null ? String.format("%.2f", aggregate) + "%" : "N/A") + "\n"
                + "Merit Category : " + meritCategory + "\n"
                + "--------------------------------------------------\n\n"
                + "Please login to your Student Portal to view the full merit rankings and monitor upcoming seat allotment rounds.\n\n"
                + "Portal Link: http://localhost:8082/student/merit-list.html\n\n"
                + "Warm Regards,\n"
                + "Hostel Administration Office\n"
                + "Campus Hostel Management System";

        sendEmail(recipientEmail, subject, body);
    }

    // =====================================================
    // 4. HOSTEL SEAT ALLOTMENT EMAIL
    // =====================================================

    public void sendAllotmentEmail(Allotment allotment) {
        if (allotment == null || allotment.getApplication() == null || allotment.getApplication().getUser() == null) {
            return;
        }

        String recipientEmail = allotment.getApplication().getUser().getEmail();
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return;
        }

        String studentName = allotment.getApplication().getFullName() != null ? allotment.getApplication().getFullName() : allotment.getApplication().getUser().getName();
        String enrollmentNo = allotment.getApplication().getEnrollmentNumber() != null ? allotment.getApplication().getEnrollmentNumber() : "N/A";
        String hostelType = allotment.getHostelType() != null ? allotment.getHostelType() : "CAMPUS HOSTEL";
        String branch = allotment.getBranch() != null ? allotment.getBranch() : "N/A";
        String year = allotment.getYear() != null ? allotment.getYear() : "N/A";
        Integer meritRank = allotment.getMeritRank();
        String seatNumber = allotment.getSeatNumber() != null ? allotment.getSeatNumber() : "Awaiting Allocation";
        String allotmentCategory = allotment.getAllotmentCategory() != null ? allotment.getAllotmentCategory() : "OPEN";
        String allotmentStatus = allotment.getAllotmentStatus() != null ? allotment.getAllotmentStatus() : "ALLOTTED";

        String subject = "Congratulations! Hostel Seat Allotted - Seat No: " + seatNumber;

        String body = "Dear " + studentName + ",\n\n"
                + "Congratulations! Your hostel seat has been ALLOTTED successfully.\n\n"
                + "--------------------------------------------------\n"
                + "SEAT ALLOTMENT DETAILS:\n"
                + "--------------------------------------------------\n"
                + "Student Name       : " + studentName + "\n"
                + "Enrollment No      : " + enrollmentNo + "\n"
                + "Hostel Wing        : " + hostelType + "\n"
                + "Branch / Year      : " + branch + " / Year " + year + "\n"
                + "Merit Rank         : #" + (meritRank != null ? meritRank : "N/A") + "\n"
                + "Allotment Category : " + allotmentCategory + "\n"
                + "Allocated Seat No  : " + seatNumber + "\n"
                + "Allotment Status   : " + allotmentStatus + "\n"
                + "--------------------------------------------------\n\n"
                + "Action Required:\n"
                + "Please login to your Student Dashboard to ACCEPT your allotted seat and complete the admission verification.\n\n"
                + "Portal Link: http://localhost:8082/student/allotment.html\n\n"
                + "Warm Regards,\n"
                + "Hostel Administration & Warden Office\n"
                + "Campus Hostel Management System";

        sendEmail(recipientEmail, subject, body);
    }
}
