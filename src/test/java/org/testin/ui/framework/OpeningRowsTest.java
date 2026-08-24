package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * The picker never runs a query on the thread that paints it (#29).
 * <p>
 * Typing was moved off that thread and opening was left on it: the constructor
 * asked for the rows of an empty query, which for the search is the broadest
 * query there is - every test set and every test run in the project, copied,
 * filtered and sorted. It ran at the one moment the tester is watching, the
 * frame after they pressed the shortcut.
 * <p>
 * A fixed set of choices is not affected and must not become so. Its rows are
 * already in hand, they go on screen in the constructor, and Enter works on the
 * first frame - a create dialog that filled in a moment later could be submitted
 * before it had anything to submit.
 * <p>
 * Read from the source rather than driven through Swing: the defect is which
 * thread a call sits on, and both threads compile.
 */
public class OpeningRowsTest {

    private static final @NotNull Path FRAMEWORK =
            Paths.get("src", "main", "java", "org", "testin", "ui", "framework");

    @Test
    public void theQueryIsOnlyEverRunOffThePaintingThread() {
        final @NotNull String source = read("TextFieldWithSelections.java");
        final int asked = countOf(source, "rows.forQuery(");

        assertEquals(asked, 1,
                "the query should be asked in exactly one place, inside requestRows; found " + asked
                        + " - a second one is a pass over the whole project on the painting thread");

        assertTrue(source.indexOf("executeOnPooledThread") < source.indexOf("rows.forQuery("),
                "rows.forQuery must sit inside executeOnPooledThread: run before it, the search freezes "
                        + "the dialog it is filling in");
    }

    /**
     * The opening rows arrive once the dialog is on screen, and once only.
     * Without the guard a hierarchy event repeated for one dialog would run the
     * whole search again for nothing.
     */
    @Test
    public void theOpeningRowsAreAskedForOnceTheDialogIsShown() {
        final @NotNull String source = read("TextFieldWithSelections.java");

        assertTrue(source.contains("HierarchyEvent.SHOWING_CHANGED"),
                "the opening rows should be asked for when the dialog is shown, not in the constructor");
        assertTrue(source.contains("askedOnce"),
                "asking must happen once: a showing event can repeat for one dialog, and the search is "
                        + "too expensive to run again for it");
    }

    /**
     * The answer comes back under the dialog's own modality. Posted without it,
     * it waits behind the open dialog and lands after it closes - which for the
     * list the dialog exists to show is never.
     */
    @Test
    public void theAnswerIsPostedUnderTheDialogsModality() {
        final @NotNull String source = read("TextFieldWithSelections.java");

        assertTrue(source.contains("ModalityState.stateForComponent(panel)"),
                "the rows must be posted back under the dialog's modality, or they never arrive while it is open");
    }

    /**
     * A fixed set of choices is still on screen from the constructor, so a
     * create dialog can be submitted on the frame it opens.
     */
    @Test
    public void aFixedSetOfChoicesIsShownBeforeAnythingIsAsked() {
        final @NotNull String picker = read("TextFieldWithSelections.java");
        final @NotNull String builder = read("ComponentDialogBase.java");

        assertTrue(picker.contains("show(shownBeforeAsking)"),
                "rows known at construction must go on screen there, or Enter has nothing to take");
        assertTrue(builder.contains("placeholder, fixed, query -> fixed"),
                "the fixed-choice picker must be handed its rows as well as its Rows, so it shows them at once");
        assertTrue(builder.contains("placeholder, List.of(), rows.orElseThrow()"),
                "the searching picker opens with nothing and fills in off the painting thread");
    }


    /**
     * How many times a literal appears. Counted rather than split on, so the
     * thing being looked for is written exactly as it is in the file instead of
     * as a regular expression with everything escaped twice.
     */
    private static int countOf(final @NotNull String source, final @NotNull String literal) {
        int found = 0;
        int at = source.indexOf(literal);

        while (at >= 0) {
            found++;
            at = source.indexOf(literal, at + literal.length());
        }

        return found;
    }

    private static @NotNull String read(final @NotNull String fileName) {
        final @NotNull Path file = FRAMEWORK.resolve(fileName);

        try {
            return Files.readString(file);
        } catch (final IOException notThere) {
            fail("Could not read " + file.toAbsolutePath() + ": " + notThere.getMessage());
            return "";
        }
    }
}
