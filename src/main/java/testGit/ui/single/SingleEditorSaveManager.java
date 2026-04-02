package testGit.ui.single;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.ExtendableTextField;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SingleEditorSaveManager {

    public static Runnable createSaveAction(
            TestCaseDto dto,
            ExtendableTextField titleField,
            JPanel expectedWrapper, ExtendableTextField expectedField,
            JPanel priorityWrapper, ComboBox<Priority> priorityCombo,
            JPanel groupsWrapper, JPanel groupsPanel,
            JPanel stepsWrapper, List<TextFieldWithAutoCompletion<String>> stepFields,
            Consumer<TestCaseDto> onSave,
            JBPopup[] popupWrapper) {

        return () -> {
            if (titleField != null) dto.setTitle(titleField.getText().trim());
            if (expectedWrapper.getParent() != null) dto.setExpected(expectedField.getText().trim());

            if (priorityWrapper.getParent() != null) dto.setPriority((Priority) priorityCombo.getSelectedItem());
            else if (dto.getPriority() == null) dto.setPriority(Priority.LOW);

            if (groupsWrapper.getParent() != null) {
                List<Groups> selectedGroups = new ArrayList<>();
                for (Component c : groupsPanel.getComponents()) {
                    if (c instanceof JBCheckBox checkBox && checkBox.isSelected()) {
                        selectedGroups.add(Groups.valueOf(checkBox.getText()));
                    }
                }
                dto.setGroups(selectedGroups.isEmpty() ? null : selectedGroups);
            }

            if (stepsWrapper.getParent() != null) {
                List<String> finalSteps = new ArrayList<>();
                for (TextFieldWithAutoCompletion<String> sf : stepFields) {

                    if (!sf.getText().trim().isEmpty())
                        finalSteps.add(sf.getText().trim());
                }
                dto.setSteps(finalSteps.isEmpty() ? null : finalSteps);
            }

            if (titleField == null || !dto.getTitle().isEmpty()) {
                onSave.accept(dto);
                popupWrapper[0].closeOk(null);
            }
        };
    }
}