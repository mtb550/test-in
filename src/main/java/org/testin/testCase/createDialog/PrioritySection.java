package org.testin.testCase.createDialog;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateTestCaseFields;
import org.testin.enums.IUIAction;
import org.testin.enums.Priority;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.IconManager;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

public class PrioritySection implements ICreateTestCaseSection {
    private final ComboBox<Priority> priority;
    private final JBPanel<?> wrapper;
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
            protected void customizeCellRenderer(final @NotNull JList<? extends Priority> list, final Priority value, final int index, final boolean selected, final boolean hasFocus) {
                if (value != null) {
                    setIcon(IconManager.createIcon(value.getColor()));
                    append(" Priority:  ");
                    append(value.name());
                    append("    " + value.getShortcut().getShortcutText(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY));
                }
            }
        });

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.PRIORITY.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.priority, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JBPanel<?> contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        priority.requestFocus();
    }

    public ComboBox<Priority> getCombo() {
        return priority;
    }

    @Override
    public void applyTo(final TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setPriority((Priority) Objects.requireNonNull(priority.getSelectedItem()));
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JBPanel<?> slot, final TestCaseBaseDialog base, final IUIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCasePriority.getCustomShortcut(), () -> {
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
    public void fillData(final TestCaseDto dto, final IUIAction repackAction) {
        priority.setSelectedItem(dto.getPriority());
    }
}