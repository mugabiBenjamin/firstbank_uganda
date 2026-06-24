package ug.firstbank.util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import ug.firstbank.model.AccountRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class PdfExporter {

    // ── Brand colours (First Bank Uganda) ───────────────────────────────────

    /** Primary brand colour — deep navy blue. */
    private static final BaseColor BRAND_PRIMARY   = new BaseColor(0x1A, 0x35, 0x6E);

    /** Accent colour — gold, used for table header stripe. */
    private static final BaseColor BRAND_ACCENT    = new BaseColor(0xC9, 0xA0, 0x2B);

    /** Light grey for alternating table rows. */
    private static final BaseColor ROW_ALT         = new BaseColor(0xF5, 0xF5, 0xF5);

    // ── Fonts ────────────────────────────────────────────────────────────────

    private static final Font FONT_TITLE  = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD,  18f, BRAND_PRIMARY);

    private static final Font FONT_SUB    = FontFactory.getFont(
            FontFactory.HELVETICA,       10f, BRAND_PRIMARY);

    private static final Font FONT_HEADER = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD,  10f, BaseColor.WHITE);

    private static final Font FONT_LABEL  = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD,  10f, BaseColor.DARK_GRAY);

    private static final Font FONT_VALUE  = FontFactory.getFont(
            FontFactory.HELVETICA,       10f, BaseColor.BLACK);

    private static final Font FONT_FOOTER = FontFactory.getFont(
            FontFactory.HELVETICA_OBLIQUE, 8f, BaseColor.GRAY);

    // ── Timestamp format ─────────────────────────────────────────────────────

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");

    private PdfExporter() {}

    // ── Public API ───────────────────────────────────────────────────────────

    public static void export(AccountRecord record, File destination)
            throws IOException, DocumentException {

        Document doc = new Document(PageSize.A4, 50f, 50f, 60f, 60f);

        try (FileOutputStream fos = new FileOutputStream(destination)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();

            doc.add(buildLetterhead());
            doc.add(Chunk.NEWLINE);
            doc.add(buildSectionTitle("New Account Opening — Confirmation"));
            doc.add(Chunk.NEWLINE);
            doc.add(buildSummaryTable(record));
            doc.add(Chunk.NEWLINE);
            doc.add(buildFooter(record));
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    // ── Private builders ─────────────────────────────────────────────────────

    /**
     * Builds the bank letterhead block (name + tagline).
     */
    private static Paragraph buildLetterhead() {
        Paragraph block = new Paragraph();
        block.setAlignment(Element.ALIGN_CENTER);
        block.add(new Chunk("FIRST BANK UGANDA\n", FONT_TITLE));
        block.add(new Chunk("Your Trusted Banking Partner", FONT_SUB));
        block.setSpacingAfter(4f);
        return block;
    }

    /**
     * Builds a bold section title paragraph.
     */
    private static Paragraph buildSectionTitle(String title) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BRAND_PRIMARY);
        Paragraph p = new Paragraph(title, f);
        p.setAlignment(Element.ALIGN_LEFT);
        p.setSpacingAfter(6f);
        return p;
    }

    /**
     * Builds the two-column label/value summary table from {@code record}.
     *
     * <p>Rows alternate between white and {@link #ROW_ALT} for readability.
     * The second NIN row is included only for Joint accounts.</p>
     */
    private static PdfPTable buildSummaryTable(AccountRecord record)
            throws DocumentException {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{35f, 65f});
        table.setSpacingAfter(8f);

        // Header row
        addHeaderCell(table, "Field");
        addHeaderCell(table, "Details");

        // Data rows — alternate background
        boolean alt = false;
        addRow(table, "Account Number",  record.getAccountNumber(),                        alt); alt = !alt;
        addRow(table, "Full Name",       record.getFirstName() + " " + record.getLastName(), alt); alt = !alt;
        addRow(table, "Account Type",    record.getAccountType(),                          alt); alt = !alt;
        addRow(table, "Branch",          record.getBranch(),                               alt); alt = !alt;
        addRow(table, "Date of Birth",   record.getDateOfBirth().toString(),               alt); alt = !alt;
        addRow(table, "National ID",     record.getNin(),                                  alt); alt = !alt;

        if (record.getSecondNin() != null) {
            addRow(table, "Spouse NIN", record.getSecondNin(), alt); alt = !alt;
        }

        addRow(table, "Email",           record.getEmail(),                                alt); alt = !alt;
        addRow(table, "Phone",           record.getPhone(),                                alt); alt = !alt;
        addRow(table, "Opening Deposit",
               CurrencyFormatter.format(record.getOpeningDeposit()),                       alt);

        return table;
    }

    /**
     * Adds a styled header cell to {@code table}.
     */
    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER));
        cell.setBackgroundColor(BRAND_PRIMARY);
        cell.setPadding(6f);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    /**
     * Adds a label/value row to {@code table} with optional alternating background.
     */
    private static void addRow(PdfPTable table,
                                String label,
                                String value,
                                boolean alternate) {
        BaseColor bg = alternate ? ROW_ALT : BaseColor.WHITE;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        labelCell.setBackgroundColor(bg);
        labelCell.setPadding(5f);
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(new BaseColor(0xDD, 0xDD, 0xDD));

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_VALUE));
        valueCell.setBackgroundColor(bg);
        valueCell.setPadding(5f);
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(new BaseColor(0xDD, 0xDD, 0xDD));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * Builds the footer paragraph containing the generation timestamp and
     * the one-line summary string from {@link AccountRecord#toSummaryLine()}.
     */
    private static Paragraph buildFooter(AccountRecord record) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_LEFT);
        p.add(new Chunk("Generated: " + timestamp + "\n", FONT_FOOTER));
        p.add(new Chunk(record.toSummaryLine(), FONT_FOOTER));
        p.setSpacingBefore(4f);
        return p;
    }
}