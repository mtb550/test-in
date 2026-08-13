package org.testin.testCase.createDialog;

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
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
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;

import javax.swing.*;
import java.awt.*;

public class ModuleSection implements ICreateTestCaseSection {
    final @NotNull Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);
    @Getter
    private final @NotNull EditorTextField moduleField;
    private final @NotNull JBPanel<?> wrapper;

    public ModuleSection(final @NotNull Project p) {
        this.moduleField = SpellChecker.createCompletionField(p,
                new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getModules(), CreateTestCaseFields.MODULE.getIcon()),
                "");
        this.moduleField.setOneLineMode(true);
        this.moduleField.setFont(fieldFont);
        this.moduleField.setPlaceholder(CreateTestCaseFields.MODULE.getPlaceholder());
        this.moduleField.setShowPlaceholderWhenFocused(true);
        this.moduleField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.MODULE.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.moduleField, BorderLayout.CENTER);
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
        moduleField.requestFocus();
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setModule(moduleField.getText().trim());
        }
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull IUIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCaseModule.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return moduleField;
    }

    @Override
    public void setEditable(final boolean editable) {
        moduleField.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull IUIAction repackAction) {
        moduleField.setText(dto.getModule());
    }
}