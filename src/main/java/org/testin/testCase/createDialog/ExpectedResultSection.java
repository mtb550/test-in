package org.testin.testCase.createDialog;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
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

public class ExpectedResultSection implements ICreateTestCaseSection {
    @Getter
    private final TextFieldWithAutoCompletion<String> expectedResultField;
    private final JBPanel<?> wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    public ExpectedResultSection(final @NotNull Project p) {
        this.expectedResultField = new TextFieldWithAutoCompletion<>(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getExpectedResults(), CreateTestCaseFields.EXPECTED_RESULT.getIcon()), false, "");
        this.expectedResultField.setFont(fieldFont);
        this.expectedResultField.setPlaceholder(CreateTestCaseFields.EXPECTED_RESULT.getPlaceholder());
        this.expectedResultField.setShowPlaceholderWhenFocused(true);
        this.expectedResultField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.EXPECTED_RESULT.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.expectedResultField, BorderLayout.CENTER);
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
        expectedResultField.requestFocus();
    }

    @Override
    public void applyTo(final TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setExpectedResult(expectedResultField.getText().trim());
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JBPanel<?> slot, final TestCaseBaseDialog base, final IUIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseExpectedResult.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return expectedResultField;
    }

    @Override
    public void setEditable(final boolean editable) {
        expectedResultField.setEnabled(editable);
    }

    @Override
    public void fillData(final TestCaseDto dto, final IUIAction repackAction) {
        expectedResultField.setText(dto.getExpectedResult());
    }
}