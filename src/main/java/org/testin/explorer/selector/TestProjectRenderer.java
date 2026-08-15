package org.testin.explorer.selector;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;

import javax.swing.*;

public class TestProjectRenderer extends ColoredListCellRenderer<TestProjectDirectoryDto> {

    @Override
    protected void customizeCellRenderer(final @NotNull JList<? extends TestProjectDirectoryDto> list,
                                         final @Nullable TestProjectDirectoryDto value, final int index,
                                         final boolean selected, final boolean hasFocus) {
        // ColoredListCellRenderer passes the value straight through with no null
        // check, and the combo renders a null selection - which is what it holds
        // while no test project is configured.
        if (value == null) return;

        final boolean isActive = value.getMarker().getStatus() == ProjectStatus.ACTIVE;
        final SimpleTextAttributes attributes = isActive ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES;
        append(value.getName(), attributes);

        if (!isActive)
            append(" (Inactive)", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    }
}