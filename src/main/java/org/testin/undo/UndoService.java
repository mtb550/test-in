package org.testin.undo;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Undo/redo for everything a tester does that can be taken back - a node moved
 * or renamed in the tree, a test case edited, created, removed or reordered in
 * an editor. Each recorded operation carries its own reverse; the stacks are
 * bounded and a new operation clears the redo history, like every editor undo.
 * <p>
 * One stack, not one per surface. CTRL+Z means the same thing wherever the
 * tester is standing, which is why a node rename and a field edit sit in the
 * same history and why every entry carries a description good enough for the
 * menu to name what the next press will undo (#165).
 */
@Service(Service.Level.PROJECT)
public final class UndoService {

    private static final int LIMIT = 20;

    /**
     * What an empty stack offers: an operation that describes itself as nothing
     * and does nothing in either direction. It is never run - undo and redo ask
     * the stack whether it has anything first - but it lets the two description
     * readers be unconditional, which is the whole reason it exists.
     */
    private static final @NotNull Operation NOTHING = new Operation("", () -> {
    }, () -> {
    });

    private final @NotNull Deque<Operation> undoStack = new ArrayDeque<>();
    private final @NotNull Deque<Operation> redoStack = new ArrayDeque<>();

    /**
     * Records a just-performed operation.
     */
    public void push(final @NotNull Operation operation) {
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
    private static @NotNull Operation next(final @NotNull Deque<Operation> stack) {
        return Objects.requireNonNullElse(stack.peek(), NOTHING);
    }

    public void undo() {
        if (!canUndo()) return;

        final @NotNull Operation operation = undoStack.pop();
        operation.undo().run();
        redoStack.push(operation);
    }

    public void redo() {
        if (!canRedo()) return;

        final @NotNull Operation operation = redoStack.pop();
        operation.redo().run();
        undoStack.push(operation);
    }

    /**
     * One reversible operation; the description is shown in the menu, so it
     * names the gesture rather than the mechanism - "Remove 3 test cases", not
     * "removeTestCase".
     */
    public record Operation(@NotNull String description, @NotNull Runnable undo, @NotNull Runnable redo) {
    }
}
