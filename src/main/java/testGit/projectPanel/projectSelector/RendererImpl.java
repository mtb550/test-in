package testGit.projectPanel.projectSelector;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;

import javax.swing.*;

public class RendererImpl extends ColoredListCellRenderer<TestProjectDirectoryDto> {
    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends TestProjectDirectoryDto> list, TestProjectDirectoryDto value, int index, boolean selected, boolean hasFocus) {

        if (value != null)
            append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
}