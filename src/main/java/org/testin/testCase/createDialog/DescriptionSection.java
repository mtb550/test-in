package org.testin.testCase.createDialog;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateTestCaseFields;
import org.testin.enums.IUIAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;

public class DescriptionSection implements ICreateTestCaseSection {
    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    @Getter
    private final TextFieldWithAutoCompletion<String> descriptionField;
    private final JPanel wrapper;

    public DescriptionSection(final @NotNull Project p) {
        this.descriptionField = new TextFieldWithAutoCompletion<>(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getDescription(), CreateTestCaseFields.DESCRIPTION.getIcon()), false, "");
        this.descriptionField.setFont(fieldFont);
        this.descriptionField.setPlaceholder(CreateTestCaseFields.DESCRIPTION.getPlaceholder());
        this.descriptionField.setShowPlaceholderWhenFocused(true);
        this.descriptionField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.DESCRIPTION.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.descriptionField, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    public void setError(final boolean error) {
        if (error) {
            descriptionField.setForeground(JBColor.RED);
            descriptionField.requestFocus();
        } else
            descriptionField.setBackground(UIUtil.getTextFieldBackground());
        descriptionField.repaint();
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        descriptionField.requestFocus();
    }

    @Override
    public void applyTo(final TestCaseDto dto) {
        if (wrapper.getParent() != null && descriptionField.isEnabled())
            dto.setDescription(descriptionField.getText().trim());
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseBaseDialog base, final IUIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseDescription.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return descriptionField;
    }

    @Override
    public void setEditable(final boolean editable) {
        descriptionField.setEnabled(editable);
    }

    @Override
    public void fillData(final TestCaseDto dto, final IUIAction repackAction) {
        descriptionField.setText(dto.getDescription());
    }
}