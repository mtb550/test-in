package testGit.editorPanel.toolBar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ToolBar extends JBPanel<ToolBar> {

    @Getter
    private final ToolBarSettings settings;

    @Getter
    private final SearchTextField searchField = new SearchTextField();

    private final JButton groupButton;

    private final JButton detailsButton;

    public ToolBar(ToolBarCallback callbacks) {
        super(new FlowLayout(FlowLayout.LEFT, JBUI.scale(5), JBUI.scale(2)));
        this.settings = new ToolBarSettings();

        setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));
        setBackground(JBUI.CurrentTheme.EditorTabs.background());

        JButton refreshButton = createToolbarButton("Refresh", AllIcons.Actions.Refresh);
        refreshButton.addActionListener(e -> callbacks.onRefresh());
        add(refreshButton);

        groupButton = createToolbarButton("Groups", AllIcons.Actions.GroupBy);
        groupButton.addActionListener(e -> FilterPopupBuilder.showGroupPopup(groupButton, settings.getSelectedGroups(), () -> {
            updateGroupButtonState();
            callbacks.onFilterChanged();
        }));
        add(groupButton);

        detailsButton = createToolbarButton("Details", AllIcons.Actions.PreviewDetailsVertically);
        detailsButton.addActionListener(e -> FilterPopupBuilder.showDetailsPopup(detailsButton, settings.getSelectedDetails(), () -> {
            settings.save();
            updateDetailsButtonState();
            callbacks.onDetailsChanged();
            callbacks.onFilterChanged();
        }));
        add(detailsButton);

        searchField.getTextEditor().setColumns(30);
        searchField.addDocumentListener(new com.intellij.ui.DocumentAdapter() {
            @Override
            protected void textChanged(@org.jetbrains.annotations.NotNull javax.swing.event.DocumentEvent e) {
                callbacks.onFilterChanged();
            }
        });
        add(searchField);

        JCheckBox showGroupsCheck = new JCheckBox("Show Groups", settings.isShowGroups());
        showGroupsCheck.setOpaque(false);
        showGroupsCheck.setFocusable(false);
        showGroupsCheck.addActionListener(e -> {
            settings.setShowGroups(showGroupsCheck.isSelected());
            settings.save();
            callbacks.onFilterChanged();
        });
        add(showGroupsCheck);

        updateGroupButtonState();
        updateDetailsButtonState();
    }

    public String getSearchQuery() {
        return searchField.getText().trim().toLowerCase();
    }

    public void resetFilters() {
        settings.resetFilters();
        updateGroupButtonState();
        updateDetailsButtonState();
    }

    private void updateGroupButtonState() {
        if (settings.getSelectedGroups().isEmpty()) {
            groupButton.setText("Groups");
            groupButton.setForeground(JBColor.foreground());

        } else {
            groupButton.setText("Groups (" + settings.getSelectedGroups().size() + ")");
            groupButton.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
        }
    }

    private void updateDetailsButtonState() {
        if (settings.getSelectedDetails().isEmpty()) {
            detailsButton.setText("Details");
            detailsButton.setForeground(JBColor.foreground());

        } else {
            detailsButton.setText("Details (" + settings.getSelectedDetails().size() + ")");
            detailsButton.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
        }
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
}