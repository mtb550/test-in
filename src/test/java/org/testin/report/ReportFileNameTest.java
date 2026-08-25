package org.testin.report;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * What a report is called before the tester changes it.
 * <p>
 * The name has to survive being saved, mailed and filed next to reports from
 * other projects, so it carries the project, the run and when it was made - and
 * it has to be a name a file system will actually accept.
 */
public class ReportFileNameTest {

    private static final @NotNull ZonedDateTime AT =
            ZonedDateTime.of(2026, 8, 25, 21, 40, 15, 0, ZoneId.of("Asia/Riyadh"));

    @Test
    public void itSaysTheProjectTheRunAndWhen() {
        assertEquals(ReportFileName.of("Nafath", "Sprint 7 Cycle 3", AT),
                "TestRun_Nafath_Sprint7Cycle3_25-08-2026_09-40-15PM");
    }

    /**
     * The one that would have bitten at the Save button. A colon is forbidden in
     * a Windows file name and is shown as a slash on macOS, so a time written
     * the way a clock writes it cannot be a file name.
     */
    @Test
    public void theTimeCarriesNoColon() {
        final @NotNull String name = ReportFileName.of("Nafath", "Sprint 7", AT);

        assertFalse(name.contains(":"), "a colon in a file name is refused by Windows outright: " + name);
        assertTrue(name.endsWith("PM"), "the hour is the tester's own twelve-hour clock: " + name);
    }

    /**
     * Whatever a tester called their run, the report is still a file. Run names
     * are free text and the obvious ones - "API / UI", "v1.2:final" - are not.
     */
    @Test
    public void aRunNamedWithPunctuationStillMakesAFileName() {
        final @NotNull String name = ReportFileName.of("Nafath", "API / UI: v1.2", AT);

        for (final String forbidden : new String[]{"/", "\\", ":", "*", "?", "\"", "<", ">", "|"}) {
            assertFalse(name.contains(forbidden), "kept " + forbidden + " in " + name);
        }
    }

    /**
     * A repository that names no project leaves the part out rather than leaving
     * a gap where it would have been.
     */
    @Test
    public void anUnnamedProjectLeavesNoGap() {
        assertEquals(ReportFileName.of("", "Sprint 7", AT), "TestRun_Sprint7_25-08-2026_09-40-15PM");
    }

    /**
     * No spaces. A name a tester reads happily is a name they then have to quote
     * on a command line, and one that arrives with %20 through half the tools it
     * is sent through.
     */
    @Test
    public void theNameCarriesNoSpaces() {
        assertFalse(ReportFileName.of("Nafath Test", "Sprint 7 Cycle 3", AT).contains(" "));
    }

    /**
     * Two reports of the same run are two files. Regenerating after fixing a
     * verdict should not ask whether to overwrite what was sent an hour ago.
     */
    @Test
    public void asecondReportOfTheSameRunIsASecondFile() {
        assertFalse(ReportFileName.of("Nafath", "Sprint 7", AT)
                .equals(ReportFileName.of("Nafath", "Sprint 7", AT.plusSeconds(1))));
    }
}
