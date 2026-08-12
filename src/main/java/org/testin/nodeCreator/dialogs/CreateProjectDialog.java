package org.testin.nodeCreator.dialogs;

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
 * Creates a test project — new, or imported by cloning a Git URL. The
 * constructor is the declaration: title, components, status bar mapping —
 * plus the submit action.
 */
public final class CreateProjectDialog extends AbstractFrameworkDialog<TextFieldWithSelections<DirectoryType>> {

    private final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onCreate;

    public CreateProjectDialog(final @NotNull Project p, final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onCreate) {
        super(p);
        this.onCreate = onCreate;

        title = "Create Project";

        components = List.of(
                ComponentDialogBase.<DirectoryType>textFieldWithSelections()
                        .icon(DirectoryType.TP.getIcon())
                        .placeholder("set name or paste url..")
                        .selection(DirectoryType.TP.getIcon(), "Test Project", "Creates an empty test project", DirectoryType.TP)
                        .selection(DirectoryType.IMPORT_TP.getIcon(), "Import Project (Git)", "Clones from a repository URL", DirectoryType.IMPORT_TP)
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Confirm", this::submit),
                StatusBarShortcut.hint("↑ ↓", "Select"),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));
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
