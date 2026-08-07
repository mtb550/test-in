package org.testin.editorPanel.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.viewPanel.ViewToolWindowFactory;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SelectionListener implements ListSelectionListener {
    private final JBList<TestCaseDto> list;
    private final IEditor editor;
    private final ArrayList<String> path;

    private final @NotNull Project p;

    public SelectionListener(final @NotNull Project p, final JBList<TestCaseDto> list, final IEditor editor, final ArrayList<String> path) {
        this.p = p;
        this.list = list;
        this.editor = editor;
        this.path = path;
    }

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            final List<TestCaseDto> selected = list.getSelectedValuesList();

            if (selected != null && !selected.isEmpty()) {
                list.ensureIndexIsVisible(list.getSelectedIndex());

                Optional.ofNullable(ViewToolWindowFactory.getToolWindow(p))
                        .filter(ToolWindow::isVisible)
                        .map(tw -> ViewToolWindowFactory.getViewPanel())
                        .ifPresent(viewer -> viewer.show(selected, path));
            }

            Optional.ofNullable(editor.getStatusBar()).ifPresent(statusBar ->
                    statusBar.updateSelectionState(
                            list.getSelectedIndices(),
                            editor.getCurrentPage(),
                            editor.getPageSize(),
                            editor.getTotalItemsCount()
                    )
            );
        }
    }
}