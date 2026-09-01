package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.NameSanitizer;
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;

import javax.swing.*;

public class DescriptionSection implements CreateTestCaseSection {
    private final @NotNull Project p;
    @Getter
    private final @NotNull EditorTextField descriptionField;
    private final @NotNull JBPanel<?> wrapper;

    public DescriptionSection(final @NotNull Project p) {
        this.p = p;
        this.descriptionField = SpellChecker.createCompletionField(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getDescription(), CreateTestCaseFields.DESCRIPTION.getIcon()), "");
        this.descriptionField.setOneLineMode(true);
        styleField(this.descriptionField, CreateTestCaseFields.DESCRIPTION);

        this.wrapper = createWrapper(CreateTestCaseFields.DESCRIPTION.getIcon(), this.descriptionField);
    }

    public void setError(final boolean error) {
        if (error) {
            descriptionField.setForeground(JBColor.RED);
            descriptionField.requestFocus();
        } else
            // The foreground, which is what the error turned red. This set the
            // background instead, so a field that had once been refused stayed
            // red however it was corrected - invisible while nothing cleared the
            // error, and visible the moment something did.
            descriptionField.setForeground(UIUtil.getTextFieldForeground());
        descriptionField.repaint();
    }

    /**
     * Refuses a description the generated code could not be named after.
     * <p>
     * Here rather than in the generator, which is where it used to be found: by
     * then the description is stored, the method is being renamed or written,
     * and the answer is either an exception the tester did not cause or a method
     * declaration Java will not compile. A description is typed once and read
     * from for the life of the case, so this is the moment to say no (#66).
     * <p>
     * A blank one is not this section's refusal to make - the save has always
     * owned that, and it says so differently.
     */
    @Override
    public boolean accepts() {
        final @NotNull String description = descriptionField.getText().trim();
        if (description.isEmpty() || NameSanitizer.canMakeMethodName(description)) {
            setError(false);
            return true;
        }

        setError(true);
        Services.getInstance(p, Notifier.class).softRefuse(p,
                "That description cannot name a test method",
                "The generated method would be called \"" + NameSanitizer.methodName(description)
                        + "\", which Java will not accept. A description has to begin with a letter, "
                        + "and cannot be a single word Java keeps for itself such as new or class.");

        return false;
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