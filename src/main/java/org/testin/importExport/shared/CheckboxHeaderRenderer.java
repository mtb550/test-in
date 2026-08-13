package org.testin.importExport.shared;

import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class CheckboxHeaderRenderer implements TableCellRenderer {
    private final @NotNull JBCheckBox headerCheckbox;

    public CheckboxHeaderRenderer(final @NotNull JBCheckBox headerCheckbox) {
        this.headerCheckbox = headerCheckbox;
    }

    @Override
    public @NotNull Component getTableCellRendererComponent(final @NotNull JTable table, final @Nullable Object value,
                                                            final boolean isSelected, final boolean hasFocus,
                                                            final int row, final int column) {
        final JTableHeader header = table.getTableHeader();
        headerCheckbox.setBackground(header.getBackground());
        headerCheckbox.setForeground(header.getForeground());
        headerCheckbox.setFont(header.getFont());
        headerCheckbox.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        return headerCheckbox;
    }
}
