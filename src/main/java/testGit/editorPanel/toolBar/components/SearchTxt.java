package testGit.editorPanel.toolBar.components;

import com.intellij.openapi.Disposable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.function.Consumer;

public class SearchTxt extends SearchTextField implements Disposable, IToolbarItem {
    private final Timer searchDebounceTimer;

    public SearchTxt(final Consumer<String> onToolBarSearchValueChanged) {
        super();

        setOpaque(false);
        getTextEditor().setOpaque(false);
        getTextEditor().setBackground(JBUI.CurrentTheme.EditorTabs.background());

        searchDebounceTimer = new Timer(300, e -> onToolBarSearchValueChanged.accept(getSearchQuery()));
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
        if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
            searchDebounceTimer.stop();
        }
    }
}