package testGit.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import testGit.editorPanel.toolBar.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ActionToolbarPanel extends JBPanel<ActionToolbarPanel> implements Disposable {

    private final IToolBar callbacks;
    @Getter
    private final ToolBarSettings settings;

    private final RefreshBtn refreshBtn;
    private final DetailsBtn detailsBtn;
    private final FilterButton filterBtn;
    @Getter
    private final SearchInputTxt searchField;

    public ActionToolbarPanel(final Disposable pDisposable, final IToolBar callbacks) {
        super(new GridBagLayout());
        this.callbacks = callbacks;
        this.settings = new ToolBarSettings();

        setBackground(JBUI.CurrentTheme.EditorTabs.background());

        this.refreshBtn = new RefreshBtn(callbacks::onRefreshing);
        this.detailsBtn = new DetailsBtn(settings, callbacks::onDetailsChanged);
        this.filterBtn = new FilterButton(settings, this::resetFilters, callbacks::onFilterChanged);
        this.searchField = new SearchInputTxt(callbacks::onFilterChanged);

        layoutComponents();

        Disposer.register(pDisposable, this);
        Disposer.register(this, this.searchField);
    }

    private void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        add(refreshBtn, gbc);
        gbc.gridx++;

        add(detailsBtn, gbc);
        gbc.gridx++;

        add(filterBtn, gbc);
        gbc.gridx++;

        for (JComponent customComponent : getCustomComponents()) {
            add(customComponent, gbc);
            gbc.gridx++;
        }

        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(searchField, gbc);
    }

    public void resetFilters() {
        settings.resetFilters();
        searchField.resetSearch();
        filterBtn.updateState();
        callbacks.onFilterChanged();
    }

    protected List<JComponent> getCustomComponents() {
        return List.of();
    }

    @Override
    public void dispose() {
        this.removeAll();
    }
}