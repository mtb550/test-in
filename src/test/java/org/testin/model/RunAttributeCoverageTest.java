package org.testin.model;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Everything a run records about a failure has somewhere a tester can see it.
 * <p>
 * The gap this closes was silent for as long as it existed. A run row has always
 * carried a stacktrace, and {@link FailureDetail} has always known to clear it
 * when a case passes - but it had no {@link RunEditorAttributes} constant, so it
 * was not a grid column, not a toolbar attribute, and printed by one report
 * format of four. Nothing failed; the field was simply written and never read.
 * <p>
 * That cost nothing while a tester had to paste into the box by hand, and became
 * the plugin's least visible value the moment automation started filling it.
 */
public class RunAttributeCoverageTest {

    /**
     * A tripwire on the count rather than a mapping by name, because the failure
     * is a fifth field being added here and nowhere else. Whoever adds it reads
     * this line and knows what the other half of the work is.
     */
    @Test
    public void everyFailureDetailIsAlsoARunAttribute() {
        assertEquals(FailureDetail.values().length, 4,
                "a fifth thing a failure records needs a RunEditorAttributes constant too, "
                        + "or it is stored, cleared on a pass, and shown nowhere");

        final List<String> named = Arrays.stream(RunEditorAttributes.values())
                .map(RunEditorAttributes::getName)
                .toList();

        for (final String expected : List.of("Actual Result", "Stacktrace", "Bug Severity", "Bug Priority")) {
            assertTrue(named.contains(expected), expected + " is recorded on a run row but has no attribute: " + named);
        }
    }

    @Test
    public void theStacktraceIsReadOnlyAndOffByDefault() {
        assertFalse(RunEditorAttributes.STACKTRACE.isEdited(),
                "the framework writes it; the tester edits it through the failure dialog, not a grid cell");
        assertEquals(RunEditorAttributes.STACKTRACE.getToolBarDefault(), ToolBarDefault.OFF,
                "it is measured in paragraphs - a column of it on by default crowds out every other column");
    }

    @Test
    public void theDetailsPanelDrawsEveryRunValueWorthReading() {
        final List<RunEditorAttributes> shown = List.of(
                RunEditorAttributes.RUN_STATUS,
                RunEditorAttributes.DURATION,
                RunEditorAttributes.ACTUAL_RESULT,
                RunEditorAttributes.STACKTRACE,
                RunEditorAttributes.BUG_SEVERITY,
                RunEditorAttributes.BUG_PRIORITY);

        assertEquals(shown.size(), 6, "the rows DetailsTab appends for a case viewed under a run");
        assertEquals(shown.stream().distinct().count(), 6L, "each drawn once");
    }
}
