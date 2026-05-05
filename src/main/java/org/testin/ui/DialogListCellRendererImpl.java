package org.testin.ui;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryType;

import javax.swing.*;

public class DialogListCellRendererImpl extends ColoredListCellRenderer<DirectoryType> {

    @Override
    protected void customizeCellRenderer(@NotNull final JList<? extends DirectoryType> list, final DirectoryType value, final int index, final boolean selected, final boolean hasFocus) {
        setIcon(value.getIcon());
        append(value.getDescription(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        setBorder(JBUI.Borders.empty(6));
    }
}