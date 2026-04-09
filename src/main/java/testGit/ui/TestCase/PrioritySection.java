package testGit.ui.TestCase;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class PrioritySection implements CreateTestCaseSection {
    private final ComboBox<Priority> priority;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 2f);

    public PrioritySection() {
        Priority[] activePriorities = Arrays.stream(Priority.values())
                .filter(Priority::isActive)
                .toArray(Priority[]::new);

        this.priority = new ComboBox<>(activePriorities);
        this.priority.setSelectedItem(Priority.LOW);
        this.priority.setFont(fieldFont);

        this.priority.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull final JList<? extends Priority> list, final Priority value, final int index, final boolean selected, final boolean hasFocus) {
                if (value != null) {
                    setIcon(value.getIcon());
                    append(" Priority:  ");
                    append(value.name());
                    append("    " + value.getShortcutText(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY));
                }
            }
        });

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.PRIORITY.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.priority, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        priority.requestFocus();
    }

    public ComboBox<Priority> getCombo() {
        return priority;
    }

    @Override
    public void applyTo(TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setPriority((Priority) priority.getSelectedItem());
        } else if (dto.getPriority() == null) {
            dto.setPriority(Priority.LOW);
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.UIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCasePriority.getShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return priority;
    }

    @Override
    public void setEditable(final boolean editable) {
        priority.setEnabled(editable);
    }

    @Override
    public void fillData(final TestCaseDto dto, final TestCaseUIBase.UIAction repackAction) {
        if (dto.getPriority() != null) {
            priority.setSelectedItem(dto.getPriority());
        }
    }
}