package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileEditorImpl extends UserDataHolderBase implements FileEditor {
    private final JBPanel<?> panel;
    private final VirtualFile file;
    private final JBList<TestCase> list;
    private final CollectionListModel<TestCase> model;
    private final List<TestCase> allTestCases;
    private final JBCheckBox regressionOnlyCb;

    public FileEditorImpl(@NotNull List<TestCase> testCases, @NotNull Directory dir, @NotNull VirtualFile file) {
        // Use a new ArrayList to ensure we have a mutable, stable master list
        this.allTestCases = new ArrayList<>(testCases);
        this.panel = new JBPanel<>(new BorderLayout());
        this.file = file;

        // 1. Create Filter Bar (JetBrains Style)
        JBPanel<?> filterBar = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(15), JBUI.scale(5)));
        filterBar.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));

        regressionOnlyCb = new JBCheckBox("Show Regression only");
        regressionOnlyCb.setOpaque(false);
        regressionOnlyCb.addActionListener(e -> applyFilters());

        filterBar.add(regressionOnlyCb);

        // 2. Setup List
        this.model = new CollectionListModel<>(allTestCases);
        this.list = new JBList<>(model);

        list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
        list.setOpaque(true);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setCellRenderer(new RendererImpl());
        list.addListSelectionListener(new SelectionListenerImpl(list));
        list.addMouseListener(new MouseAdapterImpl(list, model, dir));
        list.setTransferHandler(new TransferImpl(dir, model));
        ShortcutHandler.register(dir, list, model);

        // 3. Assemble
        panel.add(filterBar, BorderLayout.NORTH);
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);
    }

    private void applyFilters() {
        if (regressionOnlyCb.isSelected()) {
            List<TestCase> filtered = allTestCases.stream()
                    .filter(tc -> tc.getGroups() != null &&
                            tc.getGroups().stream().anyMatch(g -> g.name().equalsIgnoreCase("Regression")))
                    .collect(Collectors.toList());
            model.replaceAll(filtered); // Use replaceAll for smoother UI updates
        } else {
            model.replaceAll(allTestCases);
        }
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return list;
    }

    @Override
    public @NotNull String getName() {
        return "Test Case Editor"; // Give your editor a name
    }

    @Override
    public boolean isValid() {
        return true; // CRITICAL: Must be true for the tab to stay open
    }

    // Boilerplate for non-functional methods
    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener l) {
    }

    @Override
    public void dispose() {
    }
}