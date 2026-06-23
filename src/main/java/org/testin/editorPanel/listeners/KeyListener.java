package org.testin.editorPanel.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBList;
import org.testin.editorPanel.testEditor.TestEditorUI;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
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

    private final JBList<TestCaseDto> list;
    private final TestEditorUI ui;

    public KeyListener(final JBList<TestCaseDto> list, final TestEditorUI ui) {
        this.list = list;
        this.ui = ui;
    }

    @Override
    public void keyPressed(final KeyEvent e) {

        if (KeyboardSet.CopyTestCaseDescription.matches(e)) {
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
                ui.getAllTestCases().removeAll(selectedCases);
                ui.refreshView();

                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    final Path dirPath = ui.getVf().getTestSet().getPath();
                    final ProjectIndexer indexer = Services.getInstance(ui.getProject(), ProjectIndexer.class);

                    selectedCases.forEach(tc -> {
                        try {
                            indexer.removeTestCase(dirPath, tc.getId());
                        } catch (final Exception ex) {
                            Log.error("Failed to delete test case: " + tc.getId());
                        }
                    });

                    ApplicationManager.getApplication().invokeLater(ui::updateSequenceAndSaveAll);
                });
            }
        }
    }
}