package com.hostel.service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hostel.model.Allotment;
import com.hostel.model.Application;
import com.hostel.repository.AllotmentRepository;
import com.hostel.repository.ApplicationRepository;
import com.hostel.service.reservation.ReservationPolicy;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PdfGeneratorService {

    @Autowired
    private AllotmentRepository allotmentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    // Color Palette
    private static final BaseColor COLOR_PRIMARY_NAVY = new BaseColor(30, 58, 138);     // #1E3A8A
    private static final BaseColor COLOR_PRIMARY_DARK = new BaseColor(15, 23, 42);      // #0F172A
    private static final BaseColor COLOR_HEADER_BG = new BaseColor(30, 58, 138);        // Navy Blue
    private static final BaseColor COLOR_SPOT_HEADER_BG = new BaseColor(109, 40, 217);  // Purple
    private static final BaseColor COLOR_ROW_ALT = new BaseColor(248, 250, 252);        // Slate 50
    private static final BaseColor COLOR_ROW_WHITE = BaseColor.WHITE;
    private static final BaseColor COLOR_BORDER = new BaseColor(203, 213, 225);         // Slate 300
    private static final BaseColor COLOR_NOTE_BG = new BaseColor(254, 243, 199);        // Amber 50
    private static final BaseColor COLOR_NOTE_BORDER = new BaseColor(245, 158, 11);     // Amber 500
    private static final BaseColor COLOR_BRANCH_BAR = new BaseColor(226, 232, 240);     // Slate 200

    // Standard Branch Names Map
    private static final Map<String, String> BRANCH_FULL_NAMES = new LinkedHashMap<>();
    static {
        BRANCH_FULL_NAMES.put("COMPUTER", "COMPUTER ENGINEERING");
        BRANCH_FULL_NAMES.put("MECHANICAL", "MECHANICAL ENGINEERING");
        BRANCH_FULL_NAMES.put("CIVIL", "CIVIL ENGINEERING");
        BRANCH_FULL_NAMES.put("ELECTRICAL", "ELECTRICAL ENGINEERING");
        BRANCH_FULL_NAMES.put("IT", "INFORMATION TECHNOLOGY");
    }

    // =====================================================
    // 1. GENERATE INDIVIDUAL ALLOTMENT PDF
    // =====================================================

    public byte[] generateAllotmentPdf(String gender, String year, String round, String refNo) {
        String normalizedGender = "GIRLS".equalsIgnoreCase(gender) ? "GIRLS" : "BOYS";
        String normalizedRound = "SPOT".equalsIgnoreCase(round) ? "SPOT" : "REGULAR";
        String yearNum = (year != null && !year.trim().isEmpty()) ? year.trim() : "1";
        String referenceNumber = (refNo != null && !refNo.trim().isEmpty()) ? refNo.trim() : "शांतनीमूर्ति/वसतिगृह/२०२६/1318";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 45); // 0.5 inch margins

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            HeaderFooterEvent event = new HeaderFooterEvent();
            writer.setPageEvent(event);

            document.open();

            // Render Header, Tables, Notes & Signatures
            renderSingleSection(document, writer, normalizedGender, yearNum, normalizedRound, referenceNumber, false);

            document.close();
            return out.toByteArray();

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate Hostel Allotment PDF: " + e.getMessage(), e);
        }
    }


    // =====================================================
    // 2. GENERATE COMPLETE MASTER HOSTEL PDF
    // =====================================================

    public byte[] generateCompleteHostelPdf(String refNo) {
        String referenceNumber = (refNo != null && !refNo.trim().isEmpty()) ? refNo.trim() : "शांतनीमूर्ति/वसतिगृह/२०२६/1318";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 45);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            HeaderFooterEvent event = new HeaderFooterEvent();
            writer.setPageEvent(event);

            document.open();

            // 1. Girls Hostel: 1st, 2nd, 3rd Year (Regular)
            String[] years = {"1", "2", "3"};
            boolean isFirst = true;

            for (String yr : years) {
                if (!isFirst) {
                    document.newPage();
                }
                renderSingleSection(document, writer, "GIRLS", yr, "REGULAR", referenceNumber, true);
                isFirst = false;
            }

            // 2. Boys Hostel: 1st, 2nd, 3rd Year (Regular)
            for (String yr : years) {
                document.newPage();
                renderSingleSection(document, writer, "BOYS", yr, "REGULAR", referenceNumber, true);
            }

            document.close();
            return out.toByteArray();

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate Complete Hostel PDF: " + e.getMessage(), e);
        }
    }


    // =====================================================
    // 3. RENDER SINGLE GENDER + YEAR SECTION
    // =====================================================

    private void renderSingleSection(
            Document document,
            PdfWriter writer,
            String gender,
            String year,
            String round,
            String refNo,
            boolean isCompleteDoc) throws DocumentException {

        Font fontCollege = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, COLOR_PRIMARY_NAVY);
        Font fontSubtitle = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_PRIMARY_DARK);
        Font fontDocType = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, "SPOT".equalsIgnoreCase(round) ? COLOR_SPOT_HEADER_BG : COLOR_PRIMARY_NAVY);
        Font fontMeta = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.DARK_GRAY);
        Font fontRef = getMarathiFont(8f, Font.NORMAL, BaseColor.DARK_GRAY);

        // Header Table (Institution & Reference)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        try {
            headerTable.setWidths(new float[]{65, 35});
        } catch (DocumentException e) {
            // fallback
        }

        PdfPCell cellLeft = new PdfPCell();
        cellLeft.setBorder(Rectangle.NO_BORDER);
        cellLeft.addElement(new Paragraph("GOVERNMENT POLYTECHNIC MURTIZAPUR", fontCollege));
        cellLeft.addElement(new Paragraph("HOSTEL / VASATIGRUH", fontSubtitle));

        PdfPCell cellRight = new PdfPCell();
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pRef = new Paragraph("Ref: " + refNo, fontRef);
        pRef.setAlignment(Element.ALIGN_RIGHT);
        Paragraph pDate = new Paragraph("Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()), fontRef);
        pDate.setAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(pRef);
        cellRight.addElement(pDate);

        headerTable.addCell(cellLeft);
        headerTable.addCell(cellRight);
        document.add(headerTable);

        // Fetch Data from Database
        List<Allotment> allAllotments = allotmentRepository.findAll();

        List<Allotment> filteredAllotments = allAllotments.stream()
                .filter(a -> gender.equalsIgnoreCase(a.getGender()))
                .filter(a -> year.equals(a.getYear()))
                .filter(a -> {
                    if ("SPOT".equalsIgnoreCase(round)) {
                        return "SPOT".equalsIgnoreCase(a.getAllotmentRound())
                                && ("ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()));
                    } else {
                        return !"SPOT".equalsIgnoreCase(a.getAllotmentRound())
                                && ("ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()) || "ACCEPTED".equalsIgnoreCase(a.getAllotmentStatus()));
                    }
                })
                .collect(Collectors.toList());

        boolean hasConvertedSeats = filteredAllotments.stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsConverted()));

        // Document Title Banner
        String roundTitle;
        if ("SPOT".equalsIgnoreCase(round)) {
            roundTitle = "SPOT ROUND HOSTEL ALLOTMENT LIST";
        } else if (hasConvertedSeats) {
            roundTitle = "BRANCHWISE FINAL ALLOTMENT LIST";
        } else {
            roundTitle = "BRANCHWISE PROVISIONAL ALLOTMENT LIST";
        }

        String yearLabel = getYearLabel(year);
        String wingLabel = "BOYS".equalsIgnoreCase(gender) ? "BOYS HOSTEL" : "GIRLS HOSTEL";

        Paragraph pTitle = new Paragraph(roundTitle, fontDocType);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        pTitle.setSpacingBefore(4);
        pTitle.setSpacingAfter(2);
        document.add(pTitle);

        Paragraph pAcademic = new Paragraph("ACADEMIC YEAR 2026-2027", fontSubtitle);
        pAcademic.setAlignment(Element.ALIGN_CENTER);
        document.add(pAcademic);

        Paragraph pWingYear = new Paragraph(wingLabel + "  —  " + yearLabel, fontMeta);
        pWingYear.setAlignment(Element.ALIGN_CENTER);
        pWingYear.setSpacingAfter(8);
        document.add(pWingYear);

        // Divider
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell divCell = new PdfPCell();
        divCell.setFixedHeight(2);
        divCell.setBackgroundColor("SPOT".equalsIgnoreCase(round) ? COLOR_SPOT_HEADER_BG : COLOR_PRIMARY_NAVY);
        divCell.setBorder(Rectangle.NO_BORDER);
        divider.addCell(divCell);
        divider.setSpacingAfter(10);
        document.add(divider);

        // Sort students
        if ("SPOT".equalsIgnoreCase(round)) {
            filteredAllotments.sort(Comparator.comparing(Allotment::getAggregate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Allotment::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        } else {
            filteredAllotments.sort(Comparator.comparing(Allotment::getMeritRank, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Allotment::getAggregate, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        // Group by Branch
        Map<String, List<Allotment>> branchMap = new LinkedHashMap<>();
        for (String branchKey : BRANCH_FULL_NAMES.keySet()) {
            List<Allotment> branchStudents = filteredAllotments.stream()
                    .filter(a -> branchKey.equalsIgnoreCase(a.getBranch()))
                    .collect(Collectors.toList());

            if (!branchStudents.isEmpty()) {
                branchMap.put(branchKey, branchStudents);
            }
        }

        // Also check any other unlisted branches
        List<Allotment> otherStudents = filteredAllotments.stream()
                .filter(a -> a.getBranch() != null && !BRANCH_FULL_NAMES.containsKey(a.getBranch().toUpperCase()))
                .collect(Collectors.toList());

        if (!otherStudents.isEmpty()) {
            branchMap.put("OTHER", otherStudents);
        }

        // If no students in entire year+gender
        if (branchMap.isEmpty()) {
            PdfPTable emptyTable = new PdfPTable(1);
            emptyTable.setWidthPercentage(100);
            PdfPCell emptyCell = new PdfPCell(new Phrase("No hostel seat allotments recorded for " + wingLabel + " - " + yearLabel + " (" + ("SPOT".equalsIgnoreCase(round) ? "Spot Round" : "Regular Allotment") + ").", fontMeta));
            emptyCell.setPadding(15);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setBackgroundColor(COLOR_ROW_ALT);
            emptyCell.setBorderColor(COLOR_BORDER);
            emptyTable.addCell(emptyCell);
            emptyTable.setSpacingAfter(15);
            document.add(emptyTable);
        } else {
            // Render each Branch Table
            for (Map.Entry<String, List<Allotment>> entry : branchMap.entrySet()) {
                String branchKey = entry.getKey();
                String branchDisplayName = BRANCH_FULL_NAMES.getOrDefault(branchKey, branchKey + " ENGINEERING");
                List<Allotment> students = entry.getValue();

                renderBranchTable(document, yearLabel, branchDisplayName, branchKey, students, round);
            }
        }

        // Render Notes Section
        renderNotesSection(document, round);

        // Render Signature Block
        renderSignatureSection(document);
    }


    // =====================================================
    // 4. RENDER BRANCH TABLE
    // =====================================================

    private void renderBranchTable(
            Document document,
            String yearLabel,
            String branchDisplayName,
            String branchCode,
            List<Allotment> students,
            String round) throws DocumentException {

        Font fontBranchHeader = new Font(Font.FontFamily.HELVETICA, 9.5f, Font.BOLD, COLOR_PRIMARY_NAVY);
        Font fontTableHead = new Font(Font.FontFamily.HELVETICA, 8.5f, Font.BOLD, BaseColor.WHITE);
        Font fontBody = new Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, COLOR_PRIMARY_DARK);
        Font fontBodyBold = new Font(Font.FontFamily.HELVETICA, 8f, Font.BOLD, COLOR_PRIMARY_DARK);
        Font fontSeatNumber = new Font(Font.FontFamily.HELVETICA, 8f, Font.BOLD, "SPOT".equalsIgnoreCase(round) ? COLOR_SPOT_HEADER_BG : COLOR_PRIMARY_NAVY);

        // Branch Section Header
        PdfPTable branchHeaderTable = new PdfPTable(1);
        branchHeaderTable.setWidthPercentage(100);
        PdfPCell bCell = new PdfPCell(new Phrase(yearLabel.toUpperCase() + "  —  " + branchDisplayName + "  (" + students.size() + " Allotted)", fontBranchHeader));
        bCell.setBackgroundColor(COLOR_BRANCH_BAR);
        bCell.setBorderColor(COLOR_BORDER);
        bCell.setPadding(5);
        bCell.setPaddingLeft(8);
        branchHeaderTable.addCell(bCell);
        branchHeaderTable.setSpacingBefore(8);
        branchHeaderTable.setSpacingAfter(3);
        document.add(branchHeaderTable);

        // 6 Columns Table
        // S.N. | NAME OF STUDENT | BRANCH | CATEGORY | SECOND SEM % | ALLOTTED SEAT TYPE
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setWidths(new float[]{8f, 32f, 12f, 14f, 16f, 18f});

        BaseColor headerBg = "SPOT".equalsIgnoreCase(round) ? COLOR_SPOT_HEADER_BG : COLOR_HEADER_BG;

        // Table Headers
        String[] headers = {"S.N.", "NAME OF STUDENT", "BRANCH", "CATEGORY", "SECOND SEM %", "ALLOTTED SEAT TYPE"};
        for (int i = 0; i < headers.length; i++) {
            PdfPCell th = new PdfPCell(new Phrase(headers[i], fontTableHead));
            th.setBackgroundColor(headerBg);
            th.setBorderColor(headerBg);
            th.setPaddingTop(5);
            th.setPaddingBottom(5);
            th.setPaddingLeft(4);
            th.setPaddingRight(4);
            if (i == 0 || i == 2 || i == 3 || i == 4 || i == 5) {
                th.setHorizontalAlignment(Element.ALIGN_CENTER);
            } else {
                th.setHorizontalAlignment(Element.ALIGN_LEFT);
            }
            table.addCell(th);
        }

        // Table Rows
        int sn = 1;
        for (Allotment a : students) {
            BaseColor rowBg = (sn % 2 == 0) ? COLOR_ROW_ALT : COLOR_ROW_WHITE;

            // 1. S.N.
            PdfPCell cSn = new PdfPCell(new Phrase(String.valueOf(sn), fontBodyBold));
            cSn.setHorizontalAlignment(Element.ALIGN_CENTER);
            cSn.setBackgroundColor(rowBg);
            cSn.setBorderColor(COLOR_BORDER);
            cSn.setPadding(4.5f);
            table.addCell(cSn);

            // 2. NAME OF STUDENT
            String studentName = "Student";
            if (a.getApplication() != null && a.getApplication().getFullName() != null) {
                studentName = a.getApplication().getFullName().toUpperCase();
            } else if (a.getMeritList() != null && a.getMeritList().getStudentName() != null) {
                studentName = a.getMeritList().getStudentName().toUpperCase();
            }

            PdfPCell cName = new PdfPCell(new Phrase(studentName, fontBodyBold));
            cName.setHorizontalAlignment(Element.ALIGN_LEFT);
            cName.setBackgroundColor(rowBg);
            cName.setBorderColor(COLOR_BORDER);
            cName.setPadding(4.5f);
            table.addCell(cName);

            // 3. BRANCH
            String branchCodeDisplay = getShortBranchCode(a.getBranch() != null ? a.getBranch() : branchCode);
            PdfPCell cBranch = new PdfPCell(new Phrase(branchCodeDisplay, fontBody));
            cBranch.setHorizontalAlignment(Element.ALIGN_CENTER);
            cBranch.setBackgroundColor(rowBg);
            cBranch.setBorderColor(COLOR_BORDER);
            cBranch.setPadding(4.5f);
            table.addCell(cBranch);

            // 4. CATEGORY (Student's original category)
            String category = a.getCategory() != null ? a.getCategory().toUpperCase() : "OPEN";
            PdfPCell cCat = new PdfPCell(new Phrase(category, fontBodyBold));
            cCat.setHorizontalAlignment(Element.ALIGN_CENTER);
            cCat.setBackgroundColor(rowBg);
            cCat.setBorderColor(COLOR_BORDER);
            cCat.setPadding(4.5f);
            table.addCell(cCat);

            // 5. SECOND SEM % / AGGREGATE
            String percentageStr = "--";
            if (a.getApplication() != null && a.getApplication().getSem2Percentage() != null) {
                percentageStr = String.format("%.2f", a.getApplication().getSem2Percentage());
            } else if (a.getAggregate() != null) {
                percentageStr = String.format("%.2f", a.getAggregate());
            }
            PdfPCell cPercent = new PdfPCell(new Phrase(percentageStr, fontBody));
            cPercent.setHorizontalAlignment(Element.ALIGN_CENTER);
            cPercent.setBackgroundColor(rowBg);
            cPercent.setBorderColor(COLOR_BORDER);
            cPercent.setPadding(4.5f);
            table.addCell(cPercent);

            // 6. ALLOTTED SEAT TYPE
            String seatTypeDisplay = formatAllottedSeatType(a, round);
            PdfPCell cSeat = new PdfPCell(new Phrase(seatTypeDisplay, fontSeatNumber));
            cSeat.setHorizontalAlignment(Element.ALIGN_CENTER);
            cSeat.setBackgroundColor(rowBg);
            cSeat.setBorderColor(COLOR_BORDER);
            cSeat.setPadding(4.5f);
            table.addCell(cSeat);

            sn++;
        }

        table.setSpacingAfter(10);
        document.add(table);
    }


    // =====================================================
    // 5. RENDER NOTES SECTION
    // =====================================================

    private void renderNotesSection(Document document, String round) throws DocumentException {
        Font fontMarathiTitle = getMarathiFont(8.5f, Font.BOLD, COLOR_PRIMARY_NAVY);
        Font fontMarathiBody = getMarathiFont(7.8f, Font.NORMAL, COLOR_PRIMARY_DARK);

        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(100);
        noteTable.setSpacingBefore(10);
        noteTable.setSpacingAfter(15);

        PdfPCell noteCell = new PdfPCell();
        noteCell.setBackgroundColor(COLOR_NOTE_BG);
        noteCell.setBorderColor(COLOR_NOTE_BORDER);
        noteCell.setBorderWidth(1f);
        noteCell.setPadding(8);

        if ("SPOT".equalsIgnoreCase(round)) {
            Paragraph pHead = new Paragraph("टीप :", fontMarathiTitle);
            pHead.setSpacingAfter(3);
            noteCell.addElement(pHead);
            noteCell.addElement(new Paragraph("१. सदर यादी Spot Round साठी आहे.", fontMarathiBody));
            noteCell.addElement(new Paragraph("२. Spot Round मध्ये उपलब्ध रिक्त जागांनुसार विद्यार्थ्यांना वसतिगृहातील जागा देण्यात आली आहे.", fontMarathiBody));
            noteCell.addElement(new Paragraph("३. Spot Round मधील जागा संबंधित Hostel Gender + Year च्या उपलब्धतेवर आधारित आहेत.", fontMarathiBody));
            noteCell.addElement(new Paragraph("४. Branch-wise vacancy restriction Spot Round मध्ये लागू नाही.", fontMarathiBody));
            noteCell.addElement(new Paragraph("५. अंतिम प्रवेश प्रशासकीय मंजुरी व कागदपत्र पडताळणीनंतर निश्चित करण्यात येईल.", fontMarathiBody));
        } else {
            Paragraph pHead = new Paragraph("टीप :", fontMarathiTitle);
            pHead.setSpacingAfter(3);
            noteCell.addElement(pHead);
            noteCell.addElement(new Paragraph("१. सदर यादी तात्पुरती / Provisional आहे.", fontMarathiBody));
            noteCell.addElement(new Paragraph("२. वसतिगृहातील जागांची उपलब्धता व प्रशासकीय मंजुरीनुसार अंतिम वाटप करण्यात येईल.", fontMarathiBody));
            noteCell.addElement(new Paragraph("३. विद्यार्थ्यांची कागदपत्रे व पात्रता तपासणी आवश्यक आहे.", fontMarathiBody));
            noteCell.addElement(new Paragraph("४. सदर वाटप लागू असलेल्या गुणवत्ता व आरक्षण नियमांनुसार करण्यात आले आहे.", fontMarathiBody));
        }

        noteTable.addCell(noteCell);
        document.add(noteTable);
    }

    private Font getMarathiFont(float size, int style, BaseColor color) {
        try {
            String fontPath = "C:/Windows/Fonts/Nirmala.ttc,0";
            java.io.File f = new java.io.File("C:/Windows/Fonts/Nirmala.ttc");
            if (f.exists()) {
                BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(bf, size, style, color);
            }
        } catch (Exception ignored) {
        }
        return new Font(Font.FontFamily.HELVETICA, size, style, color);
    }


    // =====================================================
    // 6. RENDER SIGNATURE SECTION
    // =====================================================

    private void renderSignatureSection(Document document) throws DocumentException {
        Font fontSigTitle = new Font(Font.FontFamily.HELVETICA, 8.5f, Font.BOLD, COLOR_PRIMARY_NAVY);
        Font fontSigCollege = new Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, COLOR_PRIMARY_DARK);

        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.setSpacingBefore(20);
        sigTable.setKeepTogether(true);

        PdfPCell cellLeft = new PdfPCell();
        cellLeft.setBorder(Rectangle.NO_BORDER);
        cellLeft.addElement(new Paragraph("____________________________________", fontSigCollege));
        cellLeft.addElement(new Paragraph("Hostel Superintendent / Warden", fontSigTitle));
        cellLeft.addElement(new Paragraph("Government Polytechnic Murtizapur", fontSigCollege));

        PdfPCell cellRight = new PdfPCell();
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pLine = new Paragraph("____________________________________", fontSigCollege);
        pLine.setAlignment(Element.ALIGN_RIGHT);
        Paragraph pPrincipal = new Paragraph("Principal", fontSigTitle);
        pPrincipal.setAlignment(Element.ALIGN_RIGHT);
        Paragraph pInst = new Paragraph("Government Polytechnic Murtizapur", fontSigCollege);
        pInst.setAlignment(Element.ALIGN_RIGHT);

        cellRight.addElement(pLine);
        cellRight.addElement(pPrincipal);
        cellRight.addElement(pInst);

        sigTable.addCell(cellLeft);
        sigTable.addCell(cellRight);
        document.add(sigTable);
    }


    // =====================================================
    // 7. HELPER: ALLOTTED SEAT TYPE FORMATTER
    // =====================================================

    private String formatAllottedSeatType(Allotment a, String round) {
        if ("SPOT".equalsIgnoreCase(round) || "SPOT".equalsIgnoreCase(a.getAllotmentRound())) {
            return a.getSeatNumber() != null ? a.getSeatNumber() : "SPOT-ALLOTTED";
        }

        if (Boolean.TRUE.equals(a.getIsConverted())) {
            // Converted seat shows OPEN-XX
            if (a.getSeatNumber() != null && a.getSeatNumber().contains("OP")) {
                return a.getSeatNumber();
            }
            return "OPEN (Converted)";
        }

        if (a.getSeatNumber() != null && !a.getSeatNumber().trim().isEmpty()) {
            return a.getSeatNumber();
        }

        if (a.getAllotmentCategory() != null) {
            return a.getAllotmentCategory();
        }

        return "ALLOTTED";
    }


    // =====================================================
    // 8. HELPER: YEAR LABEL FORMATTER
    // =====================================================

    private String getYearLabel(String year) {
        if ("1".equals(year)) return "FIRST YEAR";
        if ("2".equals(year)) return "SECOND YEAR";
        if ("3".equals(year)) return "THIRD YEAR";
        return "YEAR " + year;
    }


    // =====================================================
    // 9. HELPER: SHORT BRANCH CODE
    // =====================================================

    private String getShortBranchCode(String branch) {
        if (branch == null) return "GEN";
        switch (branch.trim().toUpperCase()) {
            case "COMPUTER":
            case "CO":
                return "CO";
            case "MECHANICAL":
            case "ME":
                return "ME";
            case "CIVIL":
            case "CE":
                return "CE";
            case "ELECTRICAL":
            case "EE":
                return "EE";
            case "INFORMATION TECHNOLOGY":
            case "IT":
            case "IF":
                return "IF";
            default:
                return branch.length() > 4 ? branch.substring(0, 4).toUpperCase() : branch.toUpperCase();
        }
    }


    // =====================================================
    // 10. DYNAMIC FILE NAME GENERATOR
    // =====================================================

    public String generatePdfFileName(String gender, String year, String round) {
        String g = "GIRLS".equalsIgnoreCase(gender) ? "Girls" : "Boys";
        String y = "1".equals(year) ? "1stYear" : ("2".equals(year) ? "2ndYear" : "3rdYear");
        String r = "SPOT".equalsIgnoreCase(round) ? "Spot_Round" : "Regular_Allotment";
        return String.format("%s_%s_%s_2026-27.pdf", g, y, r);
    }


    // =====================================================
    // 11. PDF PAGE EVENT HELPER (PAGE NUMBERS & HEADER)
    // =====================================================

    private static class HeaderFooterEvent extends PdfPageEventHelper {
        private PdfTemplate totalPagesTemplate;
        private BaseFont baseFont;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
            } catch (Exception e) {
                // fallback
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            int pageN = writer.getPageNumber();

            // Footer
            String footerText = "Government Polytechnic Murtizapur — Hostel Allotment List 2026-27";
            String pageText = "Page " + pageN + " of ";

            cb.beginText();
            cb.setFontAndSize(baseFont, 7.5f);
            cb.setColorFill(new BaseColor(100, 116, 139)); // Slate 500

            // Left footer
            cb.showTextAligned(Element.ALIGN_LEFT, footerText, document.left(), document.bottom() - 15, 0);

            // Right footer with total page template
            float pageTextWidth = baseFont.getWidthPoint(pageText, 7.5f);
            cb.showTextAligned(Element.ALIGN_RIGHT, pageText, document.right() - 15, document.bottom() - 15, 0);
            cb.endText();

            cb.addTemplate(totalPagesTemplate, document.right() - 15, document.bottom() - 15);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPagesTemplate != null) {
                totalPagesTemplate.beginText();
                totalPagesTemplate.setFontAndSize(baseFont, 7.5f);
                totalPagesTemplate.setColorFill(new BaseColor(100, 116, 139));
                totalPagesTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
                totalPagesTemplate.endText();
            }
        }
    }
}
