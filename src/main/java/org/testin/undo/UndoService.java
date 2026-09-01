package org.testin.undo;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Undo/redo for everything a tester does that can be taken back - a node moved,
 * renamed or removed in the tree, a test case edited, created, removed or
 * reordered in an editor. Each recorded operation carries its own reverse; the
 * stacks are bounded and a new operation clears the redo history, like every
 * editor undo.
 * <p>
 * One history per surface, not one for the plugin. The tree has its own and so
 * does every test editor, keyed by {@link UndoScope}, so a tester who removed
 * two cases in one editor and two in another gets back the two they removed
 * here when they press CTRL+Z here. That is what the key means to the person
 * pressing it, and what the IDE's own editors do (#165).
 * <p>
 * An operation dropped off the end of a stack is told so. Some of them are
 * holding something until they are sure nobody wants it back - a removed test
 * set is kept aside so it can be restored - and forgetting it is the moment
 * that changes.
 */
@Service(Service.Level.PROJECT)
public final class UndoService {

    private static final int LIMIT = 20;

    /**
     * What an empty stack offers: an operation that describes itself as nothing
     * and does nothing in any direction. It is never run - undo and redo ask
     * the stack whether it has anything first - but it lets the two description
     * readers be unconditional, which is the whole reason it exists.
     */
    private static final @NotNull Operation NOTHING = new Operation("", () -> {
    }, () -> {
    });

    private final @NotNull Map<UndoScope, History> histories = new HashMap<>();

    /**
     * Records a just-performed operation against the surface it happened on.
     */
    public void push(final @NotNull UndoScope scope, final @NotNull Operation operation) {
        final @NotNull History history = of(scope);

        history.undoStack.push(operation);
        while (history.undoStack.size() > LIMIT) history.undoStack.removeLast().forget().run();

        // A new operation makes every redo unreachable, so whatever they were
        // holding is now holding nothing back.
        history.redoStack.forEach(dropped -> dropped.forget().run());
        history.redoStack.clear();
    }

    public boolean canUndo(final @NotNull UndoScope scope) {
        return !of(scope).undoStack.isEmpty();
    }

    public boolean canRedo(final @NotNull UndoScope scope) {
        return !of(scope).redoStack.isEmpty();
    }

    /**
     * What the next undo would undo, and nothing at all when the stack is
     * empty - which is what the menu entry then appends to its own word.
     */
    public @NotNull String undoDescription(final @NotNull UndoScope scope) {
        return next(of(scope).undoStack).description();
    }

    /**
     * What the next redo would redo, empty when there is nothing to redo.
     */
    public @NotNull String redoDescription(final @NotNull UndoScope scope) {
        return next(of(scope).redoStack).description();
    }

    public void undo(final @NotNull UndoScope scope) {
        final @NotNull History history = of(scope);
        if (history.undoStack.isEmpty()) return;

        final @NotNull Operation operation = history.undoStack.pop();
        operation.undo().run();
        history.redoStack.push(operation);
    }

    public void redo(final @NotNull UndoScope scope) {
        final @NotNull History history = of(scope);
        if (history.redoStack.isEmpty()) return;

        final @NotNull Operation operation = history.redoStack.pop();
        operation.redo().run();
        history.undoStack.push(operation);
    }

    private @NotNull History of(final @NotNull UndoScope scope) {
        return histories.computeIfAbsent(scope, key -> new History());
    }

    /**
     * The operation at the top of a stack, or the one that stands for none.
     */
    private static @NotNull Operation next(final @NotNull Deque<Operation> stack) {
        return Objects.requireNonNullElse(stack.peek(), NOTHING);
    }

    private static final class History {
        private final @NotNull Deque<Operation> undoStack = new ArrayDeque<>();
        private final @NotNull Deque<Operation> redoStack = new ArrayDeque<>();
    }

    /**
     * One reversible operation; the description is shown in the menu, so it
     * names the gesture rather than the mechanism - "Remove 3 test cases", not
     * "removeTestCase".
     * <p>
     * {@code forget} runs when the operation falls out of reach, which is what
     * an operation holding something aside is waiting to hear. Nothing for the
     * ones that hold nothing, which is most of them.
     */
    public record Operation(@NotNull String description, @NotNull Runnable undo, @NotNull Runnable redo, @NotNull Runnable forget) {

        public Operation(final @NotNull String description, final @NotNull Runnable undo, final @NotNull Runnable redo) {
            this(description, undo, redo, () -> {
            });
        }
    }
}
