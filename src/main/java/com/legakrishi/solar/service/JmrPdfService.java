package com.legakrishi.solar.service;

import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.model.MeterReading;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class JmrPdfService {

    private static final String GSTIN = "08AAIFL9284F1ZV";
    private static final String PAN = "AAIFL9284F";
    private static final String ADDRESS_LINE1 = "Aadarsh Visu Nagar, Post/GSS : Bhunia,";
    private static final String ADDRESS_LINE2 = "Teh : Dhanau, District: Barmer (Raj) - 344704";
    private static final String LOA = "313557";
    private static final String LOGO_PATH = "src/main/resources/static/img/legha_logo.png";

    public byte[] generateJmrPdf(JmrReport jmr) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float margin = 36f;
            float usableWidth = PDRectangle.A4.getWidth() - 2 * margin;
            float x = margin, y = PDRectangle.A4.getHeight() - margin;

            // HEADER: Logo Left, Content Centered
            float logoH = 40f, logoW = 70f;
            float headerH = 55f;
            float headerCenterX = x + usableWidth / 2f;
            float headerTopY = y - 4;

            // Draw logo
            try {
                PDImageXObject img = PDImageXObject.createFromFile(LOGO_PATH, doc);
                cs.drawImage(img, x, y - logoH + 8, logoW, logoH);
            } catch (Exception ex) { /* logo missing, skip */ }

            // Centered header text block
            cs.setFont(PDType1Font.HELVETICA_BOLD, 15);
            drawCentered(cs, "LEGHA KRISHI FARM", headerCenterX, headerTopY);

            cs.setFont(PDType1Font.HELVETICA, 10);
            drawCentered(cs, ADDRESS_LINE1, headerCenterX, headerTopY - 15);
            drawCentered(cs, ADDRESS_LINE2, headerCenterX, headerTopY - 29);
            drawCentered(cs, "LOA: " + LOA + "    GSTIN: " + GSTIN + "    PAN: " + PAN, headerCenterX, headerTopY - 43);

            // Line under header
            cs.setLineWidth(1.2f);
            cs.setStrokingColor(70, 70, 70);
            cs.moveTo(x, y - headerH); cs.lineTo(x + usableWidth, y - headerH); cs.stroke();
            cs.setStrokingColor(0, 0, 0);
            cs.setLineWidth(0.7f);

            y -= headerH + 20;

            // Report Title (centered, bold, after line)
            cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
            drawCentered(cs, "JOINT METER READING (JMR) REPORT", headerCenterX, y);
            y -= 22;

            // Plant info (centered)
            cs.setFont(PDType1Font.HELVETICA, 10);
            drawCentered(cs,
                    "Plant Name: " + safe(jmr.getPlantName())
                            + "     Month: " + safe(jmr.getMonth())
                            + "     Year: " + safe(String.valueOf(jmr.getYear()))
                            + "     Reading Date: " + safe(jmr.getReadingDate() != null ? jmr.getReadingDate().toString() : ""),
                    headerCenterX, y);
            y -= 20;

            // Main Meter Table
            y = drawStyledTable(cs, "MAIN METER", jmr.getMainMeter(), x, y, usableWidth);

            // Check Meter Table
            y -= 16;
            y = drawStyledTable(cs, "CHECK METER", jmr.getCheckMeter(), x, y, usableWidth);

            // Remarks (multi-line, left-aligned, paragraph style)
            y -= 15;
            cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
            float remarksLabelWidth = PDType1Font.HELVETICA_BOLD.getStringWidth("Remarks:") / 1000 * 10;
            float remarksX = x + remarksLabelWidth + 6; // A small gap after label
            float remarksWidth = usableWidth - (remarksX - x) + 6;
            cs.beginText(); cs.newLineAtOffset(x, y); cs.showText("Remarks:"); cs.endText();

            cs.setFont(PDType1Font.HELVETICA, 10);
            List<String> remarkLines = wrapText(safe(jmr.getRemarks()), remarksWidth, PDType1Font.HELVETICA, 10);
            for (String line : remarkLines) {
                cs.beginText();
                cs.newLineAtOffset(remarksX, y);
                cs.showText(line);
                cs.endText();
                y -= 13;
            }

            // Extra space for signatures
            y -= 36;

            // Signatures
            float boxHeight = 36, boxWidth = 200;
            cs.setFont(PDType1Font.HELVETICA, 11);
            cs.addRect(x, y - boxHeight, boxWidth, boxHeight); cs.stroke();
            cs.beginText(); cs.newLineAtOffset(x + 10, y - 16); cs.showText("Owner/Signatory"); cs.endText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
            cs.beginText(); cs.newLineAtOffset(x + 10, y - 28); cs.showText("Legha Krishi Farm, Bhunia"); cs.endText();

            cs.setFont(PDType1Font.HELVETICA, 11);
            cs.addRect(x + usableWidth - boxWidth, y - boxHeight, boxWidth, boxHeight); cs.stroke();
            cs.beginText(); cs.newLineAtOffset(x + usableWidth - boxWidth + 10, y - 16); cs.showText("Executive Engineer (M & P)"); cs.endText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
            cs.beginText(); cs.newLineAtOffset(x + usableWidth - boxWidth + 10, y - 28); cs.showText("JDVVNL, BARMER"); cs.endText();

            cs.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Draws a full-width, boxed, zebra-stripe table for JMR with explicit column header cell borders!
     */
    private float drawStyledTable(PDPageContentStream cs, String meterTitle, MeterReading m, float x, float y, float usableWidth) throws IOException {
        String[] header = {"Parameter", "Last Reading", "Reading on End of Month", "Diff", "MF", "Total"};
        float[] colWidths = {120, 68, 120, 48, 22, usableWidth - (120 + 68 + 120 + 48 + 22)};
        String[][] data = {
                // Labels swapped, values untouched
                {"Energy Import (kWh)", v(m.getKwhImportStart()), v(m.getKwhImportEnd()), v(m.getKwhImportEnd() - m.getKwhImportStart()), "1", v(m.getKwhImportEnd() - m.getKwhImportStart())},
                {"Energy Export (kWh)", v(m.getKwhExportStart()), v(m.getKwhExportEnd()), v(m.getKwhExportEnd() - m.getKwhExportStart()), "1", v(m.getKwhExportEnd() - m.getKwhExportStart())},
                {"kVAh Import", v(m.getKvahImportStart()), v(m.getKvahImportEnd()), v(m.getKvahImportEnd() - m.getKvahImportStart()), "1", v(m.getKvahImportEnd() - m.getKvahImportStart())},
                {"kVAh Export", v(m.getKvahExportStart()), v(m.getKvahExportEnd()), v(m.getKvahExportEnd() - m.getKvahExportStart()), "1", v(m.getKvahExportEnd() - m.getKvahExportStart())},
                {"kVArh Q1 (Lag Import)", v(m.getKvarhLagImportStart()), v(m.getKvarhLagImportEnd()), v(m.getKvarhLagImportEnd() - m.getKvarhLagImportStart()), "1", v(m.getKvarhLagImportEnd() - m.getKvarhLagImportStart())},
                {"kVArh Q2 (Lead Import)", v(m.getKvarhLeadImportStart()), v(m.getKvarhLeadImportEnd()), v(m.getKvarhLeadImportEnd() - m.getKvarhLeadImportStart()), "1", v(m.getKvarhLeadImportEnd() - m.getKvarhLeadImportStart())},
                {"kVArh Q3 (Lag Export)", v(m.getKvarhLagExportStart()), v(m.getKvarhLagExportEnd()), v(m.getKvarhLagExportEnd() - m.getKvarhLagExportStart()), "1", v(m.getKvarhLagExportEnd() - m.getKvarhLagExportStart())},
                {"kVArh Q4 (Lead Export)", v(m.getKvarhLeadExportStart()), v(m.getKvarhLeadExportEnd()), v(m.getKvarhLeadExportEnd() - m.getKvarhLeadExportStart()), "1", v(m.getKvarhLeadExportEnd() - m.getKvarhLeadExportStart())},
                {"Import Power Factor", "", "", "", "", v(m.getImportPowerFactor())},
                {"Export Power Factor", "", "", "", "", v(m.getExportPowerFactor())},
                {"Import Billing MD (kVA)", "", "", "", "", v(m.getImportBillingMD())},
                {"Export Billing MD (kVA)", "", "", "", "", v(m.getExportBillingMD())}
        };

        // Table title
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.beginText(); cs.newLineAtOffset(x, y); cs.showText(meterTitle); cs.endText();
        y -= 15;

        int rows = data.length + 1;
        int cols = header.length;
        float rowH = 15;
        float tableH = rows * rowH;

        // 1. Draw cell backgrounds & borders for the header (each cell individually)
        float xCol = x;
        for (int c = 0; c < cols; c++) {
            // Background color for header
            cs.setNonStrokingColor(214, 230, 248);
            cs.addRect(xCol, y - rowH, colWidths[c], rowH); cs.fill();
            cs.setNonStrokingColor(0, 0, 0);
            // Borders for header cell
            cs.setLineWidth(1.1f);
            cs.addRect(xCol, y - rowH, colWidths[c], rowH); cs.stroke();
            xCol += colWidths[c];
        }

        // 2. Draw header text, centered in each header cell
        cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        xCol = x;
        for (int c = 0; c < cols; c++) {
            String colText = header[c];
            float textW = PDType1Font.HELVETICA_BOLD.getStringWidth(colText) / 1000 * 9;
            float textX = xCol + (colWidths[c] - textW) / 2;
            cs.beginText();
            cs.newLineAtOffset(textX, y - rowH + 4);
            cs.showText(colText);
            cs.endText();
            xCol += colWidths[c];
        }

        // 3. Draw all borders for table body cells (each cell)
        for (int r = 0; r < data.length; r++) {
            xCol = x;
            for (int c = 0; c < cols; c++) {
                // Zebra background
                if (r % 2 == 1) {
                    cs.setNonStrokingColor(244, 248, 252);
                    cs.addRect(xCol, y - (r + 2) * rowH, colWidths[c], rowH); cs.fill();
                    cs.setNonStrokingColor(0, 0, 0);
                }
                // Borders
                cs.setLineWidth(0.9f);
                cs.addRect(xCol, y - (r + 2) * rowH, colWidths[c], rowH); cs.stroke();
                xCol += colWidths[c];
            }
        }

        // 4. Write table body text
        cs.setFont(PDType1Font.HELVETICA, 9);
        for (int r = 0; r < data.length; r++) {
            float colX = x;
            for (int c = 0; c < cols; c++) {
                cs.beginText();
                float offset = (c == 0) ? 4
                        : (colWidths[c] - textWidth(PDType1Font.HELVETICA, 9, data[r][c]) - 4);
                float textY = y - (r + 2) * rowH + 4;
                cs.newLineAtOffset(colX + offset, textY);
                cs.showText(data[r][c]);
                cs.endText();
                colX += colWidths[c];
            }
        }
        y -= tableH + 10;

        // Meter/seal info
        cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
        cs.beginText(); cs.newLineAtOffset(x, y); cs.showText("Meter No.: " + safe(m.getMeterName()) + "   Old Seal: " + safe(m.getSealOld()) + "   New Seal: " + safe(m.getSealNew())); cs.endText(); y -= 12;
        cs.beginText(); cs.newLineAtOffset(x, y); cs.showText("Meter Body: " + safe(m.getMeterBodyStatus()) + " | Terminal: " + safe(m.getTerminalStatus()) + " | Test Terminal Block: " + safe(m.getTestTerminalBlockStatus()) + " | Panel: " + safe(m.getPanelBoxStatus())); cs.endText();

        return y - 18;
    }

    // --- Utility: Centered text for header/title
    private void drawCentered(PDPageContentStream cs, String text, float centerX, float y) throws IOException {
        float width = PDType1Font.HELVETICA.getStringWidth(text) / 1000 * 12;
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 12);
        cs.newLineAtOffset(centerX - width / 2, y);
        cs.showText(text);
        cs.endText();
    }

    // --- Utility: Wrap long text into multiple lines to fit width
    private List<String> wrapText(String text, float maxWidth, PDType1Font font, int fontSize) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String temp = line.length() == 0 ? word : line + " " + word;
            float width = font.getStringWidth(temp) / 1000 * fontSize;
            if (width > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private float textWidth(PDType1Font font, int fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }

    private String v(Double d) {
        if (d == null) return "";
        return String.format("%.2f", d);
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replaceAll("[^\\x20-\\x7E]", " ");
    }
}
