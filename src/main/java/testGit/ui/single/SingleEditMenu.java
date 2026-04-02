package testGit.ui.single;

import testGit.pojo.dto.TestCaseDto;
import testGit.ui.GenericSelectionPopup;
import testGit.ui.bulk.UpdateField;

import java.util.Set;
import java.util.function.Consumer;

public class SingleEditMenu {

    public static void show(final TestCaseDto existingDto, final Consumer<TestCaseDto> onUpdate, Set<String> uniqueStepsCache) {
        GenericSelectionPopup.show(
                "Edit Test Case",
                UpdateField.values(),
                UpdateField::getLabel,
                UpdateField::getShortcut,
                UpdateField::getIcon,
                selectedField -> SingleTestCaseEditor.showForEdit(existingDto, selectedField, onUpdate, uniqueStepsCache)
        );
    }
}