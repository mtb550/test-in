package testGit.ui.single;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.fields.ExtendableTextField;
import testGit.pojo.Priority;
import testGit.ui.bulk.UpdateField;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Set;

import static testGit.ui.single.SingleEditorUIFactory.addStepField;
import static testGit.ui.single.SingleEditorUIFactory.registerShortcut;

public class SingleEditorShortcutManager {

    public static void registerShortcuts(
            Project project, Set<String> uniqueStepsCache,
            JPanel mainPanel, JPanel contentPanel,
            boolean isExtendable, UpdateField targetField,
            Runnable repackPopup,
            JPanel expectedWrapper, ExtendableTextField expectedField,
            JPanel priorityWrapper, ComboBox<Priority> priorityCombo,
            JPanel groupsWrapper,
            JPanel stepsWrapper, JPanel stepsContainer, List<TextFieldWithAutoCompletion<String>> stepFields,
            Runnable saveAction) {

        if (isExtendable) {
            registerShortcut(mainPanel, KeyboardSet.getShortcutFor(UpdateField.EXPECTED.getShortcut(), InputEvent.CTRL_DOWN_MASK), () -> {
                if (expectedWrapper.getParent() == null) contentPanel.add(expectedWrapper);
                repackPopup.run();
                expectedField.requestFocus();
            });
            registerShortcut(mainPanel, KeyboardSet.getShortcutFor(UpdateField.PRIORITY.getShortcut(), InputEvent.CTRL_DOWN_MASK), () -> {
                if (priorityWrapper.getParent() == null) contentPanel.add(priorityWrapper);
                repackPopup.run();
                priorityCombo.requestFocus();
            });
            registerShortcut(mainPanel, KeyboardSet.getShortcutFor(UpdateField.GROUPS.getShortcut(), InputEvent.CTRL_DOWN_MASK), () -> {
                if (groupsWrapper.getParent() == null) contentPanel.add(groupsWrapper);
                repackPopup.run();
            });
            registerShortcut(mainPanel, KeyboardSet.getShortcutFor(UpdateField.STEPS.getShortcut(), InputEvent.CTRL_DOWN_MASK), () -> {
                if (stepsWrapper.getParent() == null) contentPanel.add(stepsWrapper);
                addStepField(project, stepsContainer, stepFields, "", repackPopup, uniqueStepsCache);
                repackPopup.run();
                stepFields.getLast().requestFocus();
            });
        } else if (targetField == UpdateField.STEPS) {
            registerShortcut(mainPanel, KeyboardSet.getShortcutFor(UpdateField.STEPS.getShortcut(), InputEvent.CTRL_DOWN_MASK), () -> {
                addStepField(project, stepsContainer, stepFields, "", repackPopup, uniqueStepsCache);
                repackPopup.run();
                stepFields.getLast().requestFocus();
            });
        }

        registerShortcut(mainPanel, KeyboardSet.TabNext.getShortcut(), () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent());
        registerShortcut(mainPanel, KeyboardSet.TabPrevious.getShortcut(), () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent());
        registerShortcut(mainPanel, KeyboardSet.Enter.getShortcut(), saveAction);
    }
}