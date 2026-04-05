package testGit.ui.createTestCase;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class PrioritySection implements CreateTestCaseSection {
    private final ComboBox<Priority> priority;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 2f);

    public PrioritySection() {
        this.priority = new ComboBox<>(Priority.values());
        this.priority.setSelectedItem(Priority.LOW);
        this.priority.setFont(fieldFont);

        this.priority.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull final JList<? extends Priority> list, final Priority value, final int index, final boolean selected, final boolean hasFocus) {
                if (value != null) {
                    setIcon(value.getIcon());
                    append(" Priority: " + value.name());
                }
            }
        });

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        JLabel iconLabel = new JLabel(CreateField.PRIORITY.getIcon());
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        this.wrapper.add(iconLabel, BorderLayout.WEST);
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
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final CreateTestCaseBase base, final CreateTestCaseBase.UIAction repackAction, final Set<String> uniqueStepsCache) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCasePriority.getShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return priority;
    }
}