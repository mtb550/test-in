package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;

import javax.swing.*;

/**
 * Test data, typed over as many lines as it takes and stored exactly as typed.
 * <p>
 * Multi-line for the same reason Expected Result is, and by the same code. What
 * a tester puts here is whatever the case needs to run - a username and a
 * password, a query, a payload, a table of values - so this field cannot know
 * what a character means and has no business changing any of them.
 * <p>
 * It was a one-line field, and the details panel and the grid made up for that by
 * putting each comma-separated entry on its own line. That replaced the comma
 * rather than breaking after it, so every comma inside a value disappeared on the
 * way to the screen: a query lost the separators between its columns, and a list
 * of values became a list of lines that could not be read back. Both surfaces now
 * show what is stored, and what is stored is what was typed.
 */
public class TestDataSection extends AbstractMultiLineSection {

    public TestDataSection(final @NotNull Project p) {
        super(p, new EditorTextField(), CreateTestCaseFields.TEST_DATA);
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setTestData(field.getText().trim());
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        // No key for test data. It had one, but the status bar never advertised it -
        // the section is not in the dialog's jump map - so it was a binding
        // nobody could discover and only competed with the keys that are shown.
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        field.setText(dto.getTestData());
    }
}
