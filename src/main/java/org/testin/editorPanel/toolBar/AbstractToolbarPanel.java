package org.testin.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.testin.editorPanel.toolBar.components.GridViewBtn;
import org.testin.editorPanel.toolBar.components.IToolbarItem;
import org.testin.editorPanel.toolBar.components.ListViewBtn;
import org.testin.editorPanel.toolBar.components.SearchTxt;
import org.testin.enums.ViewMode;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractToolbarPanel extends JBPanel<AbstractToolbarPanel> implements Disposable {

    @Getter
    protected final SearchTxt searchTxt;

    @Getter
    private final IToolBar callbacks;

    @Getter
    private final Map<Class<? extends IToolbarItem>, IToolbarItem> toolbarItems = new HashMap<>();

    @Getter
    private ViewMode currentView = ViewMode.LIST_VIEW;

    public AbstractToolbarPanel(final IToolBar callbacks) {
        super(new GridBagLayout());
        this.callbacks = callbacks;

        setBackground(JBUI.CurrentTheme.EditorTabs.background());

        this.searchTxt = new SearchTxt(callbacks::onToolBarSearchValueChanged);
    }

    public <T extends IToolbarItem> T getToolbarItem(Class<T> itemClass) {
        return itemClass.cast(toolbarItems.get(itemClass));
    }

    protected void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        for (IToolbarItem item : getCustomComponents()) {
            if (item instanceof JComponent component) {
                toolbarItems.put(item.getClass(), item);
                add(component, gbc);
                gbc.gridx++;
            }
        }

        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(searchTxt, gbc);

        toolbarItems.put(SearchTxt.class, searchTxt);

        wireViewButtons();
    }

    private void wireViewButtons() {
        final GridViewBtn gridBtn = getToolbarItem(GridViewBtn.class);
        final ListViewBtn listBtn = getToolbarItem(ListViewBtn.class);
        if (gridBtn == null || listBtn == null) return;

        gridBtn.addActionListener(e -> setView(ViewMode.GRID_VIEW));
        listBtn.addActionListener(e -> setView(ViewMode.LIST_VIEW));

        updateViewButtons();
    }

    private void setView(final ViewMode view) {
        if (currentView == view) return;
        currentView = view;
        updateViewButtons();
    }

    private void updateViewButtons() {
        final GridViewBtn gridBtn = getToolbarItem(GridViewBtn.class);
        final ListViewBtn listBtn = getToolbarItem(ListViewBtn.class);
        if (gridBtn == null || listBtn == null) return;

        gridBtn.setVisible(currentView == ViewMode.LIST_VIEW);
        listBtn.setVisible(currentView == ViewMode.GRID_VIEW);
        revalidate();
        repaint();
    }

    protected abstract List<IToolbarItem> getCustomComponents();

    @Override
    public void dispose() {
        this.removeAll();
        this.toolbarItems.clear();
        if (this.searchTxt != null) {
            Disposer.dispose(this.searchTxt);
        }
    }
}