package org.testin.rename;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;

import java.util.List;
import java.util.function.Consumer;

/**
 * Renames a tree node. The constructor is the declaration: title, component,
 * status bar mapping — plus the submit action.
 */
final class RenameDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull Consumer<@NotNull String> onSubmit;

    RenameDialog(final @NotNull Project p, final @NotNull String currentName, final @NotNull Consumer<@NotNull String> onSubmit) {
        super(p);
        this.onSubmit = onSubmit;

        title = "Rename";

        components = List.of(
                ComponentDialogBase.textField()
                        .icon(AllIcons.Actions.Edit)
                        .placeholder("set new name...")
                        .value(currentName)
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    @Override
    protected void submit() {
        final @NotNull String value = component().getText().trim();
        if (value.isEmpty()) {
            component().showEmptyWarning();
            return;
        }

        onSubmit.accept(value);
        closeOk();
    }
}
