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

        String[] columns = {"Test Case ID", "Change Type", "Field", "Old Value", "New Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (TestCaseDiff diff : differences) {
            for (TestCaseDiff.FieldChange fc : diff.fieldChanges()) {
                model.addRow(new Object[]{
                        diff.testCaseId(),
                        diff.type().name(),
                        fc.fieldName(),
                        fc.oldValue(),
                        fc.newValue()
                });
            }
        }

        JBTable table = new JBTable(model);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(250);
        table.getColumnModel().getColumn(4).setPreferredWidth(250);

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
        panel.setPreferredSize(new Dimension(800, 400));

        return panel;
    }

    private void rejectSelectedChange(JBTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) return;

        String testCaseId = (String) model.getValueAt(selectedRow, 0);
        String fieldName = (String) model.getValueAt(selectedRow, 2);

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

                switch (fieldName) {
                    case "Description" -> currentDto.setDescription(oldDto.getDescription());

                    case "Expected Result" -> currentDto.setExpectedResult(oldDto.getExpectedResult());

                    case "Priority" -> currentDto.setPriority(oldDto.getPriority());
                }

                Services.getInstance(project, ProjectIndexer.class).putTestCase(jsonFile.getParentFile().toPath(), currentDto);
                model.removeRow(selectedRow);
            }
        } catch (Exception ex) {
            Services.getInstance(project, Notifier.class).error(project, "Revert Failed", "Could not revert change: " + ex.getMessage());
        }
    }
}