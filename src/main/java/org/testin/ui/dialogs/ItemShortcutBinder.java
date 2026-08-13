package org.testin.ui.dialogs;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Binds an item's own keyboard shortcut on a component
 * (e.g. {@code UpdateTestCaseFields::bindShortcut}).
 */
@FunctionalInterface
public interface ItemShortcutBinder<T> {
    void bind(final @NotNull T item, final @NotNull JComponent component, final @NotNull Runnable onTrigger);
}
