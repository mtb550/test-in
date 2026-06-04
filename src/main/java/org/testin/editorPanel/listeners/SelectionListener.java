package org.testin.editorPanel.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.viewPanel.ViewToolWindowFactory;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class SelectionListener implements ListSelectionListener {
    private final JBList<TestCaseDto> list;
    private final IEditorUI ui;
    private final Path path;

    private final Project project;

    public SelectionListener(final @NotNull Project project, final JBList<TestCaseDto> list, final IEditorUI ui, final Path path) {
        this.project = project;
        this.list = list;
        this.ui = ui;
        this.path = path;
    }

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            final List<TestCaseDto> selected = list.getSelectedValuesList();

            if (selected != null && !selected.isEmpty()) {
                list.ensureIndexIsVisible(list.getSelectedIndex());

                Optional.ofNullable(ViewToolWindowFactory.getToolWindow(project))
                        .filter(ToolWindow::isVisible)
                        .map(tw -> ViewToolWindowFactory.getViewPanel())
                        .ifPresent(viewer -> viewer.show(selected, path));
            }

            Optional.ofNullable(ui.getStatusBar()).ifPresent(statusBar ->
                    statusBar.updateSelectionState(
                            list.getSelectedIndices(),
                            ui.getCurrentPage(),
                            ui.getPageSize(),
                            ui.getTotalItemsCount()
                    )
            );
        }
    }
}