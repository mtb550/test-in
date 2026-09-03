package org.testin.editor.statusbar;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
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

    FIRST("First page", "Navigate to the first page", AllIcons.Actions.Play_first, Shortcuts.EMPTY) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return 1 - currentPage;
        }
    },

    PREVIOUS("Previous page", "Navigate to the previous page", AllIcons.Actions.Play_back, Shortcuts.PreviousTestCase) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return currentPage > 1 ? -1 : 0;
        }
    },

    NEXT("Next page", "Navigate to the next page", AllIcons.Actions.Play_forward, Shortcuts.NextTestCase) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return currentPage < totalPages ? 1 : 0;
        }
    },

    LAST("Last page", "Navigate to the last page", AllIcons.Actions.Play_last, Shortcuts.EMPTY) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return totalPages - currentPage;
        }
    };

    private final @NotNull String tooltip;
    private final @NotNull String description;
    private final @NotNull Icon icon;

    /**
     * The key that turns the page this way, {@link Shortcuts#EMPTY} for the two
     * that have none. A button built with EMPTY prints no key in its tooltip.
     */
    private final @NotNull Shortcuts shortcut;

    /**
     * How many pages this step moves from where the tester is, and zero when it
     * would move them nowhere.
     */
    public abstract int deltaFrom(final int currentPage, final int totalPages);

    /**
     * Whether this step goes anywhere from here - the one rule the four arrows
     * and the two keyboard actions now share.
     */
    public boolean isAvailable(final int currentPage, final int totalPages) {
        return deltaFrom(currentPage, totalPages) != 0;
    }
}
