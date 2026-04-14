package testGit.editorPanel.listeners;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import testGit.editorPanel.IEditor;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class SelectionListener implements ListSelectionListener {
    private final JBList<TestCaseDto> list;
    private final IEditor ui;
    private final Path path;

    public SelectionListener(final JBList<TestCaseDto> list, final IEditor ui, final Path path) {
        this.list = list;
        this.ui = ui;
        this.path = path;
    }

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            final List<TestCaseDto> selected = list.getSelectedValuesList();

            if (selected != null && !selected.isEmpty()) {
                Optional.ofNullable(ViewToolWindowFactory.getToolWindow())
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