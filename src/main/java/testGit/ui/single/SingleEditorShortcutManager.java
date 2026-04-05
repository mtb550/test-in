package testGit.ui.single;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.ExtendableTextField;
import testGit.pojo.Priority;
import testGit.ui.bulk.UpdateField;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

import static testGit.ui.single.SingleEditorUIFactory.addStepField;
import static testGit.ui.single.SingleEditorUIFactory.registerShortcut;

public class SingleEditorShortcutManager {

    public static void registerShortcuts(
            final Project project,
            final Set<String> uniqueStepsCache,
            final JPanel mainPanel,
            final JPanel contentPanel,
            final boolean isExtendable,
            final UpdateField targetField,
            final Runnable repackPopup,
            final JPanel expectedWrapper,
            final ExtendableTextField expectedField,
            final JPanel priorityWrapper,
            final ComboBox<Priority> priorityCombo,
            final JPanel groupsWrapper,
            final JPanel stepsWrapper,
            final JPanel stepsContainer,
            final List<TextFieldWithAutoCompletion<String>> stepFields,
            final Runnable saveAction) {

        if (isExtendable) {

            registerShortcut(mainPanel, KeyboardSet.CreateTestCaseExpected.getShortcut(), () -> {
                if (expectedWrapper.getParent() == null) contentPanel.add(expectedWrapper);
                repackPopup.run();
                expectedField.requestFocus();
            });

            registerShortcut(mainPanel, KeyboardSet.CreateTestCasePriority.getShortcut(), () -> {
                if (priorityWrapper.getParent() == null) contentPanel.add(priorityWrapper);
                repackPopup.run();
                priorityCombo.requestFocus();
            });

            registerShortcut(mainPanel, KeyboardSet.CreateTestCaseGroups.getShortcut(), () -> {
                if (groupsWrapper.getParent() == null) contentPanel.add(groupsWrapper);
                repackPopup.run();
                focusFirstCheckbox(groupsWrapper);
            });

            registerShortcut(mainPanel, KeyboardSet.CreateTestCaseAddStep.getShortcut(), () -> {
                if (stepsWrapper.getParent() == null) contentPanel.add(stepsWrapper);
                addStepField(project, stepsContainer, stepFields, "", repackPopup, uniqueStepsCache);
                repackPopup.run();
                stepFields.getLast().requestFocus();
            });

        } else if (targetField == UpdateField.STEPS) {
            registerShortcut(mainPanel, KeyboardSet.CreateTestCaseAddStep.getShortcut(), () -> {
                addStepField(project, stepsContainer, stepFields, "", repackPopup, uniqueStepsCache);
                repackPopup.run();
                stepFields.getLast().requestFocus();
            });
        }

        registerShortcut(mainPanel, KeyboardSet.TabNext.getShortcut(), () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent());
        registerShortcut(mainPanel, KeyboardSet.TabPrevious.getShortcut(), () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent());
        registerShortcut(mainPanel, KeyboardSet.Enter.getShortcut(), saveAction);
    }

    private static void focusFirstCheckbox(final JPanel groupsWrapper) {
        for (Component child : groupsWrapper.getComponents()) {
            if (child instanceof JPanel groupsPanel) {
                for (Component c : groupsPanel.getComponents()) {
                    if (c instanceof JBCheckBox cb) {
                        SwingUtilities.invokeLater(cb::requestFocusInWindow);
                        return;
                    }
                }
            }
        }
    }
}