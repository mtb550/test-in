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

package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

/**
 * One status bar entry of a framework dialog: the keystroke, the name shown to
 * the tester, and the action the key runs. The same declaration renders the
 * hint and binds the key, so a shortcut cannot be shown without working or
 * work without being shown (issue #11). Keys a component already binds itself
 * (e.g. list navigation) are declared as display-only {@link #hint}s.
 */
public record StatusBarShortcut(@NotNull Shortcuts shortcut, @NotNull String displayText, @NotNull String name, @NotNull Runnable action) implements StatusBarItem {

    /**
     * What a hint runs: nothing. A hint is bound to no key, so the key never
     * arrives and this never runs - it exists so the record has no null in it.
     */
    private static final @NotNull Runnable NOTHING = () -> {
    };

    // UC-INTERNAL-007, Rule-INTERNAL-054
    public static @NotNull StatusBarShortcut build(final @NotNull Shortcuts shortcut, final @NotNull String name, final @NotNull Runnable action) {
        return new StatusBarShortcut(shortcut, shortcut.getShortcutText(), name, action);
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-059.
     * <p>
     * Escape, called Cancel, closing without saving.
     * <p>
     * Twenty-one dialogs declared this identically - the same key, the same
     * word, the same method - and the word was the most duplicated string in
     * the plugin. It is a factory rather than a shared constant because the
     * three parts belong together: a dialog that took the word and bound a
     * different key, or bound Escape to something other than closing, would be
     * a dialog that lies to the tester about what Escape does.
     * <p>
     * A dialog that needs Escape to do something else still calls
     * {@link #build} and says so.
     */
    public static @NotNull StatusBarShortcut cancel(final @NotNull Runnable action) {
        return build(Shortcuts.Escape, Bundle.message("shortcut.cancel"), action);
    }

    /**
     * Enter, called Confirm, submitting.
     * <p>
     * The counterpart of {@link #cancel}, and the same argument. Not every
     * dialog uses this word - the test case dialogs say Save for the same key,
     * because that is what it does there - so this is offered rather than
     * imposed.
     */
    public static @NotNull StatusBarShortcut confirm(final @NotNull Runnable action) {
        return build(Shortcuts.Enter, Bundle.message("shortcut.confirm"), action);
    }

    /**
     * The word the dialogs that write a test case use for the same key, which
     * {@link #confirm} says is offered rather than imposed.
     * <p>
     * Public because the key is not the only thing that says it: a dialog with
     * a Save button and a Shift+Enter hint beside it writes the word twice
     * more, and all three should be one word. It was six files before this
     * existed.
     */
    public static final @NotNull String SAVE = Bundle.message("shortcut.save");

    /**
     * Enter, called Save, submitting - {@link #confirm} in the words those
     * dialogs use.
     */
    public static @NotNull StatusBarShortcut save(final @NotNull Runnable action) {
        return build(Shortcuts.Enter, SAVE, action);
    }

    /**
     * A display-only entry for keys the component binds itself.
     */
    public static @NotNull StatusBarShortcut hint(final @NotNull String displayText, final @NotNull String name) {
        return new StatusBarShortcut(Shortcuts.EMPTY, displayText, name, NOTHING);
    }

    /**
     * What moving between rows is called, wherever a dialog has rows.
     * <p>
     * Public for the same reason as {@link #SAVE}: the word is not only said by
     * the arrows. The dialog that picks a test project calls Enter {@code Select},
     * because there the key that moves and the key that chooses are one decision
     * to a tester, and both should read as one word.
     */
    public static final @NotNull String SELECT = Bundle.message("shortcut.select");

    /**
     * The arrow keys, called Select, moving between rows - a list binds them
     * itself, so this shows them and binds nothing.
     * <p>
     * Three dialogs and a menu declare exactly this, and the arrows are written
     * as two characters that are easy to get subtly wrong. The same argument as
     * {@link #cancel}: the key, the word and the fact that nothing binds it
     * belong together, and a dialog that spelled the arrows differently would be
     * a dialog whose strip does not match the one beside it.
     */
    public static @NotNull StatusBarShortcut select() {
        return hint("↑ ↓", SELECT);
    }

    /**
     * True when this entry also binds a key; hints only render.
     */
    public boolean isBindable() {
        return shortcut != Shortcuts.EMPTY;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getShortcutText() {
        return displayText;
    }
}
