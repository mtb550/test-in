package org.testin.editor.toolbar.components;

import com.intellij.openapi.Disposable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.KeyEvent;

public class SearchTxt extends SearchTextField implements Disposable, ToolbarItem {
    private final @NotNull Timer searchDebounceTimer;
    private final @NotNull Runnable onFocusReleased;

    public SearchTxt(final @NotNull Runnable onToolBarSearchValueChanged, final @NotNull Runnable onFocusReleased) {
        super();
        this.onFocusReleased = onFocusReleased;

        setOpaque(false);
        getTextEditor().setOpaque(false);
        getTextEditor().setBackground(JBUI.CurrentTheme.EditorTabs.background());
        getTextEditor().setToolTipText("Search (" + Shortcuts.FocusSearch.getShortcutText() + "), "
                + Shortcuts.Escape.getShortcutText() + " to leave");

        searchDebounceTimer = new Timer(300, e -> onToolBarSearchValueChanged.run());
        searchDebounceTimer.setRepeats(false);

        addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                searchDebounceTimer.restart();
            }
        });
    }

    /**
     * ESC releases focus back to the editor list without clearing the filter
     * text (issue #18) - leaving the field is not resetting the search.
     */
    @Override
    protected boolean preprocessEventForTextField(final KeyEvent e) {
        if (Shortcuts.Escape.matches(e) && e.getID() == KeyEvent.KEY_PRESSED) {
            e.consume();
            onFocusReleased.run();
            return true;
        }
        return super.preprocessEventForTextField(e);
    }

    public @NotNull String getSearchQuery() {
        return getText().trim().toLowerCase();
    }

    public void resetSearchQuery() {
        if (!getText().isEmpty()) {
            setText("");
            if (searchDebounceTimer.isRunning()) {
                searchDebounceTimer.stop();
            }
        }
    }

    @Override
    public void dispose() {
        if (searchDebounceTimer.isRunning()) {
            searchDebounceTimer.stop();
        }
    }
}
