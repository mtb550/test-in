package org.testin.editor.toolbar.components;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import javax.swing.*;

/**
 * Focuses the toolbar search field from anywhere inside the editor the scope
 * component spans (issue #18). Registered on the editor's main panel, so the
 * shortcut never leaks outside a Testin editor.
 */
public class FocusSearchAction extends DumbAwareAction {
    private final @NotNull SearchTxt searchTxt;

    public FocusSearchAction(final @NotNull SearchTxt searchTxt, final @NotNull JComponent scope) {
        super("Focus Search");
        this.searchTxt = searchTxt;
        registerCustomShortcutSet(Shortcuts.FocusSearch.getCustomShortcut(), scope);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        searchTxt.getTextEditor().requestFocusInWindow();
        searchTxt.getTextEditor().selectAll();
    }
}
