package org.testin.explorer.tree;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Undo/redo for reversible tree operations (move, rename). Each recorded
 * operation carries its own reverse; the stacks are bounded and a new
 * operation clears the redo history, like every editor undo.
 */
@Service(Service.Level.PROJECT)
public final class TreeUndoService {

    private static final int LIMIT = 20;

    /**
     * What an empty stack offers: an operation that describes itself as nothing
     * and does nothing in either direction. It is never run - undo and redo ask
     * the stack whether it has anything first - but it lets the two description
     * readers be unconditional, which is the whole reason it exists.
     */
    private static final @NotNull TreeOperation NOTHING = new TreeOperation("", () -> {
    }, () -> {
    });

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

    /**
     * What the next undo would undo, and nothing at all when the stack is
     * empty - which is what the menu entry then appends to its own word.
     */
    public @NotNull String undoDescription() {
        return next(undoStack).description();
    }

    /**
     * What the next redo would redo, empty when there is nothing to redo.
     */
    public @NotNull String redoDescription() {
        return next(redoStack).description();
    }

    /**
     * The operation at the top of a stack, or the one that stands for none.
     */
    private static @NotNull TreeOperation next(final @NotNull Deque<TreeOperation> stack) {
        return Objects.requireNonNullElse(stack.peek(), NOTHING);
    }

    public void undo() {
        if (!canUndo()) return;

        final @NotNull TreeOperation operation = undoStack.pop();
        operation.undo().run();
        redoStack.push(operation);
    }

    public void redo() {
        if (!canRedo()) return;

        final @NotNull TreeOperation operation = redoStack.pop();
        operation.redo().run();
        undoStack.push(operation);
    }

    /**
     * One reversible tree operation; the description is shown in the menu.
     */
    public record TreeOperation(@NotNull String description, @NotNull Runnable undo, @NotNull Runnable redo) {
    }
}
