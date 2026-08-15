package org.testin.creator.dialogs;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextFieldWithSelections;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Creates a test node (test set or test set package) under the test cases
 * main directory or a test set package. The constructor is the declaration:
 * title, components, status bar mapping — plus the submit action.
 */
public final class CreateTestDialog extends AbstractFrameworkDialog<TextFieldWithSelections<DirectoryType>> {

    private final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onCreate;

    public CreateTestDialog(final @NotNull Project p, final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onCreate) {
        super(p);
        this.onCreate = onCreate;

        title = "Create Test Node";

        components = List.of(
                ComponentDialogBase.<DirectoryType>textFieldWithSelections()
                        .icon(DirectoryType.TS.getIcon())
                        .placeholder("set name..")
                        .selection(DirectoryType.TS.getIcon(), "Test Set", "Holds test cases", DirectoryType.TS)
                        .selection(DirectoryType.TSP.getIcon(), "Test Set Package", "Groups test sets", DirectoryType.TSP)
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Confirm", this::submit),
                StatusBarShortcut.hint("↑ ↓", "Select"),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel)
        );
    }

    @Override
    protected void submit() {
        final String name = component().getText().trim();
        if (name.isEmpty()) {
            component().showEmptyWarning();
            return;
        }

        onCreate.accept(name, component().getSelectedValue());
        closeOk();
    }
}
