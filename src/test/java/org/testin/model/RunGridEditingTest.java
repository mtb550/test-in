package org.testin.model;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * What a tester may type into a run grid, and what a verdict takes away (#74).
 * <p>
 * The run grid was read-only and keyboard-dead. Making one column editable puts
 * two rules in tension, and both are checked here rather than on screen.
 * <p>
 * The first is that exactly one column takes typing. Everything else on a row is
 * either the test case's, which the run does not own, or a verdict, which has
 * its own key and clears things as it goes - a status typed into a cell would be
 * a fourth way to record one.
 * <p>
 * The second is what a pass erases. It always erased the four fields that
 * explain a failure, and that was invisible while nothing could be typed into a
 * run grid. Now a tester can write a paragraph into a cell, press P, and watch
 * it go - so the verdict has to know what it is about to take, in the same words
 * it will use to ask.
 */
public class RunGridEditingTest {

    private static TestRunItems item() {
        return TestRunItems.builder().id(UUID.randomUUID()).build();
    }

    @Test
    public void actualResultIsTheOneColumnATesterCanTypeInto() {
        final List<RunEditorAttributes> editable = Arrays.stream(RunEditorAttributes.values())
                .filter(RunEditorAttributes::isEdited)
                .toList();

        assertEquals(editable, List.of(RunEditorAttributes.ACTUAL_RESULT),
                "a status, a severity or a description typed into a cell is a way of recording it "
                        + "that nothing else in the plugin knows about");
    }

    @Test
    public void theVerdictColumnsRefuseEditingByTheirOwnDeclaration() {
        assertFalse(RunEditorAttributes.RUN_STATUS.isEdited(), "P, F and B set a status");
        assertFalse(RunEditorAttributes.BUG_SEVERITY.isEdited(), "the failure dialog collects this");
        assertFalse(RunEditorAttributes.BUG_PRIORITY.isEdited(), "and this");
        assertFalse(RunEditorAttributes.EXECUTED_BY.isEdited(), "recorded, not claimed");
        assertFalse(RunEditorAttributes.EXECUTED_AT.isEdited(), "recorded, not claimed");
    }

    @Test
    public void typingIntoTheCellIsWhatTheRunThenHolds() {
        final TestRunItems row = item();

        RunEditorAttributes.ACTUAL_RESULT.getRunValueSetter().execute(row, "The balance showed 0.00");

        assertEquals(row.getActualResult(), "The balance showed 0.00");
    }

    @Test
    public void aRowWithNothingTypedLosesNothingByPassing() {
        assertEquals(item().wouldClear(TestStatus.PASSED), List.of(),
                "the ordinary case asks the tester nothing");
    }

    @Test
    public void passingNamesEverythingItWouldErase() {
        final TestRunItems row = item();
        row.setActualResult("The balance showed 0.00");
        row.setStacktrace("java.lang.AssertionError");
        row.setBugSeverity(BugSeverity.MAJOR);
        row.setBugPriority(BugPriority.HIGH);

        assertEquals(row.wouldClear(TestStatus.PASSED),
                List.of("the actual result", "the stacktrace", "the bug severity", "the bug priority"));
    }

    @Test
    public void onlyWhatIsActuallyFilledInIsNamed() {
        final TestRunItems row = item();
        row.setActualResult("The balance showed 0.00");

        assertEquals(row.wouldClear(TestStatus.PASSED), List.of("the actual result"),
                "warning about three empty fields teaches the tester to dismiss the warning");
    }

    @Test
    public void aVerdictThatErasesNothingAsksNothing() {
        final TestRunItems row = item();
        row.setActualResult("The balance showed 0.00");

        assertEquals(row.wouldClear(TestStatus.FAILED), List.of(),
                "a failing case still has something to explain, so nothing is cleared");
        assertEquals(row.wouldClear(TestStatus.BLOCKED), List.of());
    }

    @Test
    public void passingStillClearsWhatItAlwaysCleared() {
        final TestRunItems row = item();
        row.setActualResult("The balance showed 0.00");
        row.setStacktrace("java.lang.AssertionError");
        row.setBugSeverity(BugSeverity.MAJOR);
        row.setBugPriority(BugPriority.HIGH);

        row.recordVerdict(TestStatus.PASSED, "mtb");

        assertEquals(row.getActualResult(), "");
        assertEquals(row.getStacktrace(), "");
        assertEquals(row.getBugSeverity(), BugSeverity.EMPTY);
        assertEquals(row.getBugPriority(), BugPriority.EMPTY);
        assertEquals(row.getStatus(), TestStatus.PASSED);
        assertEquals(row.getExecutedBy(), "mtb");
    }

    @Test
    public void failingKeepsWhatTheTesterTyped() {
        final TestRunItems row = item();
        row.setActualResult("The balance showed 0.00");

        row.recordVerdict(TestStatus.FAILED, "mtb");

        assertEquals(row.getActualResult(), "The balance showed 0.00",
                "the actual result is the point of a failure");
    }

    @Test
    public void whatAPassClearsAndWhatItWarnsAboutAreTheOneList() {
        final TestRunItems row = item();
        row.setActualResult("The balance showed 0.00");
        row.setBugSeverity(BugSeverity.MAJOR);

        final List<String> warned = row.wouldClear(TestStatus.PASSED);
        row.recordVerdict(TestStatus.PASSED, "mtb");

        assertTrue(row.wouldClear(TestStatus.PASSED).isEmpty(),
                "everything named in the warning is gone afterward, or the warning was wrong: " + warned);
        assertEquals(warned.size(), 2);
    }
}
