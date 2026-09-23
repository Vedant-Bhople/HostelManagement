package com.hostel.service.pdf;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hostel.model.MeritList;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class MeritPdfService {

    // Color Palette
    private static final BaseColor PRIMARY_COLOR = new BaseColor(30, 58, 138); // Deep Navy #1e3a8a
    private static final BaseColor SECONDARY_COLOR = new BaseColor(71, 85, 105); // Slate #475569
    private static final BaseColor HEADER_BG = new BaseColor(241, 245, 249); // Slate-100 #f1f5f9
    private static final BaseColor ROW_ALT_BG = new BaseColor(248, 250, 252); // Slate-50 #f8fafc
    private static final BaseColor BORDER_COLOR = new BaseColor(203, 213, 225); // Slate-300 #cbd5e1

    // Fonts
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PRIMARY_COLOR);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, SECONDARY_COLOR);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY_COLOR);
    private static final Font FONT_META = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, BaseColor.DARK_GRAY);
    private static final Font FONT_META_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, PRIMARY_COLOR);
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, PRIMARY_COLOR);
    private static final Font FONT_TD = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, BaseColor.BLACK);
    private static final Font FONT_TD_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, BaseColor.BLACK);

    /**
     * Generates a professional PDF document for the filtered Merit List.
     */
    public byte[] generateMeritListPdf(
            List<MeritList> meritList,
            String academicYear,
            String admissionYear,
            String branch,
            String gender) throws DocumentException {

        Document document = new Document(PageSize.A4.rotate(), 30, 30, 36, 45);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = PdfWriter.getInstance(document, out);
        FooterPageEvent event = new FooterPageEvent();
        writer.setPageEvent(event);

        document.open();

        // 1. Institution Header Banner
        Paragraph pCollege = new Paragraph("GOVERNMENT POLYTECHNIC MURTIZAPUR", FONT_TITLE);
        pCollege.setAlignment(Element.ALIGN_CENTER);
        pCollege.setSpacingAfter(2f);
        document.add(pCollege);

        Paragraph pHostel = new Paragraph("HOSTEL ADMISSION MANAGEMENT SYSTEM", FONT_SUBTITLE);
        pHostel.setAlignment(Element.ALIGN_CENTER);
        pHostel.setSpacingAfter(4f);
        document.add(pHostel);

        Paragraph pDocTitle = new Paragraph("OFFICIAL MERIT RANKING LIST", FONT_SECTION);
        pDocTitle.setAlignment(Element.ALIGN_CENTER);
        pDocTitle.setSpacingAfter(10f);
        document.add(pDocTitle);

        // 2. Metadata Information Box (Filters & Timestamps)
        PdfPTable metaTable = new PdfPTable(4);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{25f, 25f, 25f, 25f});
        metaTable.setSpacingAfter(12f);

        String effAdmYear = (admissionYear != null && !admissionYear.isEmpty() && !"ALL".equalsIgnoreCase(admissionYear))
                ? admissionYear : "ALL";
        String effBranch = (branch != null && !branch.isEmpty() && !"ALL".equalsIgnoreCase(branch))
                ? branch : "ALL BRANCHES";
        String effGender = (gender != null && !gender.isEmpty() && !"ALL".equalsIgnoreCase(gender))
                ? gender : "ALL (BOYS + GIRLS)";
        String timestamp = new SimpleDateFormat("dd-MMM-yyyy hh:mm a").format(new Date());

        addMetaCell(metaTable, "Academic Year:", "Year " + (academicYear != null ? academicYear : "3"));
        addMetaCell(metaTable, "Admission Year:", effAdmYear);
        addMetaCell(metaTable, "Branch Filter:", effBranch);
        addMetaCell(metaTable, "Gender Filter:", effGender);
        addMetaCell(metaTable, "Total Applicants:", String.valueOf(meritList != null ? meritList.size() : 0));
        addMetaCell(metaTable, "Ranking Pool:", "Common Unified Merit");
        addMetaCell(metaTable, "Generated On:", timestamp);
        addMetaCell(metaTable, "Status:", "Official Published");

        document.add(metaTable);

        // 3. Merit List Data Table
        // Columns: Sr | Merit No | Form No | Student Name | Gender | Branch | Year | Category | Aggregate % | Status
        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5f, 8f, 9f, 23f, 8f, 14f, 6f, 9f, 10f, 8f});
        table.setHeaderRows(1); // Repeat header across all pages

        // Table Header
        addTableHeaderCell(table, "Sr.");
        addTableHeaderCell(table, "Merit No");
        addTableHeaderCell(table, "Form No");
        addTableHeaderCell(table, "Student Full Name");
        addTableHeaderCell(table, "Gender");
        addTableHeaderCell(table, "Branch");
        addTableHeaderCell(table, "Class");
        addTableHeaderCell(table, "Category");
        addTableHeaderCell(table, "Aggregate %");
        addTableHeaderCell(table, "Status");

        if (meritList == null || meritList.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No merit list records found matching selected criteria.", FONT_TD));
            emptyCell.setColspan(10);
            emptyCell.setPadding(15);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(emptyCell);
        } else {
            int srNo = 1;
            for (MeritList m : meritList) {
                boolean isAlt = (srNo % 2 == 0);
                BaseColor rowBg = isAlt ? ROW_ALT_BG : BaseColor.WHITE;

                // Sr. No
                addTableCell(table, String.valueOf(srNo), FONT_TD, Element.ALIGN_CENTER, rowBg);

                // Merit No (Strictly Preserves Overall Rank)
                addTableCell(table, "#" + (m.getMeritRank() != null ? m.getMeritRank() : "--"), FONT_TD_BOLD, Element.ALIGN_CENTER, rowBg);

                // Form No / Enrollment No
                String formNo = m.getEnrollmentNo() != null ? m.getEnrollmentNo()
                        : (m.getApplication() != null && m.getApplication().getId() != null ? "#" + m.getApplication().getId() : "--");
                addTableCell(table, formNo, FONT_TD, Element.ALIGN_CENTER, rowBg);

                // Student Full Name
                addTableCell(table, m.getStudentName() != null ? m.getStudentName() : "--", FONT_TD_BOLD, Element.ALIGN_LEFT, rowBg);

                // Gender
                addTableCell(table, m.getGender() != null ? m.getGender() : "--", FONT_TD, Element.ALIGN_CENTER, rowBg);

                // Branch
                addTableCell(table, m.getBranch() != null ? m.getBranch() : "--", FONT_TD, Element.ALIGN_LEFT, rowBg);

                // Year
                addTableCell(table, "Y" + (m.getYear() != null ? m.getYear() : "--"), FONT_TD, Element.ALIGN_CENTER, rowBg);

                // Category
                addTableCell(table, m.getCategory() != null ? m.getCategory() : "--", FONT_TD_BOLD, Element.ALIGN_CENTER, rowBg);

                // Marks / Aggregate %
                String aggStr = m.getAggregate() != null ? String.format("%.2f%%", m.getAggregate()) : "--";
                addTableCell(table, aggStr, FONT_TD_BOLD, Element.ALIGN_RIGHT, rowBg);

                // Remarks / Status
                String statusStr = m.getMeritStatus() != null ? m.getMeritStatus() : "ELIGIBLE";
                addTableCell(table, statusStr, FONT_TD, Element.ALIGN_CENTER, rowBg);

                srNo++;
            }
        }

        document.add(table);

        // 4. Verification Sign-off Box
        Paragraph pSign = new Paragraph("\n\nVerified by: ________________________                     Approved by (Rector/Principal): ________________________", FONT_META);
        pSign.setAlignment(Element.ALIGN_CENTER);
        document.add(pSign);

        document.close();
        return out.toByteArray();
    }

    private void addMetaCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(4f);

        Paragraph p = new Paragraph();
        p.add(new Phrase(label + " ", FONT_META));
        p.add(new Phrase(value, FONT_META_BOLD));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TH));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int align, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    /**
     * Helper for page numbers and running footer.
     */
    private static class FooterPageEvent extends PdfPageEventHelper {
        private PdfTemplate totalPages;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPages = writer.getDirectContent().createTemplate(30, 16);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(2);
            try {
                footer.setWidths(new int[]{70, 30});
                footer.setTotalWidth(document.getPageSize().getWidth() - 60);
                footer.getDefaultCell().setBorder(Rectangle.TOP);
                footer.getDefaultCell().setBorderColor(BORDER_COLOR);
                footer.getDefaultCell().setPaddingTop(4);

                // Left footer: Institution notice
                PdfPCell leftCell = new PdfPCell(new Phrase("Government Polytechnic Murtizapur - Official Hostel Merit Record", FONT_META));
                leftCell.setBorder(Rectangle.TOP);
                leftCell.setBorderColor(BORDER_COLOR);
                leftCell.setPaddingTop(4);
                footer.addCell(leftCell);

                // Right footer: Page numbers
                String pageText = String.format("Page %d of ", writer.getPageNumber());
                Phrase p = new Phrase(pageText, FONT_META);
                PdfPCell rightCell = new PdfPCell(p);
                rightCell.setBorder(Rectangle.TOP);
                rightCell.setBorderColor(BORDER_COLOR);
                rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                rightCell.setPaddingTop(4);
                footer.addCell(rightCell);

                footer.writeSelectedRows(0, -1, 30, 25, writer.getDirectContent());

                // Add template for total pages
                ColumnText.showTextAligned(
                        writer.getDirectContent(),
                        Element.ALIGN_LEFT,
                        new Phrase(String.valueOf(writer.getPageNumber()), FONT_META),
                        document.getPageSize().getWidth() - 30,
                        17,
                        0
                );

            } catch (Exception e) {
                // Ignore footer exception
            }
        }
    }
}
