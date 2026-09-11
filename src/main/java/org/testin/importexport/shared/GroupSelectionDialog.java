package org.testin.importexport.shared;

import org.testin.testcase.TestEditorAttributes;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Groups;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.SelectionTable;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;

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

    public GroupSelectionDialog(final @NotNull Project p, final @NotNull String currentSelection, final @NotNull Consumer<@NotNull String> onPicked) {
        super(p);
        this.onPicked = onPicked;

        title = Bundle.message("dialog.groups.title");

        final @NotNull ComponentDialogBase<SelectionTable> table = ComponentDialogBase.table()
                .column(TestEditorAttributes.GROUP.getName(), 260)
                .build();

        components = List.of(table);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.hint("Ctrl+Click", Bundle.message("shortcut.add")),
                StatusBarShortcut.cancel(this::closeCancel));

        groups = table.getComponent();

        // No Group first, then every group the project has used. It was the eight
        // constants of an enum; a group is a word now, so the list is what the
        // cache has seen rather than what somebody shipped (#296).
        groups.addRow(Groups.NONE);
        Services.getInstance(p, TestCaseValues.class).getGroups().stream().sorted().forEach(groups::addRow);
        groups.selectRows(rowsOf(currentSelection));
    }

    /**
     * The rows the value arriving from the cell already names. A test case
     * stores its groups as text, so the match is by name - the value the cell
     * holds is the value this dialog produced last time.
     */
    private @NotNull List<Integer> rowsOf(final @NotNull String currentSelection) {
        final @NotNull List<Integer> rows = new ArrayList<>();
        if (currentSelection.isBlank()) return rows;

        final @NotNull List<String> selected = Arrays.stream(currentSelection.split(","))
                .map(String::trim)
                .toList();

        for (int row = 0; row < groups.getRowCount(); row++) {
            if (selected.contains(groups.getValueAt(row, 0))) rows.add(row);
        }

        return rows;
    }

    // UC-SHARE-003
    @Override
    protected void submit() {
        onPicked.accept(selectedGroupsStr());
        closeOk();
    }

    /**
     * The selection as a test case stores it: the group names, comma separated.
     * <p>
     * Through {@link Groups#text}, which is the owner of that line for the cell,
     * the card and the sheet alike. This wrote the join out itself, so the
     * dialog that produces the value and the classes that read it agreed only by
     * having been written the same day (#291).
     */
    private @NotNull String selectedGroupsStr() {
        return Groups.text(groups.getSelectedRows().stream()
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
