package org.testin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class PendingCommitsDialog extends DialogWrapper {

    private final Project project;
    private final List<TestCaseDiff> differences;
    private final Path repoRoot;

    public PendingCommitsDialog(@Nullable Project project, List<TestCaseDiff> differences, Path repoRoot) {
        super(project, true);
        this.project = project;
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
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Test Case ID", "Change Type", "Test Case Description", "Old Value", "New Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (TestCaseDiff diff : differences) {
            for (TestCaseDiff.FieldChange fc : diff.fieldChanges()) {
                String changeTypeLabel = getChangeTypeLabel(fc.changeType());
                String description = getDescriptionForRow(diff, fc);
                model.addRow(new Object[]{
                        diff.testCaseId(),
                        changeTypeLabel,
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
        JPopupMenu popupMenu = new JPopupMenu();
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

    private String getChangeTypeLabel(TestCaseDiff.ChangeType changeType) {
        return switch (changeType) {
            case CREATE_TEST_CASE -> "Create Test Case";
            case REMOVE_TEST_CASE -> "Remove Test Case";
            case CHANGE_DESCRIPTION -> "Change Description";
            case CHANGE_EXPECTED_RESULT -> "Change Expected Result";
            case CHANGE_PRIORITY -> "Change Priority";
            case CHANGE_GROUP -> "Change Group";
        };
    }

    private String getDescriptionForRow(TestCaseDiff diff, TestCaseDiff.FieldChange fc) {
        if (diff.type() == TestCaseDiff.DiffType.ADDED) {
            TestCaseDto newState = diff.newState();
            return newState != null ? newState.getDescription() : fc.newValue();
        } else if (diff.type() == TestCaseDiff.DiffType.DELETED) {
            TestCaseDto oldState = diff.oldState();
            return oldState != null ? oldState.getDescription() : fc.oldValue();
        } else {
            // MODIFIED: show the current (new) description from the DTO
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

            if (diff.type() == TestCaseDiff.DiffType.ADDED) {
                if (jsonFile.exists() && jsonFile.delete()) {
                    model.removeRow(selectedRow);
                }
            } else if (diff.type() == TestCaseDiff.DiffType.MODIFIED) {
                TestCaseDto currentDto = Services.getInstance(project, Mapper.class).readValue(jsonFile, TestCaseDto.class);

                if (currentDto == null) return;

                TestCaseDto oldDto = diff.oldState();

                // Determine which field to revert based on the change type label
                if (changeTypeLabel.contains("Description")) {
                    currentDto.setDescription(oldDto.getDescription());
                } else if (changeTypeLabel.contains("Expected Result")) {
                    currentDto.setExpectedResult(oldDto.getExpectedResult());
                } else if (changeTypeLabel.contains("Priority")) {
                    currentDto.setPriority(oldDto.getPriority());
                } else if (changeTypeLabel.contains("Group")) {
                    currentDto.setGroup(oldDto.getGroup());
                }

                Services.getInstance(project, ProjectIndexer.class).putTestCase(jsonFile.getParentFile().toPath(), currentDto);
                model.removeRow(selectedRow);
            }
        } catch (Exception ex) {
            Services.getInstance(project, Notifier.class).error(project, "Revert Failed", "Could not revert change: " + ex.getMessage());
        }
    }
}
