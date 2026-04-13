package testGit.editorPanel.toolBar;

import com.intellij.icons.AllIcons;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ToolBar extends JBPanel<ToolBar> {
    ///  TODO: use interface that implement any tool bar button.
    /// TODO: remove refresh text, details text, same as what you did with filter btn
    @Getter
    private final ToolBarSettings settings;

    @Getter
    private final SearchTextField searchField = new SearchTextField();
    private final JButton detailsBtn;
    private final JButton filterBtn;

    public ToolBar(final ToolBarCallback callbacks) {
        super(new FlowLayout(FlowLayout.LEFT, JBUI.scale(5), JBUI.scale(2)));
        this.settings = new ToolBarSettings();

        setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));
        setBackground(JBUI.CurrentTheme.EditorTabs.background());

        final JButton refreshButton = createToolbarButton("Refresh", AllIcons.Actions.Refresh);
        refreshButton.addActionListener(e -> callbacks.onRefresh());
        add(refreshButton);

        detailsBtn = createToolbarButton("Details", AllIcons.Actions.PreviewDetailsVertically);
        detailsBtn.addActionListener(e -> FilterPopupBuilder.showDetailsPopup(detailsBtn, settings.getSelectedDetails(), v -> {
            settings.save();
            callbacks.onDetailsChanged();
        }));
        add(detailsBtn);

        filterBtn = createToolbarButton("Filter", AllIcons.General.Filter);
        filterBtn.addActionListener(e -> FilterPopupBuilder.showFilterPopup(filterBtn, settings.getSelectedPriorities(), settings.getSelectedGroups(), v -> {
            updateFilterButtonState();
            callbacks.onFilterChanged();
        }));
        add(filterBtn);

        searchField.getTextEditor().setColumns(30);
        searchField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                callbacks.onFilterChanged();
            }
        });
        add(searchField);

        updateFilterButtonState();
    }

    private void updateFilterButtonState() {
        final int activeFiltersCount = settings.getSelectedPriorities().size() + settings.getSelectedGroups().size();
        if (activeFiltersCount == 0) {
            filterBtn.setText("");
            filterBtn.setToolTipText("Filter");
            filterBtn.setForeground(JBColor.foreground());
        } else {
            filterBtn.setText("(" + activeFiltersCount + ")");
            filterBtn.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
        }
    }

    public String getSearchQuery() {
        return searchField.getText().trim().toLowerCase();
    }

    public void resetFilters() {
        settings.resetFilters();
        searchField.setText("");
        updateFilterButtonState();
    }

    private JButton createToolbarButton(final String text, final Icon icon) {
        final JButton btn = new JButton(text, icon);
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setMargin(JBUI.insets(2));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(JBUI.Fonts.label(12f));
        btn.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(final MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(JBUI.CurrentTheme.ActionButton.hoverBackground());
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                btn.setContentAreaFilled(false);
            }
        });
        return btn;
    }
}