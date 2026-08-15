package org.testin.creator.dialogs;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.function.Consumer;

/**
 * Creates a test project — new, or imported by cloning a Git URL.
 * <p>
 * One field, no choice to make: what was typed says which it is. A repository
 * URL is clonable and a project name is not, so asking the tester to also pick
 * from a list was asking them to repeat themselves — and to be wrong. The
 * placeholder already promised this ("set name or paste url..") long before the
 * dialog behaved that way.
 */
public final class CreateProjectDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull Consumer<@NotNull String> onCreate;

    public CreateProjectDialog(final @NotNull Project p, final @NotNull Consumer<@NotNull String> onCreate) {
        super(p);
        this.onCreate = onCreate;

        title = "Create Project";

        components = List.of(
                ComponentDialogBase.textField()
                        .icon(DirectoryType.TP.getIcon())
                        .placeholder("set name or paste url..")
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Confirm", this::submit),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));
    }

    @Override
    protected void submit() {
        final String name = component().getText().trim();
        if (name.isEmpty()) {
            component().showEmptyWarning();
            return;
        }

        onCreate.accept(name);
        closeOk();
    }
}
