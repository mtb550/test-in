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

package org.testin.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.keymap.KeymapUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.util.Map;
import java.util.Optional;

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
     * UC-INTERNAL-001, Rule-INTERNAL-066, Rule-INTERNAL-068.
     * <p>
     * <b>The one file that says which key a surface gives a declared action.</b>
     * <p>
     * Eight actions are declared with no default key, because their keystroke is
     * a surface gesture rather than a command: ENTER opens what a tree or a list
     * has selected, DELETE is the tree key and the card list key and the grid
     * key, and the clipboard three belong to whichever of the three holds the
     * caret. A keymap entry for any of them would answer everywhere in the IDE,
     * which is why {@code plugin.xml} gives them none.
     * <p>
     * Written here rather than at the call sites because two things need the
     * same answer and used to get it two ways. {@link #bindTo} binds the key;
     * {@link #shortcutText} prints it on a tooltip and a status bar. The printer
     * used to read it back off the action itself, which worked only because the
     * binder had written it there - see {@link #bindTo} for why that stopped.
     * <p>
     * A surface-bound key is not rebindable, and the Keymap says so by listing
     * the action with no default. That is the price of the key belonging to the
     * surface; the twelve actions that are commands carry their key in
     * {@code plugin.xml}, where a tester may move it.
     */
    private static final @NotNull Map<String, Shortcuts> SURFACE_KEYS = Map.of(
            "Testin.Open", Shortcuts.Enter,
            "Testin.ViewDetails", Shortcuts.Enter,
            "Testin.CopyNode", Shortcuts.CopyItem,
            "Testin.CutNode", Shortcuts.CutItem,
            "Testin.PasteNode", Shortcuts.PasteItem,
            "Testin.RemoveNode", Shortcuts.DeletePackage,
            "Testin.CopyTestCase", Shortcuts.CopyItem,
            "Testin.RemoveTestCase", Shortcuts.DeletePackage);

    /**
     * What key this action answers to <em>right now</em>, and nothing when it
     * answers to none.
     * <p>
     * The keymap for a command, so a tester who rebinds it sees their own key on
     * the card and in the status bar and a hint can no longer name a key that was
     * ours and is not theirs (#119). {@link #SURFACE_KEYS} for a gesture, which
     * has no keymap entry to be read from.
     * <p>
     * An empty id is not a mistake - it is a gesture with no action behind it,
     * like the card Stop button, and it prints nothing.
     */
    public static @NotNull String shortcutText(final @NotNull String id) {
        if (id.isEmpty()) return "";

        return Optional.ofNullable(SURFACE_KEYS.get(id))
                .map(Shortcuts::getShortcutText)
                .orElseGet(() -> KeymapUtil.getFirstKeyboardShortcutText(action(id)));
    }

    /**
     * The keys this action answers to, for another action that borrows them.
     * <p>
     * The view panel binds the two card gestures to itself, and it has to bind
     * whatever the tester has bound rather than what we shipped - so it asks the
     * declared action instead of naming a keystroke of its own (#119).
     */
    public static @NotNull ShortcutSet shortcutSet(final @NotNull String id) {
        if (id.isEmpty()) return CustomShortcutSet.EMPTY;

        return Optional.ofNullable(SURFACE_KEYS.get(id))
                .<ShortcutSet>map(Shortcuts::getCustomShortcut)
                .orElseGet(() -> action(id).getShortcutSet());
    }

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-066, Rule-INTERNAL-068.
     * <p>
     * The action as a menu should show it: carrying the key its surface gives
     * it, for the eight that have one, and the registered action itself for
     * every other.
     * <p>
     * <b>A menu reads the key off the action.</b> So does
     * {@code AbstractEditorContextMenu.claimedByTheGrid}, which asks what a menu
     * entry answers to before deciding whether the grid keeps that key for its
     * own cells. Both used to get an answer because {@link #bindTo} wrote the
     * key onto the registered action - the global mutation the platform was
     * logging a PluginException about. Removing that mutation took the answer
     * away from both: the menu stopped printing CTRL+C beside Copy, and the grid
     * lost CTRL+C, CTRL+X, CTRL+V and DELETE to the menu entries that no longer
     * looked claimed.
     * <p>
     * So the key is put on a copy instead, which is the same thing {@link #bindTo}
     * does and for the same reason: {@code ActionUtil.wrap} is not registered, so
     * its shortcut set is ours to set and the instance the Keymap page shows is
     * left alone. One declaration - {@link #SURFACE_KEYS} - now answers the
     * binding, the printing and the grid's question, and they cannot come apart.
     */
    public static @NotNull AnAction forMenu(final @NotNull String id) {
        final @NotNull AnAction action = action(id);

        return Optional.ofNullable(SURFACE_KEYS.get(id))
                .map(key -> carrying(action, key))
                .orElse(action);
    }

    /**
     * A copy of the action that answers to that key. Not registered, so nothing
     * global changes.
     */
    private static @NotNull AnAction carrying(final @NotNull AnAction action, final @NotNull Shortcuts key) {
        final @NotNull AnAction copy = ActionUtil.wrap(action);
        copy.registerCustomShortcutSet(key.getCustomShortcut(), null);

        return copy;
    }

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-066, Rule-INTERNAL-068.
     * <p>
     * Puts a declared action key on one component rather than in the keymap,
     * through a wrapper rather than on the action itself.
     * <p>
     * <b>The wrapper is the point.</b> A declared action is one instance for the
     * whole IDE, and {@code registerCustomShortcutSet} sets that instance own
     * shortcut set - so eleven call sites were each overwriting a shared object,
     * and the platform logged a {@code PluginException} naming this line every
     * time an editor or the tree was built:
     * <pre>
     * ShortcutSet of global AnActions should not be changed outside of
     * KeymapManager. Action: View Test Case Details [Plugin: org.testin]
     * </pre>
     * {@code ActionUtil.wrap} is the remedy the platform message itself offers: a
     * delegating copy that is not registered, so its shortcut set is ours to set
     * and the instance the Keymap page shows is left alone. There is still one
     * action behind the menu entry and the key - the wrapper does nothing but
     * pass {@code update} and {@code actionPerformed} through.
     * <p>
     * The key comes from {@link #SURFACE_KEYS} rather than from the caller, so
     * the surface that binds it and the tooltip that prints it cannot name two
     * different keys.
     */
    public static void bindTo(final @NotNull String id, final @NotNull JComponent component) {
        final @NotNull Shortcuts key = Optional.ofNullable(SURFACE_KEYS.get(id))
                .orElseThrow(() -> new IllegalStateException("No surface key is declared for " + id));

        ActionUtil.wrap(action(id)).registerCustomShortcutSet(key.getCustomShortcut(), component);
    }
}
