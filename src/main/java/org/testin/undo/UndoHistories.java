/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.undo;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
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
public final class UndoHistories {

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
     * UC-INTERNAL-005, Rule-INTERNAL-043.
     * <p>
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

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-063.
     * <p>
     * Answers whether everything came back. An operation that could not finish
     * has already said why, so the caller says nothing rather than confirming
     * over it (#275).
     */
    public boolean undo(final @NotNull UndoScope scope) {
        final @NotNull History history = of(scope);
        if (history.undoStack.isEmpty()) return false;

        final @NotNull Operation operation = history.undoStack.pop();

        // Nothing came back whole, so nothing moves. The operation used to go
        // onto the redo stack regardless: the press was spent, the change the
        // tester asked for was now behind CTRL+Y, and the next CTRL+Z took back
        // the operation before it - one they had said nothing about (#66,
        // finding 101).
        if (!operation.undo().getAsBoolean()) {
            history.undoStack.push(operation);
            return false;
        }

        history.redoStack.push(operation);
        return true;
    }

    /** The same, the other way, and it is spent under the same condition. */
    public boolean redo(final @NotNull UndoScope scope) {
        final @NotNull History history = of(scope);
        if (history.redoStack.isEmpty()) return false;

        final @NotNull Operation operation = history.redoStack.pop();

        if (!operation.redo().getAsBoolean()) {
            history.redoStack.push(operation);
            return false;
        }

        history.undoStack.push(operation);
        return true;
    }

    /**
     * UC-INTERNAL-005, Rule-INTERNAL-045.
     * <p>
     * The surface is gone, so what it could take back goes with it.
     * <p>
     * <b>Every operation is told.</b> Some of them are holding something until
     * they are sure nobody wants it back - a removed test set is kept aside so
     * it can be restored - and this is that moment for all of them at once.
     * Dropping the map entry without saying so would leave those copies on disk
     * with nothing left that knows they are there.
     * <p>
     * <b>Why the history does not outlive its editor.</b> It used to, and the
     * argument was that an editor closed and opened again on the same test set
     * is the same history. What that costs is worse than what it buys: the
     * copies a removal keeps aside are held for as long as the project is open,
     * and a tester who closed a tab has no way to know that pressing CTRL+Z in
     * the tab they open next will take back something they did an hour ago in a
     * different one. Closing a surface is the tester saying they are done with
     * it (#66, finding 45).
     */
    public void forget(final @NotNull UndoScope scope) {
        final @Nullable History history = histories.remove(scope);
        if (history == null) return;

        history.undoStack.forEach(operation -> operation.forget().run());
        history.redoStack.forEach(operation -> operation.forget().run());
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
    public record Operation(@NotNull String description, @NotNull BooleanSupplier undo, @NotNull BooleanSupplier redo, @NotNull Runnable forget) {

        /**
         * UC-INTERNAL-004, Rule-INTERNAL-063.
         * <p>
         * For work that either happens or throws, which is four of the five.
         * Their reversals answer true because there is no half of them to fail.
         * <p>
         * The type asks anyway, so the one that can come back short - a removal
         * whose copy is no longer on disk - has somewhere to say so, and cannot
         * be confirmed as done over a node still missing (#275).
         */
        public Operation(final @NotNull String description, final @NotNull Runnable undo, final @NotNull Runnable redo) {
            this(description, always(undo), always(redo), () -> {
            });
        }

        private static @NotNull BooleanSupplier always(final @NotNull Runnable work) {
            return () -> {
                work.run();
                return true;
            };
        }
    }
}
