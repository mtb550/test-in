package org.testin.editor.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class SelectionListener implements ListSelectionListener {
    private final @NotNull Project p;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull TestinEditor editor;
    private final @NotNull ArrayList<String> path;

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            final @NotNull List<TestCaseDto> selected = list.getSelectedValuesList();

            if (!selected.isEmpty()) {
                list.ensureIndexIsVisible(list.getSelectedIndex());

                ViewToolWindowFactory.toolWindow(p)
                        .filter(ToolWindow::isVisible)
                        .flatMap(tw -> ViewToolWindowFactory.panel())
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
