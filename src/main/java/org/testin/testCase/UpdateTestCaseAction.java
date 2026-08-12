package org.testin.testCase;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GeneratorAction;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testCase.createDialog.TestCaseUpdateMenuDialog;
import org.testin.util.Shortcuts;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateTestCaseAction extends DumbAwareAction {

    private final @NotNull Project p;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull Path path;
    private final @NotNull IEditor editor;

    public UpdateTestCaseAction(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list, final @NotNull Path path) {
        super("Update");
        this.p = p;
        this.list = list;
        this.path = path;
        this.editor = editor;
        this.registerCustomShortcutSet(Shortcuts.UpdateItem.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        Logger.trace("update test cases: " + selectedItems.stream().map(TestCaseDto::getDescription).collect(Collectors.joining(", ")));

        new TestCaseUpdateMenuDialog(p, selectedItems, (updatedItems, gt) -> {

            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            for (final TestCaseDto tc : updatedItems)
                indexer.putTestCase(path, tc);

            Services.getInstance(p, Notifier.class).softShow(p, "Updated..");

            if (editor instanceof IToolBar)
                ((IToolBar) editor).onToolBarFilterSelectionChanged();

            ApplicationManager.getApplication().invokeLater(() -> {
                list.repaint();

                ViewPanel detailsPanel = ViewToolWindowFactory.getViewPanel();
                if (detailsPanel != null && detailsPanel.getCurrentTestCaseDto() != null) {
                    boolean isCurrentAffected = updatedItems.stream()
                            .anyMatch(item -> item.getId().equals(detailsPanel.getCurrentTestCaseDto().getId()));
                    if (isCurrentAffected) {
                        detailsPanel.refreshCurrentView();
                    }
                }

                Logger.trace("Generating automation code: " + gt);
                final GeneratorAction action = gt.getAction();
                final TestCaseDto firstItem = updatedItems.getFirst();

                ApplicationManager.getApplication().executeOnPooledThread(() -> action.execute(p, firstItem));
            });
        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}