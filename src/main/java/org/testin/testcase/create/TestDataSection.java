package org.testin.testcase.create;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;

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
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setTestData(testDataField.getText().trim());
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        // No key for test data. It had one, but the status bar never advertised it -
        // the section is not in the dialog's jump map - so it was a binding
        // nobody could discover and only competed with the keys that are shown.
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