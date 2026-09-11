package org.testin.editor.statusbar;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.*;

/**
 * The four ways to change page, and everything that differs between them.
 * <p>
 * There were nine places saying it. The status bar declared four buttons with
 * their tooltips and icons, computed four deltas in its listener, and decided
 * four times whether each arrow was still available; the keyboard actions
 * declared their own titles, icons, shortcuts and deltas beside them. The
 * arrows and the shortcuts therefore agreed only because nobody had changed
 * either - the same shape {@code stepPage} was written to stop (#175, C10).
 * <p>
 * <b>How far, and whether at all, are one question here.</b> Every step
 * computes its own delta from where the tester is, and a step with nowhere to
 * go computes zero - which is what {@code isAvailable} asks and what
 * {@code stepPage} already ignores. That is why Previous and Next are written
 * as conditionals rather than as a plain -1 and 1: a constant delta cannot say
 * "not from here", and saying it separately is how the fifth and sixth copies
 * of the rule appeared.
 */
@Getter
@AllArgsConstructor
public enum PageStep {

    FIRST(Bundle.message("page.first"), Bundle.message("page.first.description"), AllIcons.Actions.Play_first, Shortcuts.First) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return 1 - currentPage;
        }
    },

    PREVIOUS(Bundle.message("page.previous"), Bundle.message("page.previous.description"), AllIcons.Actions.Play_back, Shortcuts.Previous) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return currentPage > 1 ? -1 : 0;
        }
    },

    NEXT(Bundle.message("page.next"), Bundle.message("page.next.description"), AllIcons.Actions.Play_forward, Shortcuts.Next) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return currentPage < totalPages ? 1 : 0;
        }
    },

    LAST(Bundle.message("page.last"), Bundle.message("page.last.description"), AllIcons.Actions.Play_last, Shortcuts.Last) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return totalPages - currentPage;
        }
    };

    private final @NotNull String tooltip;
    private final @NotNull String description;
    private final @NotNull Icon icon;

    /**
     * The key that turns the page this way. All four have one now, so all four
     * arrows print theirs in their tooltip and all four are reachable without
     * the pointer.
     * <p>
     * {@link Shortcuts#EMPTY} is still what a step with no key would carry - a
     * button built with it prints none - and nothing else in the enum has to
     * change to add one.
     */
    private final @NotNull Shortcuts shortcut;

    /**
     * UC-EDITOR-PANEL-022.
     * <p>
     * How many pages this step moves from where the tester is, and zero when it
     * would move them nowhere.
     */
    public abstract int deltaFrom(final int currentPage, final int totalPages);

    /**
     * UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-102.
     * <p>
     * Whether this step goes anywhere from here - the one rule the four arrows
     * and the two keyboard actions now share.
     */
    public boolean isAvailable(final int currentPage, final int totalPages) {
        return deltaFrom(currentPage, totalPages) != 0;
    }
}
