package com.example.editor;

import com.example.pojo.Feature;
import com.example.pojo.TestCase;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TableEditor extends UserDataHolderBase implements FileEditor {
    private final JPanel panel;
    private final JPanel listPanel;
    private final List<TestCase> testCases;

    public TableEditor(@NotNull Feature feature) {
        panel = new JBPanel<>(new BorderLayout());
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(30, 30, 30));

        testCases = feature.getTestCases()
                .stream()
                .sorted(Comparator.comparingInt(TestCase::getOrder))
                .collect(Collectors.toList());

        refreshTestCases();

        JBScrollPane scrollPane = new JBScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    private void refreshTestCases() {
        listPanel.removeAll();
        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            TestCaseCard card = new TestCaseCard(i, tc);
            new TableContextMenu(card, tc, testCases);
            listPanel.add(card);
            listPanel.add(Box.createVerticalStrut(8));
        }
        listPanel.revalidate();
        listPanel.repaint();
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
        return "Test Case Cards";
    }

    @Override
    public void dispose() {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }


}
