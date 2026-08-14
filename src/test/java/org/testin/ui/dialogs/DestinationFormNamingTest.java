package org.testin.ui.dialogs;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * How the export and report dialogs name the file they write.
 * <p>
 * There is a test because there was a bug: the extension used to be applied by
 * cutting at the last dot whatever followed it, so a report the tester named
 * "Sprint 1.2 Report" was written as "Sprint 1.pdf" - produced, under a name
 * they did not choose and would not find by searching for the one they typed.
 * <p>
 * The rule now is that only a tail which is itself a known extension is
 * replaced. Both halves of that matter, so both are here.
 */
public class DestinationFormNamingTest {

    @Test
    public void aNameWithNoDotGetsTheExtension() {
        assertEquals(DestinationForm.withExtension("Sprint report", ".pdf"), "Sprint report.pdf");
    }

    @Test
    public void aNameThatAlreadyEndsInItIsLeftAlone() {
        assertEquals(DestinationForm.withExtension("Sprint report.pdf", ".pdf"), "Sprint report.pdf");
    }

    @Test
    public void aKnownExtensionIsReplaced() {
        assertEquals(DestinationForm.withExtension("cases.xlsx", ".csv"), "cases.csv");
        assertEquals(DestinationForm.withExtension("cases.json", ".xlsx"), "cases.xlsx");
    }

    /**
     * The defect this test exists for.
     */
    @Test
    public void aVersionNumberIsNotAnExtension() {
        assertEquals(DestinationForm.withExtension("Sprint 1.2 Report", ".pdf"), "Sprint 1.2 Report.pdf");
        assertEquals(DestinationForm.withExtension("Release 2.0", ".xlsx"), "Release 2.0.xlsx");
        assertEquals(DestinationForm.withExtension("v1.2.3", ".csv"), "v1.2.3.csv");
    }

    @Test
    public void anUnknownSuffixIsKeptRatherThanCutOff() {
        // ".backup" is not a format this plugin writes, so it is part of the name.
        assertEquals(DestinationForm.withExtension("cases.backup", ".csv"), "cases.backup.csv");
    }

    @Test
    public void theReplacedExtensionIsMatchedWhateverItsCase() {
        assertEquals(DestinationForm.withExtension("cases.XLSX", ".csv"), "cases.csv");
    }
}
