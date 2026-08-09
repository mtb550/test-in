package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class PendingCommitsDialog extends DialogWrapper {

    private final @NotNull Project p;
    private final List<TestCaseDiff> differences;
    private final Path repoRoot;

    public PendingCommitsDialog(@NotNull Project p, List<TestCaseDiff> differences, Path repoRoot) {
        super(p, true);
        this.p = p;
        this.differences = differences;
        this.repoRoot = repoRoot;
        setTitle("Pending Test Case Changes");
        setOKButtonText("Commit Changes");
        setCancelButtonText("Cancel");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());

        String[] columns = {"Test Case ID", "Change Type", "Test Case Description", "Old Value", "New Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (TestCaseDiff diff : differences) {
            for (TestCaseDiff.FieldChange fc : diff.fieldChanges()) {
                String description = getDescriptionForRow(diff, fc);
                model.addRow(new Object[]{
                        diff.testCaseId(),
                        fc.changeType().getLabel(),
                        description,
                        fc.oldValue(),
                        fc.newValue()
                });
            }
        }

        JBTable table = new JBTable(model);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);

        // --- Context Menu for Rejecting Changes ---
        JBPopupMenu popupMenu = new JBPopupMenu();
        JMenuItem rejectItem = new JMenuItem("Reject Specific Change");
        rejectItem.addActionListener(e -> rejectSelectedChange(table, model));
        popupMenu.add(rejectItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int r = table.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < table.getRowCount()) {
                        table.setRowSelectionInterval(r, r);
                    } else {
                        table.clearSelection();
                    }
                    if (table.getSelectedRow() >= 0) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        panel.add(new JBScrollPane(table), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(900, 400));

        return panel;
    }

    private String getDescriptionForRow(TestCaseDiff diff, TestCaseDiff.FieldChange fc) {
        if (diff.type() == DiffType.ADDED) {
            TestCaseDto newState = diff.newState();
            return newState != null ? newState.getDescription() : fc.newValue();

        } else if (diff.type() == DiffType.DELETED) {
            TestCaseDto oldState = diff.oldState();
            return oldState != null ? oldState.getDescription() : fc.oldValue();

        } else {
            TestCaseDto newState = diff.newState();
            return newState != null ? newState.getDescription() : fc.newValue();
        }
    }

    private void rejectSelectedChange(JBTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) return;

        String testCaseId = (String) model.getValueAt(selectedRow, 0);
        String changeTypeLabel = (String) model.getValueAt(selectedRow, 1);

        TestCaseDiff diff = differences.stream()
                .filter(d -> d.testCaseId().equals(testCaseId))
                .findFirst().orElse(null);

        if (diff == null) return;

        try {
            File jsonFile = repoRoot.resolve(diff.relativeFilePath()).toFile();

            if (diff.type() == DiffType.ADDED) {
                if (jsonFile.exists() && jsonFile.delete()) {
                    model.removeRow(selectedRow);
                }
            } else if (diff.type() == DiffType.MODIFIED) {
                final TestCaseDto currentDto = Services.getInstance(p, ProjectIndexer.class).getTestCaseById(UUID.fromString(testCaseId));
                final TestCaseDto oldDto = diff.oldState();

                if (currentDto == null)
                    return;

                final ChangeType changeType = ChangeType.fromLabel(changeTypeLabel);
                if (changeType != null && changeType.getRevertAction() != null)
                    changeType.getRevertAction().apply(currentDto, oldDto);

                Services.getInstance(p, ProjectIndexer.class).putTestCase(jsonFile.getParentFile().toPath(), currentDto);
                model.removeRow(selectedRow);
            }
        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Revert Failed", "Could not revert change: " + ex.getMessage());
        }
    }
}
