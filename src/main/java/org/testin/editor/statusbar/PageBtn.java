package org.testin.editor.statusbar;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractIconButton;
import org.testin.util.Shortcuts;


/**
 * A page arrow, drawn the way every other button in this plugin is drawn.
 * <p>
 * The four arrows were stock {@link JButton}s carrying their look-and-feel
 * border, made compact by hand with a margin and a smaller font. What they
 * wanted is what {@link AbstractIconButton} already owns and the toolbar has had all
 * along: no border at rest, a rounded pill under the pointer, one frozen size so
 * the row does not shift when the pointer arrives. Asking for it here means the
 * next change to that look reaches the status bar too.
 * <p>
 * One class rather than the toolbar's one-per-button: those exist so
 * {@code getToolbarItem} can find them by type, and the status bar holds its
 * four as fields.
 */
public class PageBtn extends AbstractIconButton {

    /**
     * Everything an arrow looks like comes from the step it turns, keystroke
     * included - {@link Shortcuts#EMPTY} prints none (#175, C10).
     */
    public PageBtn(final @NotNull PageStep step) {
        super(step.getTooltip(), step.getIcon(), step.getShortcut());
    }
}
