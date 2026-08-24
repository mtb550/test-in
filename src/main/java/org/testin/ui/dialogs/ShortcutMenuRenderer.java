package org.testin.ui.dialogs;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Function;

/**
 * Row renderer for {@link ShortcutMenuPopup}: icon, label, grayed shortcut text.
 */
final class ShortcutMenuRenderer<T> extends ColoredListCellRenderer<T> {

    private final @NotNull Function<T, Icon> icon;
    private final @NotNull Function<T, String> label;
    private final @NotNull Function<T, String> shortcutText;

    ShortcutMenuRenderer(final @NotNull Function<T, Icon> icon, final @NotNull Function<T, String> label, final @NotNull Function<T, String> shortcutText) {
        this.icon = icon;
        this.label = label;
        this.shortcutText = shortcutText;
    }

    @Override
    protected void customizeCellRenderer(final @NotNull JList<? extends T> list, final T value, final int index, final boolean selected, final boolean hasFocus) {
        setIcon(icon.apply(value));
        append(label.apply(value));
        append("   " + shortcutText.apply(value), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        setBorder(JBUI.Borders.empty(6, 12));
    }
}
