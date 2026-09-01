package org.testin.testcase.create;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Priority;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.IconManager;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class PrioritySection implements CreateTestCaseSection {
    private final @NotNull ComboBox<Priority> priority;
    private final @NotNull JBPanel<?> wrapper;

    public PrioritySection() {
        final Priority @NotNull[] activePriorities = Arrays.stream(Priority.values())
                .filter(Priority::isActive)
                .toArray(Priority[]::new);

        this.priority = new ComboBox<>(activePriorities);
        this.priority.setSelectedItem(Priority.LOW);
        this.priority.setFont(fieldFont());

        this.priority.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(final @NotNull JList<? extends Priority> list, final Priority value, final int index, final boolean selected, final boolean hasFocus) {
                // Swing renders the empty selection with no value at all, and
                // there is nothing to draw for it.
                Optional.ofNullable(value).ifPresent(priority -> {
                    setIcon(IconManager.createIcon(priority.getColor()));
                    append(" Priority:  ");
                    append(priority.name());
                    append("    " + priority.getShortcut().getShortcutText(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY));
                });
            }
        });

        this.wrapper = createWrapper(CreateTestCaseFields.PRIORITY.getIcon(), this.priority);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    public @NotNull ComboBox<Priority> getCombo() {
        return priority;
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setPriority((Priority) Objects.requireNonNull(priority.getSelectedItem()));
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCasePriority.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return priority;
    }

    @Override
    public void setEditable(final boolean editable) {
        priority.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        priority.setSelectedItem(dto.getPriority());
    }
}