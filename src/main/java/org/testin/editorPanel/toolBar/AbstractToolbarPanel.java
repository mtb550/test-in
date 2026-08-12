package org.testin.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.toolBar.components.*;
import org.testin.enums.ViewMode;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractToolbarPanel extends JBPanel<AbstractToolbarPanel> implements Disposable {

    @Getter
    protected final @NotNull SearchTxt searchTxt;

    @Getter
    private final @NotNull IToolBar callbacks;

    @Getter
    private final @NotNull Map<Class<? extends IToolbarItem>, IToolbarItem> toolbarItems = new HashMap<>();

    @Getter
    private @NotNull ViewMode currentView = ViewMode.LIST_VIEW;

    public AbstractToolbarPanel(final @NotNull IToolBar callbacks) {
        super(new GridBagLayout());
        this.callbacks = callbacks;

        setBackground(JBUI.CurrentTheme.EditorTabs.background());

        this.searchTxt = new SearchTxt(callbacks::onToolBarSearchValueChanged, callbacks::onToolBarSearchFocusReleased);
    }

    public <T extends IToolbarItem> @NotNull T getToolbarItem(final @NotNull Class<T> itemClass) {
        return itemClass.cast(toolbarItems.get(itemClass));
    }

    /**
     * Registers the search-focus shortcut on the given scope (the editor's
     * main panel), so the toolbar owns the whole search story: the field, its
     * callbacks and its shortcut (#18).
     */
    public void installSearchFocusShortcut(final @NotNull JComponent scope) {
        new FocusSearchAction(searchTxt, scope);
    }

    protected void layoutComponents() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        for (final IToolbarItem item : getCustomComponents()) {
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
        final @NotNull GridViewBtn gridBtn = getToolbarItem(GridViewBtn.class);
        final @NotNull ListViewBtn listBtn = getToolbarItem(ListViewBtn.class);

        gridBtn.addActionListener(e -> setView(ViewMode.GRID_VIEW));
        listBtn.addActionListener(e -> setView(ViewMode.LIST_VIEW));

        updateViewButtons();
    }

    private void setView(final @NotNull ViewMode view) {
        if (currentView == view) return;
        currentView = view;
        updateViewButtons();
    }

    private void updateViewButtons() {
        final @NotNull GridViewBtn gridBtn = getToolbarItem(GridViewBtn.class);
        final @NotNull ListViewBtn listBtn = getToolbarItem(ListViewBtn.class);

        gridBtn.setVisible(currentView == ViewMode.LIST_VIEW);
        listBtn.setVisible(currentView == ViewMode.GRID_VIEW);

        revalidate();
        repaint();
    }

    protected abstract @NotNull List<IToolbarItem> getCustomComponents();

    @Override
    public void dispose() {
        this.removeAll();
        this.toolbarItems.clear();
        Disposer.dispose(this.searchTxt);
    }
}