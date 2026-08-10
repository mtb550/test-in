package org.testin.testCase.createDialog;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.UpdateTestCaseFields;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.testCase.updateDialog.UpdateTestCaseDialog;
import org.testin.ui.dialogs.ShortcutMenuPopup;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public class TestCaseUpdateMenuDialog {

    private final @NotNull Project p;
    private final @NotNull List<TestCaseDto> items;
    private final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull GeneratorType> updatedItems;

    public TestCaseUpdateMenuDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> items, final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull GeneratorType> updatedItems) {
        this.p = p;
        this.items = items;
        this.updatedItems = updatedItems;
    }

    public void show() {
        final boolean isSingle = items.size() == 1;
        final String title = isSingle ? "Update Test Case" : "Update " + items.size() + " Test Cases";

        final UpdateTestCaseFields[] fields = Arrays.stream(UpdateTestCaseFields.values())
                .filter(UpdateTestCaseFields::isUpdateMenuItem)
                .toArray(UpdateTestCaseFields[]::new);

        new ShortcutMenuPopup<>(p, title, fields,
                UpdateTestCaseFields::getIcon,
                UpdateTestCaseFields::getName,
                UpdateTestCaseFields::getShortcutText,
                UpdateTestCaseFields::bindShortcut,
                selectedItem -> {

                    final GeneratorType gt = selectedItem.getGt();
                    Logger.trace("Menu item selected -> " + selectedItem.getName() + " | changeType = " + gt);

                    if (isSingle) {
                        new UpdateTestCaseDialog(p, items.getFirst(), selectedItem, tc -> {
                            Logger.trace("Single Edit Save -> changeType = " + gt);
                            updatedItems.accept(items, gt);
                        }).show();

                    } else {
                        selectedItem.getBulkAction().execute(p, items, list -> {
                            Logger.trace("Bulk Edit Save -> changeType = " + gt);
                            updatedItems.accept(list, gt);
                        });
                    }
                }).show();
    }
}
