package org.testin.editorPanel.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.viewPanel.ViewToolWindowFactory;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class SelectionListener implements ListSelectionListener {
    private final @NotNull Project p;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull IEditor editor;
    private final @NotNull ArrayList<String> path;

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            final List<TestCaseDto> selected = list.getSelectedValuesList();

            if (!selected.isEmpty()) {
                list.ensureIndexIsVisible(list.getSelectedIndex());

                Optional.ofNullable(ViewToolWindowFactory.getToolWindow(p))
                        .filter(ToolWindow::isVisible)
                        .map(tw -> ViewToolWindowFactory.getViewPanel())
                        .ifPresent(viewer -> viewer.show(selected, path));
            }

            editor.getStatusBar().updateSelectionState(
                    list.getSelectedIndices(),
                    editor.getCurrentPage(),
                    editor.getPageSize(),
                    editor.getTotalItemsCount()
            );
        }
    }
}
