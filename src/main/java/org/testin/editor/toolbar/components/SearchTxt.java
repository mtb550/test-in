package org.testin.editor.toolbar.components;

import com.intellij.openapi.Disposable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.KeyEvent;

public class SearchTxt extends SearchTextField implements Disposable, ToolbarItem {
    private final @NotNull Timer searchDebounceTimer;
    private final @NotNull Runnable onFocusReleased;

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-090
    public SearchTxt(final @NotNull Runnable onToolBarSearchValueChanged, final @NotNull Runnable onFocusReleased) {
        super();
        this.onFocusReleased = onFocusReleased;

        setOpaque(false);
        getTextEditor().setOpaque(false);
        getTextEditor().setBackground(JBUI.CurrentTheme.EditorTabs.background());
        getTextEditor().setToolTipText(Bundle.message("search.tooltip",
                Shortcuts.FocusSearch.getShortcutText(), Shortcuts.Escape.getShortcutText()));

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
     * UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-093.
     * <p>
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

    /**
     * What the tester typed, trimmed, and nothing else done to it.
     * <p>
     * It used to be lowercased here with no locale, and lowercased again by the
     * filter with {@code Locale.ROOT}, so the second one could not undo the
     * first: on a Turkish or Azeri IDE this turned the I of Login into a dotless
     * i, and the search came back empty for a word that was on screen. One owner
     * for the question, and it is the filter (#66, finding 82).
     */
    public @NotNull String getSearchQuery() {
        return getText().trim();
    }

    @Override
    public void dispose() {
        if (searchDebounceTimer.isRunning()) {
            searchDebounceTimer.stop();
        }
    }
}
