package org.testin.testRun.updateDialog;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.RunItemUpdateFields;
import org.testin.logger.Logger;
import org.testin.mappers.TestRunItems;
import org.testin.ui.dialogs.ShortcutMenuPopup;

import java.util.Arrays;
import java.util.function.Consumer;

public class RunItemUpdateMenuDialog {

    private final @NotNull Project p;
    private final @NotNull TestRunItems runItem;
    private final @NotNull Consumer<TestRunItems> updatedItem;

    public RunItemUpdateMenuDialog(final @NotNull Project p, final @NotNull TestRunItems runItem, final @NotNull Consumer<TestRunItems> updatedItem) {
        this.p = p;
        this.runItem = runItem;
        this.updatedItem = updatedItem;
    }

    public void show() {
        final RunItemUpdateFields[] fields = Arrays.stream(RunItemUpdateFields.values())
                .filter(RunItemUpdateFields::isUpdateMenuItem)
                .toArray(RunItemUpdateFields[]::new);

        new ShortcutMenuPopup<>(p, "Update Test Run Item", fields,
                RunItemUpdateFields::getIcon,
                RunItemUpdateFields::getName,
                RunItemUpdateFields::getShortcutText,
                RunItemUpdateFields::bindShortcut,
                selectedItem -> {
                    Logger.trace("Menu item selected -> " + selectedItem.getName());
                    new UpdateRunItemDialog(p, runItem, selectedItem, updatedItem).show();
                }).show();
    }
}
