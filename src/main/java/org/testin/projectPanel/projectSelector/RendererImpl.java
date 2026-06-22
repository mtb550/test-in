package org.testin.projectPanel.projectSelector;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;

import javax.swing.*;

public class RendererImpl extends ColoredListCellRenderer<TestProjectDirectoryDto> {

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends TestProjectDirectoryDto> list, final @NotNull TestProjectDirectoryDto value, final int index, final boolean selected, final boolean hasFocus) {
        boolean isActive = value.getMarker().getStatus() == ProjectStatus.ACTIVE;
        SimpleTextAttributes attributes = isActive ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES;
        append(value.getName(), attributes);

        if (!isActive)
            append(" (Inactive)", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    }
}