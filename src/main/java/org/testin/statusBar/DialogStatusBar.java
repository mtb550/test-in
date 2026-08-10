package org.testin.statusBar;

import org.testin.util.KeyboardSet;

/**
 * Status bar used by the shared action dialogs.
 */
public final class DialogStatusBar extends StatusBarBase {

    public DialogStatusBar() {
        this(new IStatusBarItem[]{
                new ShortcutItem("Confirm", KeyboardSet.Enter.getShortcutText()),
                new ShortcutItem("Cancel", KeyboardSet.Escape.getShortcutText())
        });
    }

    private DialogStatusBar(final IStatusBarItem[] items) {
        super(items);
    }

    private record ShortcutItem(String name, String shortcutText) implements IStatusBarItem {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getShortcutText() {
            return shortcutText;
        }
    }
}
