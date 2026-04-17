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
import java.util.List;

public abstract class AbstractToolbarPanel extends JBPanel<AbstractToolbarPanel> implements Disposable {

    protected final IToolBar callbacks;
    @Getter
    protected final ToolBarSettings settings;

    @Getter
    protected final SearchTxt searchTxtField;

    public AbstractToolbarPanel(final Disposable pDisposable, final IToolBar callbacks) {
        super(new GridBagLayout());
        this.callbacks = callbacks;
        this.settings = new ToolBarSettings();

        setBackground(JBUI.CurrentTheme.EditorTabs.background());

        this.searchTxtField = new SearchTxt(callbacks::onToolBarSearchValueChanged);

        Disposer.register(pDisposable, this);
        Disposer.register(this, this.searchTxtField);
    }

    protected void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        for (IToolbarItem item : getCustomComponents()) {
            if (item instanceof JComponent component) {
                add(component, gbc);
                gbc.gridx++;
            }
        }

        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(searchTxtField, gbc);
    }

    public void resetFilters() {
        settings.resetFilters(); // todo: to be removed after refactor filter class
        updateFilterPopupState(); // todo: to be removed after refactor filter class
        callbacks.onToolBarFilterResetted();
    }

    // todo: to be removed after refactor filter class
    protected abstract void updateFilterPopupState();

    protected abstract List<IToolbarItem> getCustomComponents();

    @Override
    public void dispose() {
        this.removeAll();
    }
}