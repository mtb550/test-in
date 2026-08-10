package org.testin.statusBar;

import org.testin.util.Shortcuts;

/**
 * Status bar used by the shared action dialogs.
 */
public final class DialogStatusBar extends StatusBarBase {

    public DialogStatusBar() {
        this(new IStatusBarItem[]{
                new ShortcutItem("Confirm", Shortcuts.Enter.getShortcutText()),
                new ShortcutItem("Cancel", Shortcuts.Escape.getShortcutText())
        });
    }

    private DialogStatusBar(final IStatusBarItem[] items) {
        super(items);
    }
}
