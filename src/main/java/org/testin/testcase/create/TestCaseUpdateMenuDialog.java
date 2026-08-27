package org.testin.testcase.create;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.update.UpdateTestCaseDialog;
import org.testin.ui.dialogs.ShortcutMenuPopup;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;
import java.util.function.BiConsumer;

public class TestCaseUpdateMenuDialog {

    private final @NotNull Project p;
    private final @NotNull List<TestCaseDto> items;
    private final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull GenType> updatedItems;

    public TestCaseUpdateMenuDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> items, final @NotNull BiConsumer<@NotNull List<TestCaseDto>, @NotNull GenType> updatedItems) {
        this.p = p;
        this.items = items;
        this.updatedItems = updatedItems;
    }

    /**
     * What follows an accepted update, wherever it was started from: the view
     * panel catches up if it is showing one of the cases, and the automation
     * code is regenerated off the EDT.
     * <p>
     * Every caller of this dialog needs both, and each had written both out, so
     * a change to what an update entails had to be made once per call site.
     */
    public static void applyAftermath(final @NotNull Project p, final @NotNull List<TestCaseDto> updated, final @NotNull GenType gt) {
        ViewToolWindowFactory.panel().ifPresent(viewPanel -> viewPanel.refreshIfShowing(updated));

        // Every case that was updated, not the first of them. This took the
        // list and generated for one element of it, which was invisible while
        // the only caller had a single case and became a bulk edit that wrote
        // all the data and one method's worth of code - with one "Updated"
        // covering both (#151).
        Logger.trace("Generating automation code for " + updated.size() + ": " + gt);

        ApplicationManager.getApplication().executeOnPooledThread(() -> gt.executeAll(p, updated));
    }

    public void show() {
        final boolean isSingle = items.size() == 1;
        final @NotNull String title = isSingle ? "Update Test Case" : "Update " + items.size() + " Test Cases";

        final UpdateTestCaseFields @NotNull[] fields = UpdateTestCaseFields.values();

        new ShortcutMenuPopup<>(p, title, fields,
                UpdateTestCaseFields::getIcon,
                UpdateTestCaseFields::getName,
                UpdateTestCaseFields::getShortcutText,
                UpdateTestCaseFields::bindShortcut,
                selectedItem -> {

                    final @NotNull GenType gt = selectedItem.getGt();
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
