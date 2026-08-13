package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.ui.dialogs.FramelessDialogWrapper;

import javax.swing.*;

/**
 * Collects the Git identity required for a first commit.
 */
final class GitIdentityDialog extends FramelessDialogWrapper {

    private final @NotNull JBTextField nameField = new JBTextField();
    private final @NotNull JBTextField emailField = new JBTextField();
    private final @NotNull JBCheckBox globalCheckBox = new JBCheckBox("Set globally");

    GitIdentityDialog(final @Nullable Project project) {
        super(project, true);
        setTitle("Set Git Identity and Commit");
        initFrameless();
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Name:", nameField)
                .addLabeledComponent("Email:", emailField)
                .addComponent(globalCheckBox)
                .getPanel();
    }

    @Override
    public @NotNull JComponent getPreferredFocusedComponent() {
        return nameField;
    }

    @NotNull String getUserName() {
        return nameField.getText();
    }

    @NotNull String getUserEmail() {
        return emailField.getText();
    }

    boolean isSetGlobalConfig() {
        return globalCheckBox.isSelected();
    }
}
