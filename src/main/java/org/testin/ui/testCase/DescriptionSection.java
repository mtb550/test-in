package org.testin.ui.testCase;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.services.TestCaseCacheService;

import javax.swing.*;
import java.awt.*;

public class DescriptionSection implements ICreateTestCaseSection {
    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    @Getter
    private final TextFieldWithAutoCompletion<String> descriptionField;
    private final JPanel wrapper;

    public DescriptionSection(final @NotNull Project project) {
        this.descriptionField = new TextFieldWithAutoCompletion<>(project, new TextFieldWithAutoCompletion.StringsCompletionProvider(TestCaseCacheService.getInstance(project).getDescription(), CreateTestCaseFields.DESCRIPTION.getIcon()), false, "");
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
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.IUIAction repackAction) {
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
    public void fillData(final TestCaseDto dto, final TestCaseUIBase.IUIAction repackAction) {
        descriptionField.setText(dto.getDescription());
    }
}