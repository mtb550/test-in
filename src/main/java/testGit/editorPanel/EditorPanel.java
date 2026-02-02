package testGit.editorPanel;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.TestCase;
import testGit.viewPanel.TestCaseToolWindow;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.List;

public class EditorPanel extends UserDataHolderBase implements FileEditor {
    private final JBPanel<?> panel;
    private final VirtualFile file;

    public EditorPanel(@NotNull List<TestCase> testCases, @NotNull String featurePath, @NotNull VirtualFile file) {
        this.file = file;
        this.panel = new JBPanel<>(new BorderLayout());

        // 1) Build model using CollectionListModel (IntelliJ Native)
        CollectionListModel<TestCase> model = new CollectionListModel<>(testCases);

        // 2) Initialize JBList with Multi-Selection support
        JBList<TestCase> list = new JBList<>(model);

        // This is critical: it makes the list background clickable even when empty
        list.getEmptyText().setText("No test cases.");
        list.getEmptyText().appendLine("Create your first test case.");
        list.getEmptyText().appendLine("Ctrl + M");

        // Ensure the list component fills the viewport so it can catch mouse events
        list.setOpaque(true);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new ListItemReorderHandler(model));
        list.addMouseListener(new MouseAdapter(list, model, featurePath));
        ShortcutHandler.register(featurePath, list, model);

        // 3) Auto-update details window on selection change
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                TestCase selected = list.getSelectedValue();
                if (selected != null) {
                    TestCaseToolWindow.show(selected);
                }
            }
        });

        // 4) Cell Renderer: Modern Selection Border
        list.setCellRenderer((l, tc, index, isSelected, cellHasFocus) -> {
            TestCaseCard card = new TestCaseCard(index, tc);
            if (isSelected) {
                // JBColor.link() or JBColor.namedColor provides a native feel across themes
                card.setBorder(JBUI.Borders.customLine(JBColor.blue, 1));
            } else {
                card.setBorder(JBUI.Borders.empty(1));
            }
            return card;
        });


        // 6) Wrap in ScrollPane with no border for a flat New UI look
        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setBorder(JBUI.Borders.empty());
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
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener l) {
    }
}