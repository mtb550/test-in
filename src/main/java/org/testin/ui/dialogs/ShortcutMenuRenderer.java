package org.testin.ui.dialogs;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.statusbar.MenuItem;

import javax.swing.*;

/**
 * Row renderer for {@link ShortcutMenuPopup}: icon, name, grayed shortcut text.
 */
final class ShortcutMenuRenderer<T extends MenuItem> extends ColoredListCellRenderer<T> {

    @Override
    protected void customizeCellRenderer(final @NotNull JList<? extends T> list, final T value, final int index, final boolean selected, final boolean hasFocus) {
        setIcon(value.getIcon());
        append(value.getName());
        append("   " + value.getShortcutText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        setBorder(JBUI.Borders.empty(6, 12));
    }
}
