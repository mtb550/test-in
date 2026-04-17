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

    // TODO: CHANGE RUNNABLE TO BE CONSUMER OR BYCONSUMER
    public SearchTxt(final Consumer<String> onToolBarSearchValueChanged) {
        super();

        setOpaque(false);
        getTextEditor().setOpaque(false);
        getTextEditor().setBackground(JBUI.CurrentTheme.EditorTabs.background());

        searchDebounceTimer = new Timer(300, e -> onToolBarSearchValueChanged.accept(getQuery()));
        searchDebounceTimer.setRepeats(false);

        addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                searchDebounceTimer.restart();
            }
        });
    }

    public String getQuery() {
        return getText().trim().toLowerCase();
    }

    @Override
    public void dispose() {
        if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
            searchDebounceTimer.stop();
        }
    }
}