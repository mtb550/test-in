package org.testin.editor.grid;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The keys a grid answers for itself, declared once.
 * <p>
 * Each of them means something different in a grid than on the list beside it.
 * Copy, cut and paste are the selected cells rather than the selected test case
 * (#66, finding D1). ENTER edits the cell under the tester, or opens the details
 * on the sequence column.
 * <p>
 * <b>Why one file.</b> A keystroke is claimed in two ways that do not see each
 * other: a component's {@code InputMap}, and an {@code AnAction} registered on
 * that component - and the IDE dispatches a registered action <em>before</em> the
 * input map, so an action does not compete with a binding, it replaces it. That
 * is invisible at both ends. ENTER cost four attempts to fix because it was
 * decided in three files and excluded from the menu in a fourth: the exclusion
 * list was maintained beside the bindings instead of being derived from them, so
 * it went stale the moment a key was added.
 * <p>
 * Everything here is therefore read from one declaration. {@link #clipboard}
 * both installs the bindings and supplies their keys to {@link #keptFromMenus},
 * so a key cannot be bound without also being kept.
 *
 * @see GridEnterAction the single handler for ENTER
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridKeys {

    /**
     * What each clipboard key runs, named here so the binding and its handler
     * cannot be spelled differently in two files.
     */
    public static final @NotNull String COPY = "testin.grid.copy";
    public static final @NotNull String CUT = "testin.grid.cut";
    public static final @NotNull String PASTE = "testin.grid.paste";

    /**
     * The clipboard keys and the {@code ActionMap} name each one runs.
     * <p>
     * Through {@link Shortcuts#menuMask}, so this is CMD on a Mac (#25) and so
     * the menu actions carrying the same keys are built from the same modifier -
     * two sources for it is how one platform ended up with two copy gestures.
     */
    public static @NotNull Map<KeyStroke, String> clipboard() {
        final int menuMask = Shortcuts.menuMask();

        return Map.of(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), COPY,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask), CUT,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), PASTE);
    }

    /**
     * ENTER, which {@link GridEnterAction} answers and nothing else may.
     */
    public static @NotNull KeyStroke enter() {
        return Shortcuts.Enter.getKey();
    }

    /**
     * The keys an editor's context menu must not bind onto a grid table.
     * <p>
     * The menu carries actions with these same keys, and binding one to the table
     * does not merely compete with the grid's own handling - it wins, silently.
     * Every action stays on the menu and still acts on the test case there; it is
     * only the key the grid keeps, and only while a grid is on screen.
     */
    public static @NotNull Set<KeyStroke> keptFromMenus() {
        final @NotNull Set<KeyStroke> kept = new HashSet<>(clipboard().keySet());
        kept.add(enter());

        return Set.copyOf(kept);
    }
}
