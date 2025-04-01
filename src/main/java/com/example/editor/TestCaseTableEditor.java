package com.example.editor;

import com.example.demo.TestCaseToolWindow;
import com.example.pojo.Feature;
import com.example.pojo.TestCase;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.components.JBComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

public class TestCaseTableEditor extends UserDataHolderBase implements FileEditor {
    private final JPanel panel;

    public TestCaseTableEditor(Feature feature) {
        panel = new JBPanel(new BorderLayout());

        TestCaseTableModel model = new TestCaseTableModel(feature.getTestCases());
        JBTable table = new JBTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.setShowGrid(true);

        new TestCaseTableContextMenu(table, model);
        new TableSpeedSearch(table);

        TableCellRenderer wrapRenderer = new WrapTextCellRenderer();
        for (int i = 0; i < model.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(wrapRenderer);
        }

        JBScrollPane scrollPane = new JBScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Double-click to show details in TestCaseDetails tool window
        table.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        TestCase tc = model.getTestCaseAt(row);
                        TestCaseToolWindow.show(tc); // This opens the side panel
                    }
                }
            }
        });

    }

    static class WrapTextCellRenderer extends JTextArea implements TableCellRenderer {
        public WrapTextCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText(value == null ? "" : value.toString());
            setFont(table.getFont());

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            table.setRowHeight(row, getPreferredSize().height + 10);
            return this;
        }
    }

    // FileEditor interface methods
    @Override public @NotNull JComponent getComponent() { return panel; }
    @Override public @Nullable JComponent getPreferredFocusedComponent() { return panel; }
    @Override public @NotNull String getName() { return "Test Case Table"; }
    @Override public void dispose() {}
    @Override public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}
    @Override public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}
    @Override public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return FileEditorState.INSTANCE;
    }
    @Override public void setState(@NotNull FileEditorState state) {}
    @Override public boolean isModified() { return false; }
    @Override public boolean isValid() { return true; }
}
