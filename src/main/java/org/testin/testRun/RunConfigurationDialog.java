package org.testin.testRun;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.*;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.util.List;

/**
 * The run-creation working dialog: configuration form on top, test case
 * selection tree filling the middle, a visible Create button at the bottom —
 * no Enter shortcut, a working dialog confirms by button; Escape cancels.
 */
public final class RunConfigurationDialog extends AbstractFrameworkDialog<RunConfigurationForm> {

    private final @NotNull Runnable onCreate;
    private final @NotNull SelectionTree selection;

    public RunConfigurationDialog(final @NotNull Project p, final @NotNull RunConfigurationForm form,
                                  final @NotNull SelectionTree selection, final @NotNull Runnable onCreate) {
        super(p);
        this.onCreate = onCreate;
        this.selection = selection;

        title = "Create Test Run";

        final ComponentDialogBase<DialogButton> create = ComponentDialogBase.button("Create");
        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.of(selection),
                create);

        shortcuts = List.of(
                StatusBarShortcut.hint("Space", "Check"),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(900), JBUI.scale(600));

        // A run without test cases makes no sense - the button follows the
        // checked state live.
        final DialogButton createButton = create.getComponent();
        createButton.setEnabled(selection.hasChecked());
        selection.onCheckChanged(() -> createButton.setEnabled(selection.hasChecked()));
    }

    @Override
    protected void submit() {
        if (!selection.hasChecked()) return;

        onCreate.run();
        closeOk();
    }
}
