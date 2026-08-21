package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
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

public class ExpectedResultSection implements CreateTestCaseSection {
    final @NotNull Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);
    @Getter
    private final @NotNull EditorTextField expectedResultField;
    private final @NotNull JBPanel<?> wrapper;

    public ExpectedResultSection(final @NotNull Project p) {
        this.expectedResultField = SpellChecker.createCompletionField(p,
                new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getExpectedResults(), CreateTestCaseFields.EXPECTED_RESULT.getIcon()),
                "");
        this.expectedResultField.setOneLineMode(true);
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
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setExpectedResult(expectedResultField.getText().trim());
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCaseExpectedResult.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return expectedResultField;
    }

    @Override
    public void setEditable(final boolean editable) {
        expectedResultField.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        expectedResultField.setText(dto.getExpectedResult());
    }
}