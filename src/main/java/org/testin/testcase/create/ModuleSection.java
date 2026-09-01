package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBPanel;
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

public class ModuleSection implements CreateTestCaseSection {
    @Getter
    private final @NotNull EditorTextField moduleField;
    private final @NotNull JBPanel<?> wrapper;

    public ModuleSection(final @NotNull Project p) {
        this.moduleField = SpellChecker.createCompletionField(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getModules(), CreateTestCaseFields.MODULE.getIcon()), "");
        this.moduleField.setOneLineMode(true);
        styleField(this.moduleField, CreateTestCaseFields.MODULE);

        this.wrapper = createWrapper(CreateTestCaseFields.MODULE.getIcon(), this.moduleField);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setModule(moduleField.getText().trim());
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
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
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        moduleField.setText(dto.getModule());
    }
}