package org.testin.editorPanel.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.testEditor.TestEditor;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class KeyListener extends KeyAdapter {

    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull TestEditor editor;

    public KeyListener(final @NotNull JBList<TestCaseDto> list, final @NotNull TestEditor editor) {
        this.list = list;
        this.editor = editor;
    }

    @Override
    public void keyPressed(final KeyEvent e) {

        if (KeyboardSet.CopyTestCase.matches(e)) {
            final List<TestCaseDto> selectedCases = list.getSelectedValuesList();
            if (selectedCases != null && !selectedCases.isEmpty()) {
                final String titles = selectedCases.stream()
                        .map(TestCaseDto::getDescription)
                        .collect(Collectors.joining("\n"));

                final StringSelection selection = new StringSelection(titles);
                final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
            }
            return;
        }

        if (e.getKeyCode() == KeyboardSet.DeletePackage.getKeyCode()) {
            final List<TestCaseDto> selectedCases = list.getSelectedValuesList();

            if (selectedCases != null && !selectedCases.isEmpty()) {
                editor.getAllTestCases().removeAll(selectedCases);
                editor.refreshView();

                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    final Path dirPath = editor.getParent().getPath();
                    final ProjectIndexer indexer = Services.getInstance(editor.getProject(), ProjectIndexer.class);

                    selectedCases.forEach(tc -> {
                        try {
                            indexer.removeTestCase(dirPath, tc.getId());
                        } catch (final Exception ex) {
                            Logger.error("Failed to delete test case: " + tc.getId());
                        }
                    });

                    ApplicationManager.getApplication().invokeLater(editor::updateSequenceAndSaveAll);
                });
            }
        }
    }
}