package testGit.projectPanel.projectSelector;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.tree.dirs.TestProjectDirectory;

import javax.swing.*;

public class RendererImpl extends ColoredListCellRenderer<TestProjectDirectory> {
    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends TestProjectDirectory> list, TestProjectDirectory value, int index, boolean selected, boolean hasFocus) {

        if (value != null)
            append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
}