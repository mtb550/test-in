package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;
import org.testin.statusbar.StatusBarItem;
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

    public static @NotNull StatusBarShortcut build(final @NotNull Shortcuts shortcut, final @NotNull String name, final @NotNull Runnable action) {
        return new StatusBarShortcut(shortcut, shortcut.getShortcutText(), name, action);
    }

    /**
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
        return build(Shortcuts.Escape, "Cancel", action);
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
        return build(Shortcuts.Enter, "Confirm", action);
    }

    /**
     * A display-only entry for keys the component binds itself.
     */
    public static @NotNull StatusBarShortcut hint(final @NotNull String displayText, final @NotNull String name) {
        return new StatusBarShortcut(Shortcuts.EMPTY, displayText, name, NOTHING);
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
