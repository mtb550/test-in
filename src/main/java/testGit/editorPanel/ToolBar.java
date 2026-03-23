package testGit.editorPanel;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import testGit.pojo.GroupType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reusable toolbar header panel for test-case editors.
 * Contains: group filter, details filter, search field, show-groups and show-priority toggles.
 *
 * <p>Callers implement {@link Callbacks} to receive notifications when state changes
 * so they can refresh their list/view without this class holding any reference to it.</p>
 */
public class ToolBar extends JBPanel<ToolBar> {

    // --- Persisted settings keys ---
    private static final String KEY_SHOW_GROUPS = "testGit.showGroups";
    private static final String KEY_SHOW_PRIORITY = "testGit.showPriority";
    private static final String KEY_DETAILS = "testGit.selectedDetails";
    private static final String DEFAULT_DETAILS = "ID,Module,Expected Result,Steps,Automation Ref,Business Ref";
    // --- State ---
    @Getter
    private final Set<GroupType> selectedGroups = new HashSet<>();
    @Getter
    private final Set<String> selectedDetails = new HashSet<>();
    // --- Widgets ---
    @Getter
    private final SearchTextField searchField = new SearchTextField();
    private final JButton groupButton;
    private final JButton detailsButton;
    private final Callbacks callbacks;
    @Getter
    private boolean showGroups;
    @Getter
    private boolean showPriority;

    public ToolBar(Callbacks callbacks) {
        super(new FlowLayout(FlowLayout.LEFT, JBUI.scale(5), JBUI.scale(2)));
        this.callbacks = callbacks;

        setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));
        setBackground(JBUI.CurrentTheme.EditorTabs.background());

        // --- Restore persisted settings ---
        PropertiesComponent props = PropertiesComponent.getInstance();
        showGroups = props.getBoolean(KEY_SHOW_GROUPS, true);
        showPriority = props.getBoolean(KEY_SHOW_PRIORITY, true);
        String saved = props.getValue(KEY_DETAILS, DEFAULT_DETAILS);
        if (!saved.isEmpty()) selectedDetails.addAll(List.of(saved.split(",")));

        // --- Group filter button ---
        groupButton = createToolbarButton("Groups", AllIcons.Actions.GroupBy);
        groupButton.addActionListener(e -> showGroupPopup(groupButton));
        add(groupButton);

        // --- Details filter button ---
        detailsButton = createToolbarButton("Details", AllIcons.Actions.PreviewDetailsVertically);
        detailsButton.addActionListener(e -> showDetailsPopup(detailsButton));
        add(detailsButton);

        // --- Search ---
        searchField.getTextEditor().setColumns(30);
        searchField.addDocumentListener(new com.intellij.ui.DocumentAdapter() {
            @Override
            protected void textChanged(@org.jetbrains.annotations.NotNull javax.swing.event.DocumentEvent e) {
                callbacks.onFilterChanged();
            }
        });
        add(searchField);

        // --- Show Groups toggle ---
        JCheckBox showGroupsCheck = new JCheckBox("Show Groups", showGroups);
        showGroupsCheck.setOpaque(false);
        showGroupsCheck.setFocusable(false);
        showGroupsCheck.addActionListener(e -> {
            showGroups = showGroupsCheck.isSelected();
            saveSettings();
            callbacks.onFilterChanged();
        });
        add(showGroupsCheck);

        // --- Show Priority toggle ---
        JCheckBox showPriorityCheck = new JCheckBox("Show Priority", showPriority);
        showPriorityCheck.setOpaque(false);
        showPriorityCheck.addActionListener(e -> {
            showPriority = showPriorityCheck.isSelected();
            saveSettings();
            callbacks.onFilterChanged();
        });
        add(showPriorityCheck);

        updateDetailsButtonState();
    }

    /**
     * Returns the current search query (trimmed, lower-case).
     */
    public String getSearchQuery() {
        return searchField.getText().trim().toLowerCase();
    }

    /**
     * Clears groups and details selections — used when the list data is replaced externally.
     */
    public void resetFilters() {
        selectedGroups.clear();
        selectedDetails.clear();
        updateGroupButtonState();
        updateDetailsButtonState();
    }

    private JButton createToolbarButton(String text, Icon icon) {
        JButton btn = new JButton(text, icon);
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(JBUI.Fonts.label(12f));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(JBUI.CurrentTheme.ActionButton.hoverBackground());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false);
            }
        });
        return btn;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void showGroupPopup(JButton anchor) {
        JBList<GroupType> groupList = new JBList<>(GroupType.values());
        groupList.setBackground(JBColor.namedColor("Popup.background", new JBColor(0xffffff, 0x3c3f41)));
        groupList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JCheckBox cb = new JCheckBox(value.name(), selectedGroups.contains(value));
            cb.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            cb.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            cb.setBorder(JBUI.Borders.empty(2, 8));
            cb.setOpaque(true);
            return cb;
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
                    updateGroupButtonState();
                    callbacks.onFilterChanged();
                }
            }
        });

        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(groupList), null)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .createPopup()
                .showUnderneathOf(anchor);
    }

    private void showDetailsPopup(JButton anchor) {
        List<String> detailOptions = List.of("ID", "Module", "Expected Result", "Steps", "Automation Ref", "Business Ref");
        JBList<String> detailsList = new JBList<>(detailOptions);
        detailsList.setBackground(JBColor.namedColor("Popup.background", new JBColor(0xffffff, 0x3c3f41)));
        detailsList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JCheckBox cb = new JCheckBox(value, selectedDetails.contains(value));
            cb.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            cb.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            cb.setBorder(JBUI.Borders.empty(2, 8));
            cb.setOpaque(true);
            return cb;
        });
        detailsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = detailsList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    String detail = detailsList.getModel().getElementAt(index);
                    if (selectedDetails.contains(detail)) selectedDetails.remove(detail);
                    else selectedDetails.add(detail);
                    detailsList.repaint();
                    saveSettings();
                    updateDetailsButtonState();
                    callbacks.onDetailsChanged();
                }
            }
        });

        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(detailsList), null)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .createPopup()
                .showUnderneathOf(anchor);
    }

    private void updateGroupButtonState() {
        if (selectedGroups.isEmpty()) {
            groupButton.setText("Groups");
            groupButton.setForeground(JBColor.foreground());
        } else {
            groupButton.setText("Groups (" + selectedGroups.size() + ")");
            groupButton.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
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
    }

    private void saveSettings() {
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(KEY_SHOW_GROUPS, showGroups, true);
        props.setValue(KEY_SHOW_PRIORITY, showPriority, true);
        props.setValue(KEY_DETAILS, String.join(",", selectedDetails));
    }

    /**
     * Implemented by the owning editor to react to header state changes.
     */
    public interface Callbacks {
        /**
         * Called when group filter, details filter, or search text changes — editor should re-filter and repaint.
         */
        void onFilterChanged();

        /**
         * Called when the details selection changes — editor should also force a cell-renderer refresh.
         */
        void onDetailsChanged();
    }
}
