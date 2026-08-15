package org.testin.explorer.tree;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo/redo for reversible tree operations (move, rename). Each recorded
 * operation carries its own reverse; the stacks are bounded and a new
 * operation clears the redo history, like every editor undo.
 */
@Service(Service.Level.PROJECT)
public final class TreeUndoService {

    private static final int LIMIT = 20;
    private final @NotNull Deque<TreeOperation> undoStack = new ArrayDeque<>();
    private final @NotNull Deque<TreeOperation> redoStack = new ArrayDeque<>();

    /**
     * Records a just-performed operation.
     */
    public void push(final @NotNull TreeOperation operation) {
        undoStack.push(operation);
        while (undoStack.size() > LIMIT) undoStack.removeLast();
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public @Nullable String undoDescription() {
        final TreeOperation operation = undoStack.peek();
        return operation == null ? null : operation.description();
    }

    public @Nullable String redoDescription() {
        final TreeOperation operation = redoStack.peek();
        return operation == null ? null : operation.description();
    }

    public void undo() {
        final TreeOperation operation = undoStack.poll();
        if (operation == null) return;

        operation.undo().run();
        redoStack.push(operation);
    }

    public void redo() {
        final TreeOperation operation = redoStack.poll();
        if (operation == null) return;

        operation.redo().run();
        undoStack.push(operation);
    }

    /**
     * One reversible tree operation; the description is shown in the menu.
     */
    public record TreeOperation(@NotNull String description, @NotNull Runnable undo, @NotNull Runnable redo) {
    }
}
