package org.testin.testrun;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.*;

import java.awt.*;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * The run-creation working dialog: configuration form on top, test case
 * selection tree filling the middle, a visible Create button at the bottom —
 * no Enter shortcut, a working dialog confirms by button; Escape cancels.
 */
public final class RunConfigurationDialog extends AbstractFrameworkDialog<RunConfigurationForm> {

    /**
     * What creating does, and whether it happened. It was a {@code Runnable}
     * while the name was decided before this dialog opened and could not be
     * wrong by the time it got here. The name is typed here now, so creating can
     * be refused - an empty one, or one already taken - and a dialog that closed
     * anyway would take the tester's configuration with it (#9).
     */
    private final @NotNull BooleanSupplier onCreate;
    private final @NotNull SelectionTree selection;

    public RunConfigurationDialog(final @NotNull Project p, final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull BooleanSupplier onCreate) {
        super(p);
        this.onCreate = onCreate;
        this.selection = selection;

        title = "Create Test Run";

        final @NotNull ComponentDialogBase<DialogButton> create = ComponentDialogBase.button("Create");
        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.of(selection),
                create);

        shortcuts = List.of(
                StatusBarShortcut.hint("Tab", "Navigate"),
                StatusBarShortcut.hint("Space", "Check"),
                StatusBarShortcut.cancel(this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(900), JBUI.scale(600));

        // A run without test cases makes no sense - the button follows the
        // checked state live.
        final @NotNull DialogButton createButton = create.getComponent();
        createButton.setEnabled(selection.hasChecked());
        selection.onCheckChanged(() -> createButton.setEnabled(selection.hasChecked()));
    }

    @Override
    protected void submit() {
        if (!selection.hasChecked()) return;

        // Not closed first, deliberately: the callback reads the form and the
        // checked tree to build the run before it hands the writing off, and a
        // closed dialog has no components to read. It returns as soon as it has
        // them, so the dialog still goes on the button (#87).
        //
        // And only when it accepted. A refused name leaves the dialog where it
        // is, with everything the tester typed still in it.
        if (onCreate.getAsBoolean()) closeOk();
    }
}
