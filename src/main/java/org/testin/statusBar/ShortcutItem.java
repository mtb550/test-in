package org.testin.statusBar;

/**
 * Plain name + shortcut pair for the dialog status bars.
 */
record ShortcutItem(String name, String shortcutText) implements IStatusBarItem {

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortcutText() {
        return shortcutText;
    }
}
