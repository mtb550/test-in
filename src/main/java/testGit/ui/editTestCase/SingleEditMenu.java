package testGit.ui.editTestCase;

import testGit.pojo.dto.TestCaseDto;
import testGit.ui.GenericSelectionPopup;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

public class SingleEditMenu {

    public static void show(final TestCaseDto existingDto, final Consumer<TestCaseDto> onUpdate, Set<String> uniqueStepsCache) {
        UpdateField[] editableFields = Arrays.stream(UpdateField.values())
                .filter(f -> f != UpdateField.SAVE)
                .toArray(UpdateField[]::new);

        GenericSelectionPopup.show(
                "Edit Test Case",
                editableFields,
                UpdateField::getLabel,
                field -> field.getShortcutText().charAt(0),
                UpdateField::getIcon,
                selectedField -> new UpdateTestCaseUI().show(existingDto, selectedField, onUpdate, uniqueStepsCache));
    }
}