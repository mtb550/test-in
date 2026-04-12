package testGit.editorPanel.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBList;
import testGit.editorPanel.testCaseEditor.TestEditorUI;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
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

        if (KeyboardSet.CopyTestCaseTitle.matches(e)) {
            final List<TestCaseDto> selectedCases = list.getSelectedValuesList();
            if (selectedCases != null && !selectedCases.isEmpty()) {
                final String titles = selectedCases.stream()
                        .map(TestCaseDto::getTitle)
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
                ui.getAllTestCaseDtos().removeAll(selectedCases);
                ui.refreshView();

                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    final Path dirPath = ui.getVf().getTestSet().getPath();

                    selectedCases.forEach(tc -> {
                        try {
                            /// TODO: move to files controller class, any file action should be in the file calls
                            /// TODO: same tree class, any crud on tree should be from the unified class TreeUtilImpl
                            Files.deleteIfExists(dirPath.resolve(tc.getId() + ".json"));
                        } catch (final Exception ex) {
                            System.err.println("Failed to delete test case JSON: " + tc.getId());
                        }
                    });

                    ApplicationManager.getApplication().invokeLater(ui::updateSequenceAndSaveAll);
                });
            }
        }
    }
}