package testGit.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import testGit.editorPanel.toolBar.components.IToolbarItem;
import testGit.editorPanel.toolBar.components.SearchTxt;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractToolbarPanel extends JBPanel<AbstractToolbarPanel> implements Disposable {

    protected final IToolBar callbacks;

    @Getter
    protected final SearchTxt searchTxt;

    @Getter
    private final Map<Class<? extends IToolbarItem>, IToolbarItem> toolbarItems = new HashMap<>();

    public AbstractToolbarPanel(final Disposable pDisposable, final IToolBar callbacks) {
        super(new GridBagLayout());
        this.callbacks = callbacks;

        setBackground(JBUI.CurrentTheme.EditorTabs.background());

        this.searchTxt = new SearchTxt(callbacks::onToolBarSearchValueChanged);

        Disposer.register(pDisposable, this);
        Disposer.register(this, this.searchTxt);
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
            toolbarItems.put(item.getClass(), item);

            if (item instanceof JComponent component) {
                add(component, gbc);
                gbc.gridx++;
            }
        }

        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(searchTxt, gbc);

        toolbarItems.put(SearchTxt.class, searchTxt);
    }

    protected abstract List<IToolbarItem> getCustomComponents();

    @Override
    public void dispose() {
        this.removeAll();
        this.toolbarItems.clear();
    }
}