package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.statusBar.IStatusBarItem;
import org.testin.util.Shortcuts;

/**
 * One status bar entry of a framework dialog: the keystroke, the name shown to
 * the tester, and the action the key runs. The same declaration renders the
 * hint and binds the key, so a shortcut cannot be shown without working or
 * work without being shown (issue #11). Keys a component already binds itself
 * (e.g. list navigation) are declared as display-only {@link #hint}s.
 */
public record StatusBarShortcut(@Nullable Shortcuts shortcut,
                                @NotNull String displayText,
                                @NotNull String name,
                                @Nullable Runnable action) implements IStatusBarItem {

    public static @NotNull StatusBarShortcut build(final @NotNull Shortcuts shortcut, final @NotNull String name, final @NotNull Runnable action) {
        return new StatusBarShortcut(shortcut, shortcut.getShortcutText(), name, action);
    }

    /**
     * A display-only entry for keys the component binds itself.
     */
    public static @NotNull StatusBarShortcut hint(final @NotNull String displayText, final @NotNull String name) {
        return new StatusBarShortcut(null, displayText, name, null);
    }

    /**
     * True when this entry also binds a key; hints only render.
     */
    public boolean isBindable() {
        return shortcut != null && action != null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortcutText() {
        return displayText;
    }
}
