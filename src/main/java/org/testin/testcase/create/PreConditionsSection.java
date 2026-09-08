package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;

import javax.swing.*;

public class PreConditionsSection implements CreateTestCaseSection {
    @Getter
    private final @NotNull EditorTextField preConditionsField;
    private final @NotNull JBPanel<?> wrapper;

    public PreConditionsSection(final @NotNull Project p) {
        this.preConditionsField = SpellChecker.createField(p);
        this.preConditionsField.setOneLineMode(true);
        styleField(this.preConditionsField, CreateTestCaseFields.PRE_CONDITIONS);

        this.wrapper = createWrapper(CreateTestCaseFields.PRE_CONDITIONS.getIcon(), this.preConditionsField);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setPreConditions(preConditionsField.getText().trim());
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-028
    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        // Advertised as well as bound - the field is in the dialog's jump map.
        // The key was taken away once because it was not, which left the field
        // drawn in a dialog with no way at all to reach it.
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCasePreConditions.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return preConditionsField;
    }

    @Override
    public void setEditable(final boolean editable) {
        preConditionsField.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        preConditionsField.setText(dto.getPreConditions());
    }
}