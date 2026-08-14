package org.testin.editorPanel.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copies the selected descriptions to the clipboard.
 * <p>
 * Deletion is deliberately not here: {@code RemoveTestCaseAction} owns it and
 * already binds the same key on this list, so handling it here as well ran two
 * different deletions for one keypress - one of them without the confirmation.
 */
@AllArgsConstructor
public class KeyListener extends KeyAdapter {
    private final @NotNull Project p;
    private final @NotNull JBList<TestCaseDto> list;

    @Override
    public void keyPressed(final KeyEvent e) {
        if (!Shortcuts.CopyItem.matches(e)) return;

        final List<TestCaseDto> selectedCases = list.getSelectedValuesList();
        if (selectedCases.isEmpty()) return;

        final String titles = selectedCases.stream()
                .map(TestCaseDto::getDescription)
                .collect(Collectors.joining("\n"));

        final StringSelection selection = new StringSelection(titles);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }
}
