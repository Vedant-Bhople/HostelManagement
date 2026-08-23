package com.hostel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.SendEmailRequest;

import com.hostel.model.Allotment;
import com.hostel.model.Application;
import com.hostel.model.MeritList;

@Service
public class EmailService {

    private static final Logger logger =
            LoggerFactory.getLogger(EmailService.class);

    // =====================================================
    // RESEND CONFIGURATION
    // =====================================================

    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    @Value("${EMAIL_FROM:onboarding@resend.dev}")
    private String fromEmail;


    // =====================================================
    // GENERIC SEND EMAIL METHOD
    // =====================================================

    public boolean sendEmail(
            String to,
            String subject,
            String messageText) {

        if (to == null || to.trim().isEmpty()) {

            logger.warn("Cannot send email: Recipient address is empty.");

            return false;
        }

        try {

            Resend resend =
                    new Resend(resendApiKey);

            String htmlMessage =
                    "<div style=\"font-family:Arial,sans-serif;"
                    + "white-space:pre-wrap;\">"
                    + escapeHtml(messageText)
                    + "</div>";

            SendEmailRequest request =
                    SendEmailRequest.builder()
                            .from(fromEmail)
                            .to(to.trim())
                            .subject(subject)
                            .html(htmlMessage)
                            .build();

            // Send email
            resend.emails().send(request);

            logger.info(
                    "Email successfully sent to {} | Subject: {}",
                    to,
                    subject
            );

            return true;

        } catch (Exception e) {

            logger.error(
                    "Failed to send email to '{}' | Subject: '{}'. Reason: {}",
                    to,
                    subject,
                    e.getMessage(),
                    e
            );

            return false;
        }
    }


    // =====================================================
    // HTML ESCAPE
    // =====================================================

    private String escapeHtml(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }


    // =====================================================
    // APPLICATION APPROVED EMAIL
    // =====================================================

    public void sendApplicationApprovedEmail(
            Application application) {

        if (application == null
                || application.getUser() == null) {
            return;
        }

        String recipientEmail =
                application.getUser().getEmail();

        if (recipientEmail == null
                || recipientEmail.trim().isEmpty()) {
            return;
        }

        String studentName =
                application.getFullName() != null
                ? application.getFullName()
                : application.getUser().getName();

        String enrollmentNo =
                application.getEnrollmentNumber() != null
                ? application.getEnrollmentNumber()
                : "N/A";

        String branch =
                application.getBranch() != null
                ? application.getBranch()
                : "N/A";

        String year =
                application.getYear() != null
                ? application.getYear()
                : "N/A";

        String category =
                application.getCategory() != null
                ? application.getCategory()
                : "N/A";

        String subject =
                "Hostel Admission Application Approved - "
                + enrollmentNo;

        String body =
                "Dear " + studentName + ",\n\n"

                + "Congratulations! Your Hostel Admission "
                + "Application has been APPROVED.\n\n"

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

                + "Please login to your Student Portal to check "
                + "your application status.\n\n"

                + "Portal Link: YOUR_RENDER_URL/student/login.html\n\n"

                + "Warm Regards,\n"
                + "Hostel Administration & Warden Office\n"
                + "Campus Hostel Management System";

        sendEmail(
                recipientEmail,
                subject,
                body
        );
    }


    // =====================================================
    // APPLICATION REJECTED EMAIL
    // =====================================================

    public void sendApplicationRejectedEmail(
            Application application) {

        if (application == null
                || application.getUser() == null) {
            return;
        }

        String recipientEmail =
                application.getUser().getEmail();

        if (recipientEmail == null
                || recipientEmail.trim().isEmpty()) {
            return;
        }

        String studentName =
                application.getFullName() != null
                ? application.getFullName()
                : application.getUser().getName();

        String enrollmentNo =
                application.getEnrollmentNumber() != null
                ? application.getEnrollmentNumber()
                : "N/A";

        String branch =
                application.getBranch() != null
                ? application.getBranch()
                : "N/A";

        String year =
                application.getYear() != null
                ? application.getYear()
                : "N/A";

        String reason =
                application.getRejectionReason() != null
                ? application.getRejectionReason()
                : "Incomplete or inaccurate documents.";

        String subject =
                "Update: Hostel Admission Application Status - "
                + enrollmentNo;

        String body =
                "Dear " + studentName + ",\n\n"

                + "We regret to inform you that your Hostel "
                + "Admission Application has been REJECTED.\n\n"

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

                + "Please contact the Hostel Administration Office "
                + "for further clarification.\n\n"

                + "Portal Link: YOUR_RENDER_URL/student/login.html\n\n"

                + "Warm Regards,\n"
                + "Hostel Administration & Warden Office\n"
                + "Campus Hostel Management System";

        sendEmail(
                recipientEmail,
                subject,
                body
        );
    }


    // =====================================================
    // MERIT LIST PUBLISHED EMAIL
    // =====================================================

    public void sendMeritListPublishedEmail(
            MeritList meritList) {

        if (meritList == null
                || meritList.getApplication() == null
                || meritList.getApplication().getUser() == null) {
            return;
        }

        String recipientEmail =
                meritList.getApplication()
                        .getUser()
                        .getEmail();

        if (recipientEmail == null
                || recipientEmail.trim().isEmpty()) {
            return;
        }

        String studentName =
                meritList.getStudentName() != null
                ? meritList.getStudentName()
                : "Student";

        String enrollmentNo =
                meritList.getEnrollmentNo() != null
                ? meritList.getEnrollmentNo()
                : "N/A";

        String branch =
                meritList.getBranch() != null
                ? meritList.getBranch()
                : "N/A";

        String year =
                meritList.getYear() != null
                ? meritList.getYear()
                : "N/A";

        Integer meritRank =
                meritList.getMeritRank();

        Double aggregate =
                meritList.getAggregate();

        String meritCategory =
                meritList.getMeritCategory() != null
                ? meritList.getMeritCategory()
                : "OPEN";

        String subject =
                "Hostel Merit List Published - Rank #"
                + (meritRank != null
                ? meritRank
                : "N/A");

        String body =
                "Dear " + studentName + ",\n\n"

                + "The official Hostel Admission Merit List "
                + "has been PUBLISHED.\n\n"

                + "--------------------------------------------------\n"
                + "YOUR MERIT DETAILS:\n"
                + "--------------------------------------------------\n"

                + "Student Name   : " + studentName + "\n"
                + "Enrollment No  : " + enrollmentNo + "\n"
                + "Branch / Year  : " + branch
                + " / Year " + year + "\n"
                + "Merit Rank     : #"
                + (meritRank != null
                ? meritRank
                : "N/A") + "\n"

                + "Aggregate %    : "
                + (aggregate != null
                ? String.format("%.2f", aggregate) + "%"
                : "N/A") + "\n"

                + "Merit Category : "
                + meritCategory + "\n"

                + "--------------------------------------------------\n\n"

                + "Please login to your Student Portal to view "
                + "the full merit list.\n\n"

                + "Portal Link: YOUR_RENDER_URL/student/merit-list.html\n\n"

                + "Warm Regards,\n"
                + "Hostel Administration Office\n"
                + "Campus Hostel Management System";

        sendEmail(
                recipientEmail,
                subject,
                body
        );
    }


    // =====================================================
    // HOSTEL SEAT ALLOTMENT EMAIL
    // =====================================================

    public void sendAllotmentEmail(
            Allotment allotment) {

        if (allotment == null
                || allotment.getApplication() == null
                || allotment.getApplication().getUser() == null) {
            return;
        }

        String recipientEmail =
                allotment.getApplication()
                        .getUser()
                        .getEmail();

        if (recipientEmail == null
                || recipientEmail.trim().isEmpty()) {
            return;
        }

        String studentName =
                allotment.getApplication().getFullName() != null
                ? allotment.getApplication().getFullName()
                : allotment.getApplication()
                        .getUser()
                        .getName();

        String enrollmentNo =
                allotment.getApplication()
                        .getEnrollmentNumber() != null
                ? allotment.getApplication()
                        .getEnrollmentNumber()
                : "N/A";

        String hostelType =
                allotment.getHostelType() != null
                ? allotment.getHostelType()
                : "CAMPUS HOSTEL";

        String branch =
                allotment.getBranch() != null
                ? allotment.getBranch()
                : "N/A";

        String year =
                allotment.getYear() != null
                ? allotment.getYear()
                : "N/A";

        Integer meritRank =
                allotment.getMeritRank();

        String seatNumber =
                allotment.getSeatNumber() != null
                ? allotment.getSeatNumber()
                : "Awaiting Allocation";

        String allotmentCategory =
                allotment.getAllotmentCategory() != null
                ? allotment.getAllotmentCategory()
                : "OPEN";

        String allotmentStatus =
                allotment.getAllotmentStatus() != null
                ? allotment.getAllotmentStatus()
                : "ALLOTTED";

        String subject =
                "Congratulations! Hostel Seat Allotted - "
                + "Seat No: " + seatNumber;

        String body =
                "Dear " + studentName + ",\n\n"

                + "Congratulations! Your hostel seat has been "
                + "ALLOTTED successfully.\n\n"

                + "--------------------------------------------------\n"
                + "SEAT ALLOTMENT DETAILS:\n"
                + "--------------------------------------------------\n"

                + "Student Name       : " + studentName + "\n"
                + "Enrollment No      : " + enrollmentNo + "\n"
                + "Hostel Wing        : " + hostelType + "\n"
                + "Branch / Year      : " + branch
                + " / Year " + year + "\n"

                + "Merit Rank         : #"
                + (meritRank != null
                ? meritRank
                : "N/A") + "\n"

                + "Allotment Category : "
                + allotmentCategory + "\n"

                + "Allocated Seat No  : "
                + seatNumber + "\n"

                + "Allotment Status   : "
                + allotmentStatus + "\n"

                + "--------------------------------------------------\n\n"

                + "Please login to your Student Dashboard to "
                + "check your allotment details.\n\n"

                + "Portal Link: YOUR_RENDER_URL/student/allotment.html\n\n"

                + "Warm Regards,\n"
                + "Hostel Administration & Warden Office\n"
                + "Campus Hostel Management System";

        sendEmail(
                recipientEmail,
                subject,
                body
        );
    }
}
