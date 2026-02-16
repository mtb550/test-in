package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
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
import testGit.pojo.Directory;
import testGit.pojo.GroupType;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileEditorImpl extends UserDataHolderBase implements FileEditor {
    private final JBPanel<?> panel;
    private final VirtualFile file;
    private final JBList<TestCase> list;
    private final CollectionListModel<TestCase> model;
    private final List<TestCase> allTestCases;
    private final Set<GroupType> selectedGroups = new HashSet<>();
    private final JButton groupButton;
    private final ModelSyncListener<TestCase> syncListener;

    public FileEditorImpl(@NotNull List<TestCase> testCases, @NotNull Directory dir, @NotNull VirtualFile file) {
        this.allTestCases = new ArrayList<>(testCases);
        this.panel = new JBPanel<>(new BorderLayout());
        this.file = file;

        JBPanel<?> filterBar = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(15), JBUI.scale(5)));
        filterBar.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));

        filterBar.add(new JLabel("Filter by Groups:"));

        groupButton = new JButton("Groups ▼");
        groupButton.addActionListener(e -> showGroupPopup(groupButton));
        filterBar.add(groupButton);

        this.model = new CollectionListModel<>(new ArrayList<>(allTestCases));
        this.syncListener = new ModelSyncListener<>(allTestCases, model);
        this.syncListener.setOnUpdate(() -> {

            if (!selectedGroups.isEmpty()) {
                selectedGroups.clear();
                applyFilters();
                groupButton.setText("Groups ▼");
            }
        });

        this.model.addListDataListener(syncListener);

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

        panel.add(filterBar, BorderLayout.NORTH);
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);
    }

    private void showGroupPopup(JButton anchor) {
        JBList<GroupType> groupList = new JBList<>(GroupType.values());

        groupList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JCheckBox checkBox = new JCheckBox(value.name(), selectedGroups.contains(value));
            checkBox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            checkBox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            checkBox.setOpaque(true);
            return checkBox;
        });

        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = groupList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    GroupType group = groupList.getModel().getElementAt(index);
                    if (selectedGroups.contains(group)) selectedGroups.remove(group);
                    else selectedGroups.add(group);

                    groupList.repaint();
                    applyFilters();
                }
            }
        });

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(groupList), null)
                .setTitle("Select Groups")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .createPopup();

        popup.showUnderneathOf(anchor);
    }

    private void applyFilters() {
        syncListener.pause();

        try {
            if (selectedGroups.isEmpty()) {
                model.replaceAll(allTestCases);
                groupButton.setText("Groups ▼");
            } else {
                List<TestCase> filtered = allTestCases.stream()
                        .filter(tc -> tc.getGroups() != null &&
                                tc.getGroups().stream().anyMatch(selectedGroups::contains))
                        .collect(Collectors.toList());
                model.replaceAll(filtered);
                groupButton.setText("Groups (" + selectedGroups.size() + ") ▼");
            }
        } finally {
            syncListener.resume();
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
        return "Test Case Editor";
    }

    @Override
    public boolean isValid() {
        return true;
    }

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