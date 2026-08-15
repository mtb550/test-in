package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.dialogs.FramelessDialogWrapper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class PendingCommitsDialog extends FramelessDialogWrapper {

    private final @NotNull Project p;
    private final @NotNull List<TestCaseDiff> differences;
    private final @NotNull Path repoRoot;
    private final @NotNull List<TestCaseDiff> rowDifferences = new ArrayList<>();

    /**
     * Built by {@link #createCenterPanel()}; null until the dialog has been laid out.
     */
    private @Nullable JBTable table;

    public PendingCommitsDialog(final @NotNull Project p,
                                final @NotNull List<TestCaseDiff> differences,
                                final @NotNull Path repoRoot) {
        super(p);
        this.p = p;
        this.differences = differences;
        this.repoRoot = repoRoot;
        setTitle("Pending Test Case Changes");
        initFrameless();
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        final JBPanel<?> panel = new JBPanel<>(new BorderLayout());

        final String[] columns = {"Test Case ID", "Change Type", "Test Case Description", "Old Value", "New Value"};
        final DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        };

        for (final TestCaseDiff diff : differences) {
            for (final FieldChange fc : diff.fieldChanges()) {
                final String description = getDescriptionForRow(diff, fc);
                model.addRow(new Object[]{
                        diff.testCaseId(),
                        fc.changeType().getLabel(),
                        description,
                        fc.oldValue(),
                        fc.newValue()
                });
                rowDifferences.add(diff);
            }
        }

        final JBTable builtTable = new JBTable(model);
        table = builtTable;
        builtTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        if (builtTable.getRowCount() > 0) builtTable.addRowSelectionInterval(0, builtTable.getRowCount() - 1);
        builtTable.setFillsViewportHeight(true);
        builtTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        builtTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        builtTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        builtTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        builtTable.getColumnModel().getColumn(4).setPreferredWidth(200);

        // --- Context Menu for Rejecting Changes ---
        final JBPopupMenu popupMenu = new JBPopupMenu();
        final JMenuItem rejectItem = new JMenuItem("Reject Specific Change");
        rejectItem.addActionListener(e -> rejectSelectedChange(model));
        popupMenu.add(rejectItem);

        builtTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(final @NotNull MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    final int r = builtTable.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < builtTable.getRowCount()) {
                        builtTable.setRowSelectionInterval(r, r);
                    } else {
                        builtTable.clearSelection();
                    }
                    if (builtTable.getSelectedRow() >= 0) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        panel.add(new JBScrollPane(builtTable), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(900, 400));

        return panel;
    }

    public @NotNull List<TestCaseDiff> getSelectedDifferences() {
        if (table == null || table.getSelectedRowCount() == 0) return List.of();
        final Set<TestCaseDiff> selected = new LinkedHashSet<>();
        for (final int row : table.getSelectedRows()) {
            if (row >= 0 && row < rowDifferences.size()) selected.add(rowDifferences.get(row));
        }
        return List.copyOf(selected);
    }

    private @Nullable String getDescriptionForRow(final @NotNull TestCaseDiff diff, final @NotNull FieldChange fc) {
        if (diff.type() == DiffType.DELETED) {
            final TestCaseDto oldState = diff.oldState();
            return oldState != null ? oldState.getDescription() : fc.oldValue();
        }

        final TestCaseDto newState = diff.newState();
        return newState != null ? newState.getDescription() : fc.newValue();
    }

    private void rejectSelectedChange(final @NotNull DefaultTableModel model) {
        final JBTable selectionSource = table;
        if (selectionSource == null) return;

        final int selectedRow = selectionSource.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= rowDifferences.size()) return;

        // rowDifferences maps rows to diffs exactly; searching by test case id would
        // pick the wrong diff when one test case contributes several change rows.
        final TestCaseDiff diff = rowDifferences.get(selectedRow);

        // The row's change-type label says which field this row reverts; a diff can
        // contribute several rows, so it cannot be read off the diff itself.
        final String changeTypeLabel = (String) model.getValueAt(selectedRow, 1);

        try {
            final Path testSetPath = repoRoot.resolve(diff.relativeFilePath()).getParent();
            if (testSetPath == null) return;

            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            final UUID testCaseId = UUID.fromString(diff.testCaseId());

            if (diff.type() == DiffType.ADDED) {
                indexer.removeTestCase(testSetPath, testCaseId);
                removeRow(selectedRow, model);
            } else if (diff.type() == DiffType.MODIFIED) {
                final TestCaseDto currentDto = indexer.getTestCaseById(testCaseId);
                final TestCaseDto oldDto = diff.oldState();
                if (currentDto == null || oldDto == null) return;

                final ChangeType changeType = ChangeType.fromLabel(changeTypeLabel);
                final RevertAction revertAction = changeType == null ? null : changeType.getRevertAction();
                if (revertAction != null) revertAction.apply(currentDto, oldDto);

                indexer.putTestCase(testSetPath, currentDto);
                removeRow(selectedRow, model);
            } else if (diff.type() == DiffType.DELETED) {
                final TestCaseDto oldDto = diff.oldState();
                if (oldDto == null) return;

                indexer.putTestCase(testSetPath, oldDto);
                removeRow(selectedRow, model);
            }
        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Revert Failed", "Could not revert change: " + ex.getMessage());
        }
    }

    private void removeRow(final int row, final @NotNull DefaultTableModel model) {
        model.removeRow(row);
        if (row >= 0 && row < rowDifferences.size()) rowDifferences.remove(row);
    }
}
