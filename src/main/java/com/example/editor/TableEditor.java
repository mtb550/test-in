// src/main/java/com/example/editor/TableEditor.java
package com.example.editor;

import com.example.demo.TestCaseToolWindow;
import com.example.pojo.Feature;
import com.example.pojo.TestCase;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;

/**
 * Displays your TestCase list as draggable, multi‑selectable cards with working context‑menu & double‑click.
 */
public class TableEditor extends UserDataHolderBase implements FileEditor {
    private final JPanel panel;

    public TableEditor(@NotNull Feature feature) {
        panel = new JBPanel<>(new BorderLayout());

        // 1) Build a sorted, mutable list model
        DefaultListModel<TestCase> model = new DefaultListModel<>();
        feature.getTestCases().stream()
                .sorted(Comparator.comparingInt(TestCase::getOrder))
                .forEach(model::addElement);

        // 2) JList with multi‑select and drag‑to‑reorder
        JBList<TestCase> list = new JBList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new ListItemReorderHandler(model));

        // 3) Render each TestCase as a TestCaseCard
        list.setCellRenderer((JList<? extends TestCase> l,
                              TestCase tc,
                              int index,
                              boolean isSelected,
                              boolean cellHasFocus) -> {
            TestCaseCard card = new TestCaseCard(index, tc);
            if (isSelected) {
                card.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
            }
            return card;
        });

        // 4) Global mouse listener for context‑menu & double‑click
        list.addMouseListener(new MouseAdapter() {
            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;
                if (!list.isSelectedIndex(idx)) {
                    list.setSelectedIndex(idx);
                }
                TestCase tc = model.getElementAt(idx);
                JPopupMenu menu = TableContextMenu.create(list, model, tc);
                menu.show(list, e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2
                        && SwingUtilities.isLeftMouseButton(e)) {
                    int idx = list.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        TestCaseToolWindow.show(model.getElementAt(idx));
                    }
                }
            }
        });

        // 5) Wrap in scroll pane
        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);
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
