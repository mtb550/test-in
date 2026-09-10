package org.testin.creator.dialogs;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextFieldWithSelections;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Creates a run node (test run or test run package) under the test runs main
 * directory or a test run package. The constructor is the declaration:
 * title, components, status bar mapping — plus the submit action.
 */
public final class CreateRunDialog extends AbstractFrameworkDialog<TextFieldWithSelections<DirectoryType>> {

    private final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onCreate;

    // UC-TREE-PANEL-009, UC-TREE-PANEL-010, Rule-TREE-PANEL-032
    public CreateRunDialog(final @NotNull Project p, final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onCreate) {
        super(p);
        this.onCreate = onCreate;

        title = "Create Run Node";

        components = List.of(
                ComponentDialogBase.<DirectoryType>textFieldWithSelections()
                        .icon(DirectoryType.TR.getIcon())
                        .placeholder("set name, like Sprint 3 Cycle 1...")
                        .selection(DirectoryType.TR.getIcon(), DirectoryType.TR.getDescription(), "Records execution results", DirectoryType.TR)
                        .selection(DirectoryType.TRP.getIcon(), DirectoryType.TRP.getDescription(), "Groups test runs", DirectoryType.TRP)
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.select(),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    // UC-TREE-PANEL-009, UC-TREE-PANEL-010, Rule-TREE-PANEL-005, Rule-TREE-PANEL-095
    @Override
    protected void submit() {
        // The same question the test side asks, and the run types answer yes to
        // every name - they generate no code. Asked anyway, so a run node is not
        // the one dialog that knows a rule instead of asking for it.
        final @NotNull DirectoryType type = component().getSelectedValue();

        final @NotNull String name = accepted(component(), type::canTakeName, Refused.NOT_A_JAVA_NAME);
        if (name.isEmpty()) return;

        onCreate.accept(name, type);
        closeOk();
    }
}
