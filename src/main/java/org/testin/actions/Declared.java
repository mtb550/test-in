package org.testin.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.keymap.KeymapUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;

/**
 * An action the platform owns, by the id {@code plugin.xml} gives it (#119).
 * <p>
 * A declared action is one instance for the whole IDE, built by the platform, so
 * a menu fetches it rather than constructing it - and fetching it is what keeps
 * the menu and the keymap showing the same action. Three menus do this now, so
 * the lookup and what a miss means are here rather than copied into each.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Declared {

    /**
     * The action registered under this id.
     * <p>
     * Throws rather than answering with something inert: every caller names an id
     * from this plugin's own descriptor, so a miss is a wiring mistake and not a
     * state to handle. A menu quietly missing an entry is the failure this avoids.
     */
    public static @NotNull AnAction action(final @NotNull String id) {
        final @Nullable AnAction action = ActionManager.getInstance().getAction(id);

        if (action == null) {
            Logger.error("No action is registered as '" + id + "', so a menu is missing an entry");
            throw new IllegalStateException("No action is registered as '" + id + "'");
        }

        return action;
    }

    /**
     * What key this action answers to <em>right now</em>, and nothing when it
     * answers to none.
     * <p>
     * Asked of the keymap rather than of {@code Shortcuts}, which is the whole
     * point of declaring an action: a tester who rebinds it sees their own key on
     * the card and in the status bar, and a hint can no longer name a key that
     * was ours and is not theirs (#119).
     * <p>
     * An empty id is not a mistake - it is a gesture with no action behind it,
     * like the card's Stop button, and it prints nothing.
     */
    public static @NotNull String shortcutText(final @NotNull String id) {
        if (id.isEmpty()) return "";

        return KeymapUtil.getFirstKeyboardShortcutText(action(id));
    }

    /**
     * The keys this action answers to, for another action that borrows them.
     * <p>
     * The view panel binds the card's two gestures to itself, and it has to bind
     * whatever the tester has bound rather than what we shipped - so it asks the
     * declared action instead of naming a keystroke of its own (#119).
     */
    public static @NotNull ShortcutSet shortcutSet(final @NotNull String id) {
        return id.isEmpty() ? CustomShortcutSet.EMPTY : action(id).getShortcutSet();
    }

    /**
     * Puts a declared action's key on one component rather than in the keymap.
     * <p>
     * For the keys that are a surface's gesture and not a command: ENTER opens
     * what a tree or a list has selected, and a global ENTER would fire in every
     * editor in the IDE. The action is still declared - Find Action offers it and
     * the Keymap lists it with no default - and there is still one action behind
     * the menu entry and the key.
     */
    public static void bindTo(final @NotNull String id, final @NotNull Shortcuts shortcut, final @NotNull JComponent component) {
        action(id).registerCustomShortcutSet(shortcut.getCustomShortcut(), component);
    }
}
