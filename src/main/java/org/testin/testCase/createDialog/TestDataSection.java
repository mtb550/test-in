package org.testin.testCase.createDialog;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateTestCaseFields;
import org.testin.enums.UIAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;

public class TestDataSection implements CreateTestCaseSection {
    final @NotNull Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);
    @Getter
    private final @NotNull EditorTextField testDataField;
    private final @NotNull JBPanel<?> wrapper;

    public TestDataSection() {
        this.testDataField = new EditorTextField();
        this.testDataField.setFont(fieldFont);
        this.testDataField.setPlaceholder(CreateTestCaseFields.TEST_DATA.getPlaceholder());
        this.testDataField.setShowPlaceholderWhenFocused(true);
        this.testDataField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.TEST_DATA.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.testDataField, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));

    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final @NotNull JBPanel<?> contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        testDataField.requestFocus();
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setTestData(testDataField.getText().trim());
        }
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCaseTestData.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return testDataField;
    }

    @Override
    public void setEditable(final boolean editable) {
        testDataField.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        testDataField.setText(dto.getTestData());
    }
}