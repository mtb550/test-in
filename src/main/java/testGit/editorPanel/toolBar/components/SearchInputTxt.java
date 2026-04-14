package testGit.editorPanel.toolBar.components;

import com.intellij.openapi.Disposable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class SearchInputTxt extends SearchTextField implements Disposable {
    private final Timer searchDebounceTimer;

    // TODO: CHANGE RUNNABLE TO BE CONSUMER OR BYCONSUMER
    public SearchInputTxt(Runnable onSearchChanged) {
        super();

        setOpaque(false);
        getTextEditor().setOpaque(false);
        getTextEditor().setBackground(JBUI.CurrentTheme.EditorTabs.background());

        searchDebounceTimer = new Timer(300, e -> onSearchChanged.run());
        searchDebounceTimer.setRepeats(false);

        addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                searchDebounceTimer.restart();
            }
        });
    }

    public String getSearchQuery() {
        return getText().trim().toLowerCase();
    }

    public void resetSearch() {
        setText(null);
    }

    @Override
    public void dispose() {
        if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
            searchDebounceTimer.stop();
        }
    }
}