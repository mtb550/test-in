package org.testin.project.tree;

import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

/**
 * The tree undo/redo contract: undo runs the reverse, redo runs the forward
 * again, a new operation clears the redo history, and the stack is bounded.
 */
public class TreeUndoServiceTest {

    private static TreeUndoService.TreeOperation counting(final String description,
                                                          final AtomicInteger undone, final AtomicInteger redone) {
        return new TreeUndoService.TreeOperation(description, undone::incrementAndGet, redone::incrementAndGet);
    }

    @Test
    public void undoRunsTheReverseAndEnablesRedo() {
        final TreeUndoService service = new TreeUndoService();
        final AtomicInteger undone = new AtomicInteger();
        final AtomicInteger redone = new AtomicInteger();

        service.push(counting("Move 'pkg'", undone, redone));
        assertTrue(service.canUndo());
        assertEquals(service.undoDescription(), "Move 'pkg'");
        assertFalse(service.canRedo());

        service.undo();

        assertEquals(undone.get(), 1);
        assertEquals(redone.get(), 0);
        assertFalse(service.canUndo());
        assertTrue(service.canRedo());
        assertEquals(service.redoDescription(), "Move 'pkg'");
    }

    @Test
    public void redoRunsTheForwardAgainAndRestoresUndo() {
        final TreeUndoService service = new TreeUndoService();
        final AtomicInteger undone = new AtomicInteger();
        final AtomicInteger redone = new AtomicInteger();

        service.push(counting("Rename 'x'", undone, redone));
        service.undo();
        service.redo();

        assertEquals(undone.get(), 1);
        assertEquals(redone.get(), 1);
        assertTrue(service.canUndo());
        assertFalse(service.canRedo());
    }

    @Test
    public void newOperationClearsTheRedoHistory() {
        final TreeUndoService service = new TreeUndoService();
        final AtomicInteger ignored = new AtomicInteger();

        service.push(counting("first", ignored, ignored));
        service.undo();
        assertTrue(service.canRedo());

        service.push(counting("second", ignored, ignored));

        assertFalse(service.canRedo(), "a new operation must clear the redo history");
        assertEquals(service.undoDescription(), "second");
    }

    @Test
    public void historyIsBounded() {
        final TreeUndoService service = new TreeUndoService();
        final AtomicInteger undone = new AtomicInteger();

        for (int i = 0; i < 30; i++) {
            service.push(counting("op " + i, undone, new AtomicInteger()));
        }
        while (service.canUndo()) {
            service.undo();
        }

        assertEquals(undone.get(), 20, "the undo stack must be capped");
    }

    @Test
    public void undoAndRedoOnEmptyStacksDoNothing() {
        final TreeUndoService service = new TreeUndoService();

        service.undo();
        service.redo();

        assertFalse(service.canUndo());
        assertFalse(service.canRedo());
    }
}
