package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;

import javax.swing.*;
import java.awt.*;

public class DescriptionSection implements CreateTestCaseSection {
    final @NotNull Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    @Getter
    private final @NotNull EditorTextField descriptionField;
    private final @NotNull JBPanel<?> wrapper;

    public DescriptionSection(final @NotNull Project p) {
        this.descriptionField = SpellChecker.createCompletionField(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getDescription(), CreateTestCaseFields.DESCRIPTION.getIcon()), "");
        this.descriptionField.setOneLineMode(true);
        this.descriptionField.setFont(fieldFont);
        this.descriptionField.setPlaceholder(CreateTestCaseFields.DESCRIPTION.getPlaceholder());
        this.descriptionField.setShowPlaceholderWhenFocused(true);
        this.descriptionField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JBPanel<>(new BorderLayout());
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
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setDescription(descriptionField.getText().trim());
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCaseDescription.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return descriptionField;
    }

    @Override
    public void setEditable(final boolean editable) {
        descriptionField.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        descriptionField.setText(dto.getDescription());
    }
}