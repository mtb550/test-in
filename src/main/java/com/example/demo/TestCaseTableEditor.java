package com.example.demo;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

public class TestCaseTableEditor extends UserDataHolderBase implements FileEditor {
    private final JPanel panel;

    public TestCaseTableEditor(Feature feature) {
        panel = new JPanel(new BorderLayout());
        TestCaseTableModel model = new TestCaseTableModel(feature.getTestCases());
        JBTable table = new JBTable(model);

        // Add toolbar
        JToolBar toolBar = new JToolBar();
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        JButton clearButton = new JButton("Clear");

        toolBar.add(new JLabel("Search: "));
        toolBar.add(searchField);
        toolBar.add(searchButton);
        toolBar.add(clearButton);

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(new JBScrollPane(table), BorderLayout.CENTER);

        // Search functionality
        searchButton.addActionListener(e -> {
            String query = searchField.getText().toLowerCase();
            table.clearSelection();
            for (int i = 0; i < model.getRowCount(); i++) {
                boolean match = model.getValueAt(i, 0).toString().toLowerCase().contains(query);
                if (match) {
                    table.addRowSelectionInterval(i, i);
                }
            }
        });

        clearButton.addActionListener(e -> {
            searchField.setText("");
            table.clearSelection();
        });

        // Double-click event
        table.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        TestCase tc = model.getTestCaseAt(row);
                        TestCaseToolWindow.show(tc);
                    }
                }
            }
        });
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return panel;
    }

    @Override
    public @NotNull String getName() {
        return "Test Case Table";
    }

    @Override
    public void dispose() {}

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}

    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {}

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
