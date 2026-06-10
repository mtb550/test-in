package org.testin.util.reports;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.Tools;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestRunPdfGenerator {

    public byte[] generate(final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final Map<UUID, TestCaseDto> detailsMap) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            String cleanName = tr.getRunName().replace(".json", "");

            Paragraph header = new Paragraph("Test Run Report: " + cleanName)
                    .setFont(boldFont)
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(header);

            document.add(new Paragraph("Platform: " + tr.getPlatform()));
            document.add(new Paragraph("Status: " + trDir.getMarker().getStatus().name()).setMarginBottom(20));

            Table table = new Table(UnitValue.createPercentArray(new float[]{10, 50, 20, 20})).useAllAvailableWidth();

            table.addHeaderCell(createHeaderCell("#", boldFont));
            table.addHeaderCell(createHeaderCell("Title", boldFont));
            table.addHeaderCell(createHeaderCell("Status", boldFont));
            table.addHeaderCell(createHeaderCell("Duration", boldFont));

            if (!tr.getResults().isEmpty()) {
                AtomicInteger seq = new AtomicInteger(1);

                tr.getResults().forEach(result -> {
                    UUID id = result.getId();

                    table.addCell(new Cell().add(new Paragraph(String.valueOf(seq.getAndIncrement())))
                            .setTextAlignment(TextAlignment.CENTER));

                    TestCaseDto details = (detailsMap != null) ? detailsMap.get(id) : null;
                    String title = details != null ? details.getDescription() : "N/A";

                    table.addCell(new Cell().add(new Paragraph(title)));

                    TestStatus statusEnum = result.getStatus();
                    String statusText = statusEnum.name();
                    String hexColor = statusEnum.getHex();

                    DeviceRgb statusColor = new DeviceRgb(
                            Integer.valueOf(hexColor.substring(0, 2), 16),
                            Integer.valueOf(hexColor.substring(2, 4), 16),
                            Integer.valueOf(hexColor.substring(4, 6), 16)
                    );

                    Cell statusCell = new Cell().add(
                            new Paragraph(statusText)
                                    .setFont(boldFont)
                                    .setFontColor(statusColor)
                    );
                    table.addCell(statusCell);

                    String duration = Tools.getInstance().getFormattedDuration(result.getDuration());
                    table.addCell(new Cell().add(new Paragraph(duration != null ? duration : "N/A")));
                });
            } else {
                Cell emptyCell = new Cell(1, 4).add(new Paragraph("No test results found."))
                        .setTextAlignment(TextAlignment.CENTER);
                table.addCell(emptyCell);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        }
    }

    private Cell createHeaderCell(String text, PdfFont boldFont) {
        return new Cell()
                .add(new Paragraph(text).setFont(boldFont))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
    }
}