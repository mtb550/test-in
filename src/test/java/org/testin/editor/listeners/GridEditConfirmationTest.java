package org.testin.editor.listeners;

import org.testng.annotations.Test;

import javax.swing.event.TableModelListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Every grid edit says it happened, and says it in one place (#66, finding 29).
 * <p>
 * Typing into a grid cell writes a test case or a run to disk. Every other way
 * of changing the same data confirms itself with one soft notification in the
 * past tense - the update dialog says "Updated" - and the grids said nothing at
 * all, so the one surface where a tester changes data fastest was the one
 * surface that never told them it had worked.
 * <p>
 * Written as a scan of the sources for the same reason as
 * {@code LightServiceContractTest}: what has to hold is a rule about the shape
 * of the code, and the alternative - a running IDE with a project, a grid and a
 * balloon to look for - tests the platform rather than the rule.
 * <p>
 * The rule is that no listener answers this for itself. A second copy of the
 * confirmation is how the two grids end up saying different words for one act,
 * and a listener written without one is how a grid goes quiet again.
 */
public class GridEditConfirmationTest {

    private static final Path LISTENERS = Paths.get("src", "main", "java", "org", "testin", "editor", "listeners");

    private static final String PARENT = "AbstractGridEditListener";

    /**
     * Every class in the listeners package that takes grid edits, found two ways
     * so that neither can hide from this.
     * <p>
     * One that goes through the parent names it; one written without the parent -
     * which is the thing being guarded against - has to say
     * {@code TableModelListener} instead, because that is what the table hands
     * its edits to.
     */
    private static List<Path> gridEditListeners() {
        try (Stream<Path> files = Files.list(LISTENERS)) {
            final List<Path> found = new ArrayList<>();

            for (final Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().startsWith(PARENT)) continue;

                final String source = Files.readString(file);
                if (source.contains("TableModelListener") || source.contains(PARENT)) found.add(file);
            }

            return found;
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + LISTENERS.toAbsolutePath(), ex);
        }
    }

    private static String sourceOf(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file, ex);
        }
    }

    @Test
    public void bothGridsHaveAnEditListenerToCheck() {
        assertEquals(gridEditListeners().size(), 2,
                "the test grid and the run grid, and this test needs updating if a third arrives");
    }

    @Test
    public void everyGridEditListenerGoesThroughTheSharedOne() {
        for (final Path listener : gridEditListeners()) {
            assertTrue(sourceOf(listener).contains("extends " + PARENT),
                    listener.getFileName() + " writes test data from a grid cell without the guards or the "
                            + "confirmation that " + PARENT + " owns");
        }
    }

    @Test
    public void noListenerConfirmsForItself() {
        for (final Path listener : gridEditListeners()) {
            if (!sourceOf(listener).contains("softShow")) continue;

            fail(listener.getFileName() + " says the edit landed in its own words. Two copies of that is how "
                    + "the two grids end up telling a tester different things about the same act - it belongs "
                    + "to " + PARENT + ", which both of them already run through.");
        }
    }

    /**
     * Both say it by naming the same outcome rather than by spelling the same
     * word, which is what makes them unable to disagree.
     * <p>
     * This used to look for the literal {@code softShow(p, "Updated")} in both -
     * true of the code at the time, and satisfied just as well by two files that
     * happened to hold matching strings. {@code Done} now owns the past-tense
     * vocabulary the whole plugin confirms in, so the thing worth asserting is
     * that neither of these picks its own word.
     */
    @Test
    public void theSharedOneSaysWhatTheUpdateDialogSays() {
        final String parent = sourceOf(LISTENERS.resolve(PARENT + ".java"));
        final String dialog = sourceOf(Paths.get("src", "main", "java", "org", "testin", "testcase",
                "UpdateTestCaseAction.java"));

        assertTrue(parent.contains("Done.UPDATED"),
                PARENT + " confirms a grid edit in words of its own rather than naming the outcome");

        assertTrue(dialog.contains("Done.UPDATED"),
                "the update dialog names a different outcome from the grid edit, for the same act on the "
                        + "same field - a tester should not have to learn that they are called different things");
    }
}
