package testGit.ui.bulk;

import testGit.pojo.dto.TestCaseDto;
import testGit.ui.GenericSelectionPopup;

import java.util.List;

public class BulkEditMenu {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
        GenericSelectionPopup.show(
                "Update " + selectedItems.size() + " Test Cases",
                UpdateField.values(),
                UpdateField::getLabel,
                UpdateField::getShortcut,
                item -> null,
                selectedField -> {
                    if (selectedField == UpdateField.PRIORITY) {
                        PriorityBulkEditor.show(selectedItems, onUpdate);

                    } else if (selectedField == UpdateField.TITLE) {
                        TitleBulkEditor.show(selectedItems, onUpdate);

                    } else {
                        System.out.println("Selected: " + selectedField.getLabel() + " (To be implemented)");
                    }
                }
        );
    }
}