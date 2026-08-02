package org.testin.testCase.createDialog;

import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.testin.enums.CreateTestCaseFields;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;

public class TestDataSection implements ICreateTestCaseSection {
    @Getter
    private final EditorTextField testDataField;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    public TestDataSection() {
        this.testDataField = new EditorTextField();
        this.testDataField.setFont(fieldFont);
        this.testDataField.setPlaceholder(CreateTestCaseFields.TEST_DATA.getPlaceholder());
        this.testDataField.setShowPlaceholderWhenFocused(true);
        this.testDataField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.TEST_DATA.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.testDataField, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));

    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        testDataField.requestFocus();
    }

    @Override
    public void applyTo(final TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setTestData(testDataField.getText().trim());
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseDialogBase base, final TestCaseDialogBase.IUIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseTestData.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return testDataField;
    }

    @Override
    public void setEditable(final boolean editable) {
        testDataField.setEnabled(editable);
    }

    @Override
    public void fillData(final TestCaseDto dto, final TestCaseDialogBase.IUIAction repackAction) {
        testDataField.setText(dto.getTestData());
    }
}