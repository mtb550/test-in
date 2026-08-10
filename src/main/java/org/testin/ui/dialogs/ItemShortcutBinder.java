package org.testin.ui.dialogs;

import javax.swing.*;

/**
 * Binds an item's own keyboard shortcut on a component
 * (e.g. {@code UpdateTestCaseFields::bindShortcut}).
 */
@FunctionalInterface
public interface ItemShortcutBinder<T> {
    void bind(T item, JComponent component, Runnable onTrigger);
}
