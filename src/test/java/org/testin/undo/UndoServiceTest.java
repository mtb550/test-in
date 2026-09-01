package org.testin.undo;

import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

/**
 * The undo/redo contract: undo runs the reverse, redo runs the forward again, a
 * new operation clears the redo history, the stack is bounded - and each
 * surface keeps its own history, so a test case removed in one editor is not
 * what CTRL+Z reaches for in another (#165).
 */
public class UndoServiceTest {

    private static final UndoScope TREE = UndoScope.TREE;
    private static final UndoScope EDITOR = UndoScope.of(Path.of("root", "project", "Test Cases", "login"));

    private static UndoService.Operation counting(final String description, final AtomicInteger undone, final AtomicInteger redone) {
        return new UndoService.Operation(description, undone::incrementAndGet, redone::incrementAndGet);
    }

    @Test
    public void undoRunsTheReverseAndEnablesRedo() {
        final UndoService service = new UndoService();
        final AtomicInteger undone = new AtomicInteger();
        final AtomicInteger redone = new AtomicInteger();

        service.push(TREE, counting("Move 'pkg'", undone, redone));
        assertTrue(service.canUndo(TREE));
        assertEquals(service.undoDescription(TREE), "Move 'pkg'");
        assertFalse(service.canRedo(TREE));

        service.undo(TREE);

        assertEquals(undone.get(), 1);
        assertEquals(redone.get(), 0);
        assertFalse(service.canUndo(TREE));
        assertTrue(service.canRedo(TREE));
        assertEquals(service.redoDescription(TREE), "Move 'pkg'");
    }

    @Test
    public void redoRunsTheForwardAgainAndRestoresUndo() {
        final UndoService service = new UndoService();
        final AtomicInteger undone = new AtomicInteger();
        final AtomicInteger redone = new AtomicInteger();

        service.push(TREE, counting("Rename 'x'", undone, redone));
        service.undo(TREE);
        service.redo(TREE);

        assertEquals(undone.get(), 1);
        assertEquals(redone.get(), 1);
        assertTrue(service.canUndo(TREE));
        assertFalse(service.canRedo(TREE));
    }

    @Test
    public void newOperationClearsTheRedoHistory() {
        final UndoService service = new UndoService();
        final AtomicInteger ignored = new AtomicInteger();

        service.push(TREE, counting("first", ignored, ignored));
        service.undo(TREE);
        assertTrue(service.canRedo(TREE));

        service.push(TREE, counting("second", ignored, ignored));

        assertFalse(service.canRedo(TREE), "a new operation must clear the redo history");
        assertEquals(service.undoDescription(TREE), "second");
    }

    @Test
    public void historyIsBounded() {
        final UndoService service = new UndoService();
        final AtomicInteger undone = new AtomicInteger();

        for (int i = 0; i < 30; i++) {
            service.push(TREE, counting("op " + i, undone, new AtomicInteger()));
        }
        while (service.canUndo(TREE)) {
            service.undo(TREE);
        }

        assertEquals(undone.get(), 20, "the undo stack must be capped");
    }

    /**
     * Two surfaces, two histories. This is the whole reason the service is keyed:
     * a tester who removed cases in one editor and in another presses the same
     * key in both and gets back what they removed there.
     */
    @Test
    public void eachSurfaceKeepsItsOwnHistory() {
        final UndoService service = new UndoService();
        final AtomicInteger tree = new AtomicInteger();
        final AtomicInteger editor = new AtomicInteger();

        service.push(TREE, counting("Remove 'login set'", tree, new AtomicInteger()));
        service.push(EDITOR, counting("Remove 2 test cases", editor, new AtomicInteger()));

        assertEquals(service.undoDescription(TREE), "Remove 'login set'");
        assertEquals(service.undoDescription(EDITOR), "Remove 2 test cases");

        service.undo(EDITOR);

        assertEquals(editor.get(), 1);
        assertEquals(tree.get(), 0, "undoing in an editor must not reach into the tree's history");
        assertTrue(service.canUndo(TREE), "the tree still has its own operation to undo");
    }

    /**
     * An operation that falls out of reach is told so, because some of them are
     * holding a copy of a removed test set until they are sure nobody wants it.
     */
    @Test
    public void anOperationPushedOffTheEndIsForgotten() {
        final UndoService service = new UndoService();
        final AtomicInteger forgotten = new AtomicInteger();
        final AtomicInteger ignored = new AtomicInteger();

        for (int i = 0; i < 21; i++) {
            service.push(TREE, new UndoService.Operation("op " + i, ignored::incrementAndGet, ignored::incrementAndGet, forgotten::incrementAndGet));
        }

        assertEquals(forgotten.get(), 1, "the operation dropped off the end must release what it held");
    }

    @Test
    public void undoAndRedoOnEmptyStacksDoNothing() {
        final UndoService service = new UndoService();

        service.undo(TREE);
        service.redo(TREE);

        assertFalse(service.canUndo(TREE));
        assertFalse(service.canRedo(TREE));
    }
}
