package org.testin.undo;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Done;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Which way through a history a key goes, and everything that differs between
 * the two: the word, the icon, the keystroke, what it says when it lands, and
 * which end of the stack it reads.
 * <p>
 * One enum and one action rather than two actions, because that is all they ever
 * differed by - the pair were the same fifty lines twice, and the second copy
 * was where a scope, a guard or a thread rule got added once.
 */
@Getter
@AllArgsConstructor
public enum UndoDirection {

    UNDO("Undo", AllIcons.Actions.Undo, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), Done.UNDONE) {
        @Override
        public boolean can(final @NotNull UndoService service, final @NotNull UndoScope scope) {
            return service.canUndo(scope);
        }

        @Override
        public void apply(final @NotNull UndoService service, final @NotNull UndoScope scope) {
            service.undo(scope);
        }

        @Override
        public @NotNull String next(final @NotNull UndoService service, final @NotNull UndoScope scope) {
            return service.undoDescription(scope);
        }
    },

    REDO("Redo", AllIcons.Actions.Redo, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), Done.REDONE) {
        @Override
        public boolean can(final @NotNull UndoService service, final @NotNull UndoScope scope) {
            return service.canRedo(scope);
        }

        @Override
        public void apply(final @NotNull UndoService service, final @NotNull UndoScope scope) {
            service.redo(scope);
        }

        @Override
        public @NotNull String next(final @NotNull UndoService service, final @NotNull UndoScope scope) {
            return service.redoDescription(scope);
        }
    };

    private final @NotNull String title;
    private final @NotNull Icon icon;
    private final @NotNull KeyStroke shortcut;
    private final @NotNull Done done;

    /**
     * Whether this surface has anything to go back or forward to.
     */
    public abstract boolean can(final @NotNull UndoService service, final @NotNull UndoScope scope);

    public abstract void apply(final @NotNull UndoService service, final @NotNull UndoScope scope);

    /**
     * What the next press would do, and nothing at all when there is nothing -
     * which is what the menu entry then appends to its own word.
     */
    public abstract @NotNull String next(final @NotNull UndoService service, final @NotNull UndoScope scope);
}
