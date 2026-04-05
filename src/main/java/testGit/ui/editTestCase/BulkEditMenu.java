package testGit.ui.editTestCase;

import testGit.pojo.dto.TestCaseDto;
import testGit.ui.GenericSelectionPopup;
import testGit.ui.editTestCase.bulk.ExpectedBulkEditor;
import testGit.ui.editTestCase.bulk.PriorityBulkEditor;
import testGit.ui.editTestCase.bulk.StepsBulkEditor;
import testGit.ui.editTestCase.bulk.TitleBulkEditor;

import java.util.Arrays;
import java.util.List;

public class BulkEditMenu {

    public static void show(final List<TestCaseDto> selectedItems, final Runnable onUpdate) {
        UpdateField[] editableFields = Arrays.stream(UpdateField.values())
                .filter(f -> f != UpdateField.SAVE)
                .toArray(UpdateField[]::new);

        GenericSelectionPopup.show(
                "Update " + selectedItems.size() + " Test Cases",
                editableFields,
                UpdateField::getLabel,
                field -> field.getShortcutText().charAt(0),
                UpdateField::getIcon,
                selectedField -> {
                    if (selectedField == UpdateField.PRIORITY) {
                        PriorityBulkEditor.show(selectedItems, onUpdate);
                        return;
                    }

                    if (selectedField == UpdateField.TITLE) {
                        TitleBulkEditor.show(selectedItems, onUpdate);
                        return;
                    }

                    if (selectedField == UpdateField.EXPECTED) {
                        ExpectedBulkEditor.show(selectedItems, onUpdate);
                        return;
                    }

                    if (selectedField == UpdateField.STEPS) {
                        StepsBulkEditor.show(selectedItems, onUpdate);
                        return;
                    }

                    System.out.println("Selected: " + selectedField.getLabel() + " (To be implemented)");
                }
        );
    }
}