package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.SpellChecker;
import org.testin.util.Shortcuts;

/**
 * The module this test case belongs to, completing from the modules the project
 * already uses.
 */
public class ModuleSection extends AbstractOneLineSection {

    public ModuleSection(final @NotNull Project p) {
        super(SpellChecker.createCompletionField(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseValues.class).getModules(), CreateTestCaseFields.MODULE.getIcon()), ""),
                CreateTestCaseFields.MODULE, Shortcuts.CreateTestCaseModule);
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-032
    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setModule(field.getText().trim());
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        field.setText(dto.getModule());
    }
}
