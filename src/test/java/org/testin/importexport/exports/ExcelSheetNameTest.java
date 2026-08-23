package org.testin.importexport.exports;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

/**
 * Sheet names an export can actually write.
 * <p>
 * A workbook is one sheet per test set, named after it, and Excel is strict
 * about both: at most 31 characters, none of {@code \ / * ? [ ] :}, and no two
 * sheets sharing a name whatever their case. POI enforces all of it by throwing,
 * so a name this method gets wrong is an export that dies rather than one that
 * looks odd.
 * <p>
 * Two test sets collide more easily than their names suggest. They are distinct
 * on disk and still collide here, because the characters Excel refuses are
 * replaced - "A/B" and "A*B" both become "A_B" - and anything past 31 characters
 * is cut, which collapses names that differ only in their tail.
 * <p>
 * The previous attempt at that retried with {@code name.substring(0, 28) +
 * "..."}, which failed both ways round: a name shorter than 28 characters threw
 * out of substring and killed the export, and a longer one produced the very
 * same string on every pass, so the loop never ended. Both are pinned below.
 */
public class ExcelSheetNameTest {

    @Test
    public void aNameExcelAcceptsIsLeftAlone() {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                assertEquals(ExportExcel.uniqueSheetName(workbook, "Login"), "Login");
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * The crash: two short names that sanitize alike. "Login" is five
     * characters, and the old code asked for its first 28.
     */
    @Test
    public void aSecondShortSheetOfTheSameNameIsNumbered() {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                workbook.createSheet(ExportExcel.uniqueSheetName(workbook, "A/B"));

                assertEquals(ExportExcel.uniqueSheetName(workbook, "A*B"), "A_B (2)",
                        "both sanitize to A_B, so the second one takes a number");
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * The hang: a name at the limit, retried. Every pass used to rebuild the
     * same 28 characters and the same ellipsis.
     */
    @Test
    public void namesAtTheLimitStillGetDistinctSheets() {
        try {
            final String tooLong = "Regression suite for the checkout flow";

            try (Workbook workbook = new XSSFWorkbook()) {
                for (int i = 0; i < 5; i++) {
                    final String name = ExportExcel.uniqueSheetName(workbook, tooLong);

                    assertFalse(name.isBlank(), "a sheet needs a name");
                    assertTrue(name.length() <= 31, "Excel refuses more than 31 characters: " + name);
                    assertNull(workbook.getSheet(name), "name " + i + " was already taken: " + name);

                    workbook.createSheet(name);
                }

                assertEquals(workbook.getNumberOfSheets(), 5);
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Case is not a difference to Excel, so it must not be one here either.
     */
    @Test
    public void twoNamesDifferingOnlyInCaseAreStillTwoSheets() {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                workbook.createSheet(ExportExcel.uniqueSheetName(workbook, "Login"));

                final String second = ExportExcel.uniqueSheetName(workbook, "login");
                assertNotEquals(second.toLowerCase(), "login", "Excel refuses two sheets differing only in case");

                workbook.createSheet(second);
                assertEquals(workbook.getNumberOfSheets(), 2);
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * The characters the old regex never removed. A colon is illegal in Excel
     * and a leading quote is illegal at the ends, and POI throws on both.
     */
    @Test
    public void theCharactersExcelRefusesAreReplaced() {
        try {
            try (Workbook workbook = new XSSFWorkbook()) {
                final String name = ExportExcel.uniqueSheetName(workbook, "'Smoke: run [1]?");

                workbook.createSheet(name);
                assertEquals(workbook.getNumberOfSheets(), 1, "POI accepted the name");
                assertFalse(name.contains(":"), name);
                assertFalse(name.startsWith("'"), name);
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }
}
