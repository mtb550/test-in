package org.testin.testCase.createDialog;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.testin.enums.CreateTestCaseFields;
import org.testin.enums.IUIAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;

public class PreConditionsSection implements ICreateTestCaseSection {
    @Getter
    private final EditorTextField preConditionsField;
    private final JBPanel<?> wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    public PreConditionsSection() {
        this.preConditionsField = new EditorTextField();
        this.preConditionsField.setFont(fieldFont);
        this.preConditionsField.setPlaceholder(CreateTestCaseFields.PRE_CONDITIONS.getPlaceholder());
        this.preConditionsField.setShowPlaceholderWhenFocused(true);
        this.preConditionsField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.PRE_CONDITIONS.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.preConditionsField, BorderLayout.CENTER);
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
        preConditionsField.requestFocus();
    }

    @Override
    public void applyTo(final TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setPreConditions(preConditionsField.getText().trim());
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JBPanel<?> slot, final TestCaseBaseDialog base, final IUIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCasePreConditions.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return preConditionsField;
    }

    @Override
    public void setEditable(final boolean editable) {
        preConditionsField.setEnabled(editable);
    }

    @Override
    public void fillData(final TestCaseDto dto, final IUIAction repackAction) {
        preConditionsField.setText(dto.getPreConditions());
    }
}