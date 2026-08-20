package org.testin.util;

import org.testin.model.Config;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * A timestamp the plugin wrote is a timestamp the plugin can read (#66).
 * <p>
 * The import preview, a grid cell edit and a re-imported export all go through
 * one parser, and it knew only the plain spreadsheet shape - while every value
 * a tester sees is written in the plugin's own, "Wednesday 19-08-2026 At
 * 01:12:58 [Asia/Riyadh]". So exporting a sheet and importing it back read every
 * date as a failure and answered "now": the preview showed today for a case
 * created months ago, and nothing said so.
 */
public class TestDataParserDateTest {

    private final ZonedDateTime when = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusMonths(7);

    @Test
    public void readsBackWhatThePluginDisplays() {
        final String displayed = Display.formatDate(when);

        assertEquals(TestDataParser.date(displayed).toInstant(), when.toInstant(),
                "the export writes this shape, so the import has to read it");
    }

    @Test
    public void readsThePlainSpreadsheetShape() {
        final String plain = when.format(Config.EXCEL_DATE_FORMATTER);

        assertEquals(TestDataParser.date(plain).toLocalDateTime(), when.toLocalDateTime(),
                "a sheet from another tool carries this one");
    }

    /**
     * The weekday is decoration - the plugin derives it from the date - so a cell
     * whose weekday was edited, or copied from the row above, still reads. Refusing
     * it emptied the Updated At column of a sheet whose dates were perfectly good.
     */
    @Test
    public void readsADateWhoseWeekdayIsWrong() {
        final ZonedDateTime read = TestDataParser.date("Sunday 05-08-2026 At 02:13:07 [Asia/Riyadh]");

        assertEquals(read.getDayOfMonth(), 5);
        assertEquals(read.getMonthValue(), 8);
        assertEquals(read.getYear(), 2026);
    }

    @Test
    public void aBlankCellIsNoTimeAtAll() {
        // An absent column arrives here as "": both importers default it, and
        // the setter that hands it over declares it @NotNull (#71).
        assertTrue(Config.isNotExecuted(TestDataParser.date("")), "the file did not say when");
        assertTrue(Config.isNotExecuted(TestDataParser.date("   ")), "nor here");
    }

    @Test
    public void textThatIsNeitherShapeIsNoTimeEither() {
        assertTrue(Config.isNotExecuted(TestDataParser.date("last Tuesday")),
                "inventing 'now' for it is what put today's date on every imported case");
    }
}
