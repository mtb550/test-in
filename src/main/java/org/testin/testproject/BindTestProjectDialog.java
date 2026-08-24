package org.testin.testproject;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ProjectStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.SelectionTable;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Map;

/**
 * Asks which test project this automation repository exercises, once, and writes
 * the answer into its {@code testin.yml} (#8).
 * <p>
 * The only screen that lists every test project under the Testin root, and it
 * appears until the repository is bound. What replaced the dropdown is not
 * another dropdown: the pairing is a fact about the repository, so it is chosen
 * once and committed, not re-picked on every machine that opens it.
 * <p>
 * Archived projects are listed with their status rather than hidden. A tester
 * whose project is archived would otherwise look at a list that does not contain
 * the project they know is there, with nothing saying why.
 */
public final class BindTestProjectDialog extends AbstractFrameworkDialog<SelectionTable> {

    private final @NotNull SelectionTable projects;
    private final @NotNull Runnable onBound;

    /**
     * @param underRoot the test projects to choose from, by name - handed in
     *                  rather than read here, because both callers have already
     *                  read the same listing to decide whether to open this at all
     */
    public BindTestProjectDialog(final @NotNull Project p, final @NotNull Map<String, ProjectStatus> underRoot, final @NotNull Runnable onBound) {
        super(p);
        this.onBound = onBound;

        title = "Select Test Project";

        final @NotNull ComponentDialogBase<SelectionTable> table = ComponentDialogBase.table()
                .column("Test Project", 260)
                .column("Status", 100)
                .build();

        components = List.of(table);

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Select", this::submit),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

        projects = table.getComponent();

        for (final Map.Entry<String, ProjectStatus> entry : underRoot.entrySet()) {
            projects.addRow(entry.getKey(), entry.getValue().getLabel());
        }

        selectCurrent();
    }

    /**
     * Opens on the project the repository already names, so a tester who came
     * here to change the binding sees where it stands rather than an empty
     * selection.
     */
    private void selectCurrent() {
        final @NotNull String bound = Services.getInstance(p, BoundTestProject.class).name();

        for (int row = 0; row < projects.getRowCount(); row++) {
            if (projects.getValueAt(row, 0).equals(bound)) {
                projects.selectRows(List.of(row));
                return;
            }
        }
    }

    @Override
    protected void submit() {
        final @NotNull List<Integer> selected = projects.getSelectedRows();
        if (selected.isEmpty()) return;

        final @NotNull String name = projects.getValueAt(selected.getFirst(), 0);

        if (!Services.getInstance(p, BoundTestProject.class).bind(name)) {
            Services.getInstance(p, Notifier.class).error(p, "Not Bound",
                    "testin.yml could not be written, so " + name + " will not be remembered.");
            return;
        }

        closeOk();

        // Announced after the file is written, not before: what the panel draws is
        // read back from the file, so it can only be right once the file says so.
        Services.getInstance(p, Notifier.class).softShow(p, "Bound", name);
        onBound.run();
    }
}
