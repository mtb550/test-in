package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.application.ApplicationManager;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.update.UpdateTestCaseDialog;
import org.testin.ui.dialogs.ShortcutMenuPopup;
import org.testin.view.ViewPanel;
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
    public static void applyAftermath(final @NotNull Project p, final @NotNull List<TestCaseDto> updated,
                                      final @NotNull GenType gt) {
        final ViewPanel viewPanel = ViewToolWindowFactory.getViewPanel();
        if (viewPanel != null) viewPanel.refreshIfShowing(updated);

        Logger.trace("Generating automation code: " + gt);
        final GenAction action = gt.getAction();
        final TestCaseDto first = updated.getFirst();

        ApplicationManager.getApplication().executeOnPooledThread(() -> action.execute(p, first));
    }

    public void show() {
        final boolean isSingle = items.size() == 1;
        final String title = isSingle ? "Update Test Case" : "Update " + items.size() + " Test Cases";

        final UpdateTestCaseFields[] fields = UpdateTestCaseFields.values();

        new ShortcutMenuPopup<>(p, title, fields,
                UpdateTestCaseFields::getIcon,
                UpdateTestCaseFields::getName,
                UpdateTestCaseFields::getShortcutText,
                UpdateTestCaseFields::bindShortcut,
                selectedItem -> {

                    final GenType gt = selectedItem.getGt();
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
