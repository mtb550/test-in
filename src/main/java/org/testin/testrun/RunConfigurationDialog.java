package org.testin.testrun;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.*;

import java.awt.*;
import java.util.List;

/**
 * The run working dialog: configuration form on top, test case selection tree
 * filling the middle, a visible button at the bottom - no Enter shortcut, a
 * working dialog confirms by button; Escape cancels.
 * <p>
 * What that button says, and what it does, comes from the {@link RunFormAction}
 * it is opened with: creating a run and editing one are this dialog with two
 * different answers to that (#96).
 */
public final class RunConfigurationDialog extends AbstractFrameworkDialog<RunConfigurationForm> {

    private final @NotNull RunFormAction action;
    private final @NotNull RunConfigurationForm form;
    private final @NotNull SelectionTree selection;

    public RunConfigurationDialog(final @NotNull Project p, final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull RunFormAction action) {
        super(p);
        this.action = action;
        this.form = form;
        this.selection = selection;

        title = action.title();

        final @NotNull ComponentDialogBase<DialogButton> confirm = ComponentDialogBase.button(action.button());
        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.of(selection),
                confirm);

        shortcuts = List.of(
                StatusBarShortcut.hint("Tab", "Navigate"),
                StatusBarShortcut.hint("Space", "Check"),
                StatusBarShortcut.cancel(this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(900), JBUI.scale(600));

        // A run without test cases makes no sense - the button follows the
        // checked state live. Editing a run down to nothing is refused the same
        // way creating an empty one is: the button simply goes dead.
        final @NotNull DialogButton confirmButton = confirm.getComponent();
        confirmButton.setEnabled(selection.hasChecked());
        selection.onCheckChanged(() -> confirmButton.setEnabled(selection.hasChecked()));
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
        if (action.submit().of(form, selection)) closeOk();
    }
}
