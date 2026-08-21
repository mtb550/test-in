package org.testin.importexport.shared;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Group;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.SelectionTable;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Picks the groups a test case belongs to, for an import or export cell.
 * <p>
 * On the framework, so the two keys it binds are shown rather than assumed: it
 * used to sit on the frameless wrapper, which put Enter and Escape into the
 * root pane and told the tester about neither (#66).
 * <p>
 * The rows are the groups and the selection is the answer - a group is chosen
 * by being selected, which is what the framework's multi-select table already
 * means everywhere else it is used.
 */
public final class GroupSelectionDialog extends AbstractFrameworkDialog<SelectionTable> {

    private final @NotNull SelectionTable groups;
    private final @NotNull Consumer<@NotNull String> onPicked;

    public GroupSelectionDialog(final @NotNull Project p, final @NotNull String currentSelection,
                                final @NotNull Consumer<@NotNull String> onPicked) {
        super(p);
        this.onPicked = onPicked;

        title = "Select Groups";

        final ComponentDialogBase<SelectionTable> table = ComponentDialogBase.table()
                .column("Group", 260)
                .build();

        components = List.of(table);

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Confirm", this::submit),
                StatusBarShortcut.hint("Ctrl+Click", "Add"),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

        groups = table.getComponent();
        for (final Group group : Group.values()) {
            groups.addRow(group.getName());
        }
        groups.selectRows(rowsOf(currentSelection));
    }

    /**
     * The rows the value arriving from the cell already names. A test case
     * stores its groups as text, so the match is by name - the value the cell
     * holds is the value this dialog produced last time.
     */
    private @NotNull List<Integer> rowsOf(final @NotNull String currentSelection) {
        final List<Integer> rows = new ArrayList<>();
        if (currentSelection.isBlank()) return rows;

        final List<String> selected = Arrays.stream(currentSelection.split(","))
                .map(String::trim)
                .toList();

        for (int row = 0; row < groups.getRowCount(); row++) {
            if (selected.contains(groups.getValueAt(row, 0))) rows.add(row);
        }

        return rows;
    }

    @Override
    protected void submit() {
        onPicked.accept(selectedGroupsStr());
        closeOk();
    }

    /**
     * The selection as a test case stores it: the group names, comma separated.
     */
    private @NotNull String selectedGroupsStr() {
        return String.join(", ", groups.getSelectedRows().stream()
                .map(row -> groups.getValueAt(row, 0))
                .toList());
    }

    /**
     * Runs when the dialog closes, whichever way it closed. The cell editor that
     * opens this has to stop editing either way, and a popup - unlike the modal
     * it replaced - does not return an answer to the line that showed it.
     */
    public void onClosed(final @NotNull Runnable action) {
        getPopup().addListener(new com.intellij.openapi.ui.popup.JBPopupListener() {
            @Override
            public void onClosed(final @NotNull com.intellij.openapi.ui.popup.LightweightWindowEvent event) {
                action.run();
            }
        });
    }
}
