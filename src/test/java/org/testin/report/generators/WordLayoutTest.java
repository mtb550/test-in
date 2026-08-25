package org.testin.report.generators;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jetbrains.annotations.NotNull;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * The Word report's case tables let Word size their columns, and the document
 * survives being written and read back.
 * <p>
 * Both halves matter. A table that says autofit sizes its columns to the text,
 * so priority and severity take what their longest word needs and the test case
 * column keeps the rest - which is what the PDF and the HTML report already do.
 * And the way that is said in the file is a {@code <w:tcW>} element per cell: a
 * second one in the same cell is invalid, and Word answers invalid XML by
 * refusing to open the document rather than by ignoring the extra element. That
 * failure only shows up on the round trip.
 */
public class WordLayoutTest {

    @Test
    public void anAutofitTableSurvivesBeingWrittenAndReadBack() {
        try (ByteArrayOutputStream saved = new ByteArrayOutputStream()) {
            try (XWPFDocument written = new XWPFDocument()) {
                final @NotNull XWPFTable table = written.createTable(2, 4);

                // Widths set first, as the report used to: the cells already have
                // a tcW, which is the case the reuse has to survive.
                for (final XWPFTableRow row : table.getRows()) {
                    for (final XWPFTableCell cell : row.getTableCells()) {
                        cell.getCTTc().addNewTcPr().addNewTcW().setW(2500);
                    }
                }

                autoFit(table);
                written.write(saved);
            }

            try (XWPFDocument read = new XWPFDocument(new ByteArrayInputStream(saved.toByteArray()))) {
                final @NotNull XWPFTable table = read.getTables().getFirst();

                assertEquals(table.getCTTbl().getTblPr().getTblLayout().getType(), STTblLayoutType.AUTOFIT,
                        "the table should be marked autofit, or Word keeps the widths it was given");

                for (final XWPFTableRow row : table.getRows()) {
                    for (final XWPFTableCell cell : row.getTableCells()) {
                        assertEquals(cell.getCTTc().getTcPr().getTcW().getType(), STTblWidth.AUTO,
                                "every cell should ask for no particular width");
                        final @NotNull String cellXml = cell.getCTTc().getTcPr().xmlText();
                        assertEquals(cellXml.split("<w:tcW", -1).length - 1, 1,
                                "a cell with two width elements is invalid and Word refuses to open it: " + cellXml);
                    }
                }
            }

        } catch (final Exception cannotRun) {
            fail("Could not round-trip the table: " + cannotRun.getMessage(), cannotRun);
        }
    }

    /**
     * The same two steps the report performs, kept here so the test exercises the
     * shape rather than a copy of the wording.
     */
    private void autoFit(final @NotNull XWPFTable table) {
        final var properties = table.getCTTbl().getTblPr();
        final var layout = properties.isSetTblLayout() ? properties.getTblLayout() : properties.addNewTblLayout();
        layout.setType(STTblLayoutType.AUTOFIT);

        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final var cellProperties = cell.getCTTc().getTcPr() != null
                        ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
                (cellProperties.isSetTcW() ? cellProperties.getTcW() : cellProperties.addNewTcW())
                        .setType(STTblWidth.AUTO);
            }
        }
    }
}
