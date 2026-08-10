package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Collects the Git identity required for a first commit.
 */
final class GitIdentityDialog extends DialogWrapper {

    private final JBTextField nameField = new JBTextField();
    private final JBTextField emailField = new JBTextField();
    private final JBCheckBox globalCheckBox = new JBCheckBox("Set globally");

    GitIdentityDialog(final @Nullable Project project) {
        super(project, true);
        setTitle("Set Git Identity and Commit");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Name:", nameField)
                .addLabeledComponent("Email:", emailField)
                .addComponent(globalCheckBox)
                .getPanel();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return nameField;
    }

    String getUserName() {
        return nameField.getText();
    }

    String getUserEmail() {
        return emailField.getText();
    }

    boolean isSetGlobalConfig() {
        return globalCheckBox.isSelected();
    }
}
