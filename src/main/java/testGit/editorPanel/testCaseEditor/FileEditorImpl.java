package testGit.editorPanel.testCaseEditor;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
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
    @Getter
    private final Set<String> selectedDetails = new HashSet<>();
    private final JButton groupButton;
    private final JButton detailsButton;
    private final ModelSyncListener<TestCase> syncListener;
    private final SearchTextField searchField = new SearchTextField();
    private final Footer footer;
    @Getter
    private boolean showGroups;
    @Getter
    private boolean showPriority;
    private String currentSearchQuery = "";

    public FileEditorImpl(@NotNull List<TestCase> testCases, @NotNull Directory dir, @NotNull VirtualFile file) {
        this.allTestCases = new ArrayList<>(testCases);
        this.panel = new JBPanel<>(new BorderLayout());
        this.file = file;

        PropertiesComponent props = PropertiesComponent.getInstance();
        this.showGroups = props.getBoolean("testGit.showGroups", true);
        this.showPriority = props.getBoolean("testGit.showPriority", true);
        String savedDetails = props.getValue("testGit.selectedDetails", "ID,Module,Expected Result,Steps,Automation Ref,Business Ref");
        if (!savedDetails.isEmpty()) {
            selectedDetails.addAll(List.of(savedDetails.split(",")));
        }

        JBPanel<?> header = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(5), JBUI.scale(2)));
        header.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));
        header.setBackground(JBUI.CurrentTheme.EditorTabs.background());

        groupButton = new JButton("Groups", AllIcons.Actions.GroupBy);
        groupButton.setFocusable(false);
        groupButton.setBorderPainted(false);
        groupButton.setContentAreaFilled(false);
        groupButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        groupButton.setFont(JBUI.Fonts.label(12f));
        groupButton.addActionListener(e -> showGroupPopup(groupButton));

        groupButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                groupButton.setContentAreaFilled(true);
                groupButton.setBackground(JBUI.CurrentTheme.ActionButton.hoverBackground());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                groupButton.setContentAreaFilled(false);
            }
        });

        header.add(groupButton);

        detailsButton = new JButton("Details", AllIcons.Actions.PreviewDetailsVertically);
        detailsButton.setFocusable(false);
        detailsButton.setBorderPainted(false);
        detailsButton.setContentAreaFilled(false);
        detailsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        detailsButton.setFont(JBUI.Fonts.label(12f));
        detailsButton.addActionListener(e -> showDetailsPopup(detailsButton));

        detailsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                detailsButton.setContentAreaFilled(true);
                detailsButton.setBackground(JBUI.CurrentTheme.ActionButton.hoverBackground());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                detailsButton.setContentAreaFilled(false);
            }
        });

        header.add(detailsButton);

        this.model = new CollectionListModel<>(new ArrayList<>(allTestCases));
        this.syncListener = new ModelSyncListener<>(allTestCases, model);

        this.syncListener.setOnUpdate(() -> {

            if (!selectedGroups.isEmpty()) {
                selectedGroups.clear();
                applyGroupFilters();
            }

            if (!selectedDetails.isEmpty()) {
                selectedDetails.clear();
                updateDetailsButtonState();
            }
        });

        this.model.addListDataListener(syncListener);

        searchField.getTextEditor().setColumns(30);

        searchField.addDocumentListener(new com.intellij.ui.DocumentAdapter() {
            @Override
            protected void textChanged(@org.jetbrains.annotations.NotNull javax.swing.event.DocumentEvent e) {
                applyFilters(searchField.getText().trim());
            }
        });

        header.add(searchField);

        this.footer = new Footer();
        this.panel.add(footer, BorderLayout.SOUTH);
        this.footer.updateStatus(allTestCases.size(), allTestCases.size());

        this.list = new JBList<>(model);
        list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
        list.setOpaque(true);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);

        list.addListSelectionListener(new SelectionListenerImpl(list));
        list.addMouseListener(new MouseAdapterImpl(list, model, dir));

        Runnable resetGroupFilter = () -> {
            if (!selectedGroups.isEmpty()) {
                selectedGroups.clear();
                applyGroupFilters();
            }
        };

        Runnable resetDetailsFilter = () -> {
            if (!selectedDetails.isEmpty()) {
                selectedDetails.clear();
                updateDetailsButtonState();
            }
        };
        list.setTransferHandler(new TransferImpl(dir, model, resetGroupFilter));
        list.setTransferHandler(new TransferImpl(dir, model, resetDetailsFilter));
        ShortcutHandler.register(dir, list, model);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);

        JCheckBox showGroupsCheck = new JCheckBox("Show Groups", showGroups);
        showGroupsCheck.setOpaque(false);
        showGroupsCheck.setFocusable(false);
        showGroupsCheck.addActionListener(e -> {
            showGroups = showGroupsCheck.isSelected();
            list.repaint();
        });

        header.add(showGroupsCheck);

        JCheckBox showPriorityCheck = new JCheckBox("Show Priority", showPriority);
        showPriorityCheck.setOpaque(false);
        showPriorityCheck.addActionListener(e -> {
            showPriority = showPriorityCheck.isSelected();
            list.repaint();
        });

        header.add(showPriorityCheck);

        this.list.setCellRenderer(new RendererImpl(this));
    }

    private void showGroupPopup(JButton anchor) {
        JBList<GroupType> groupList = new JBList<>(GroupType.values());

        groupList.setBackground(JBColor.namedColor("Popup.background", new JBColor(0xffffff, 0x3c3f41)));

        groupList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JCheckBox checkBox = new JCheckBox(value.name(), selectedGroups.contains(value));
            checkBox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            checkBox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            checkBox.setBorder(JBUI.Borders.empty(2, 8)); // Native-like horizontal padding
            checkBox.setOpaque(true);
            return checkBox;
        });

        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = groupList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    GroupType group = groupList.getModel().getElementAt(index);
                    if (selectedGroups.contains(group)) {
                        selectedGroups.remove(group);
                    } else {
                        selectedGroups.add(group);
                    }
                    groupList.repaint();
                    applyGroupFilters();
                }
            }
        });

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(groupList), null)
                .setMovable(false)
                .setRequestFocus(true)
                .setResizable(false)
                .setCancelOnClickOutside(true)
                .createPopup();

        popup.showUnderneathOf(anchor);
    }

    private void showDetailsPopup(JButton anchor) {
        JBList<String> detailsList = new JBList<>(List.of("ID", "Module", "Expected Result", "Steps", "Automation Ref", "Business Ref"));

        detailsList.setBackground(JBColor.namedColor("Popup.background", new JBColor(0xffffff, 0x3c3f41)));

        detailsList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JCheckBox checkBox = new JCheckBox(value, selectedDetails.contains(value));
            checkBox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            checkBox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            checkBox.setBorder(JBUI.Borders.empty(2, 8)); // Native-like horizontal padding
            checkBox.setOpaque(true);
            return checkBox;
        });

        detailsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = detailsList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    String detailName = detailsList.getModel().getElementAt(index);

                    if (selectedDetails.contains(detailName)) {
                        selectedDetails.remove(detailName);
                    } else {
                        selectedDetails.add(detailName);
                    }

                    list.setFixedCellHeight(-1);
                    list.setCellRenderer(new RendererImpl(FileEditorImpl.this));
                    detailsList.repaint();
                    list.revalidate();
                    list.repaint();
                    saveSettings();
                    updateDetailsButtonState();
                }
            }
        });

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(detailsList), null)
                .setMovable(false)
                .setRequestFocus(true)
                .setResizable(false)
                .setCancelOnClickOutside(true)
                .createPopup();

        popup.showUnderneathOf(anchor);
    }

    private void applyGroupFilters() {
        syncListener.pause();

        try {
            if (selectedGroups.isEmpty()) {
                model.replaceAll(allTestCases);
                groupButton.setText("Groups");
                groupButton.setForeground(JBColor.foreground());

            } else {
                List<TestCase> filtered = allTestCases.stream()
                        .filter(tc -> tc.getGroups() != null &&
                                tc.getGroups().stream().anyMatch(selectedGroups::contains))
                        .collect(Collectors.toList());
                model.replaceAll(filtered);
                groupButton.setText("Groups (" + selectedGroups.size() + ")");
                groupButton.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
            }
        } finally {
            syncListener.resume();
        }
    }

    private void updateDetailsButtonState() {
        if (selectedDetails.isEmpty()) {
            detailsButton.setText("Details");
            detailsButton.setForeground(JBColor.foreground());
        } else {
            detailsButton.setText("Details (" + selectedDetails.size() + ")");
            detailsButton.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
        }
        list.repaint();
    }

    private void saveSettings() {
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue("testGit.showGroups", showGroups, true);
        props.setValue("testGit.showPriority", showPriority, true);
        props.setValue("testGit.selectedDetails", String.join(",", selectedDetails));
    }

    public void applyFilters(String query) {
        this.currentSearchQuery = query;

        if (syncListener != null) syncListener.pause();

        try {
            List<TestCase> filtered = allTestCases.stream()
                    .filter(tc -> {
                        boolean matchesSearch = query.isEmpty() ||
                                (tc.getTitle() != null && tc.getTitle().toLowerCase().contains(query.toLowerCase()));

                        boolean matchesGroup = selectedGroups.isEmpty() ||
                                (tc.getGroups() != null && tc.getGroups().stream().anyMatch(selectedGroups::contains));

                        return matchesSearch && matchesGroup;
                    })
                    .collect(java.util.stream.Collectors.toList());

            model.replaceAll(filtered);

            if (footer != null) {
                footer.updateStatus(model.getSize(), allTestCases.size());
            }
        } finally {
            if (syncListener != null) syncListener.resume();
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
        /// when close editor reset details in view page
    }
}