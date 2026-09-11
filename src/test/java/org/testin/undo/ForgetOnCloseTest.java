package org.testin.undo;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Closing a surface drops what CTRL+Z could take back in it (#66, finding 45).
 * <p>
 * The history used to outlive its editor, on the argument that an editor closed
 * and opened again on the same test set is the same history. What that cost was
 * worse than what it bought: the copies a removal keeps aside were held for as
 * long as the project was open, and a tester who closed a tab had no way to know
 * that CTRL+Z in the next one would take back something they did an hour ago
 * somewhere else.
 * <p>
 * Built without a project: {@code UndoHistories} is a project service and this
 * is about the map inside it, which needs none.
 */
public class ForgetOnCloseTest {

    private static final @NotNull UndoScope ONE = UndoScope.of(Path.of("root", "Test Cases", "Login"));
    private static final @NotNull UndoScope ANOTHER = UndoScope.of(Path.of("root", "Test Cases", "Payment"));

    /**
     * Every operation in the dropped history is told, both stacks, because that
     * is the moment the copies they hold aside stop being wanted.
     */
    @Test
    public void forgettingAHistoryReleasesWhatItsOperationsHeld() {
        final @NotNull UndoHistories histories = new UndoHistories();
        final @NotNull AtomicInteger released = new AtomicInteger();

        histories.push(ONE, held(released));
        histories.push(ONE, held(released));

        // One of the two moved to the redo stack, so the release has to reach
        // both - a tester who pressed CTRL+Z once before closing the tab is the
        // ordinary case, not an unusual one.
        assertTrue(histories.undo(ONE), "the operation undoes cleanly");
        assertEquals(released.get(), 0, "nothing is released while the history is still reachable");

        histories.forget(ONE);

        assertEquals(released.get(), 2, "both stacks are told, so nothing is left holding a copy nobody can reach");
    }

    /**
     * Only that surface. Two editors are two histories, and closing one must
     * not take the other's back with it.
     */
    @Test
    public void forgettingOneSurfaceLeavesTheOthersAlone() {
        final @NotNull UndoHistories histories = new UndoHistories();
        final @NotNull AtomicInteger released = new AtomicInteger();

        histories.push(ONE, held(released));
        histories.push(ANOTHER, held(released));

        histories.forget(ONE);

        assertEquals(released.get(), 1, "the other editor's history is untouched");
        assertTrue(histories.undo(ANOTHER), "and it can still take its own work back");
    }

    /**
     * A surface with nothing recorded, and one closed twice, answer the same
     * way: nothing to release and nothing to fail on.
     */
    @Test
    public void forgettingWhatWasNeverThereIsNotAnError() {
        final @NotNull UndoHistories histories = new UndoHistories();

        histories.forget(ONE);
        histories.forget(ONE);

        assertFalse(histories.undo(ONE), "an untouched surface has nothing to undo, before or after");
    }

    /**
     * An operation that holds something aside until somebody says nobody wants
     * it back, which is what a removal does with the copy it kept.
     */
    private static @NotNull UndoHistories.Operation held(final @NotNull AtomicInteger released) {
        return new UndoHistories.Operation("held", () -> true, () -> true, released::incrementAndGet);
    }
}
