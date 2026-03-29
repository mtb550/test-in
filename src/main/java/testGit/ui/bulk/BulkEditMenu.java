package testGit.ui.bulk;

import testGit.pojo.dto.TestCaseDto;
import testGit.ui.GenericSelectionPopup;

import java.util.List;

public class BulkEditMenu {

    public static void show(final List<TestCaseDto> selectedItems, final Runnable onUpdate) {
        GenericSelectionPopup.show(
                "Update " + selectedItems.size() + " Test Cases",
                UpdateField.values(),
                UpdateField::getLabel,
                UpdateField::getShortcut,
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

                    System.out.println("Selected: " + selectedField.getLabel() + " (To be implemented)");
                }
        );
    }
}