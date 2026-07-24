package org.testin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.Dialogs.testCase.TestCaseUpdateMenu;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.autoGenerator.GeneratorAction;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.autoGenerator.UpdateTestMethod;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateTestCase extends DumbAwareAction {

    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull Path path;
    private final @NotNull IEditor editor;

    public UpdateTestCase(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list, final @NotNull Path path) {
        super("Update");
        this.list = list;
        this.path = path;
        this.editor = editor;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        Log.trace("update test cases: " + selectedItems.stream().map(TestCaseDto::getDescription).collect(Collectors.joining(", ")));

        new TestCaseUpdateMenu(project, selectedItems, (updatedItems, cg) -> {

            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
            for (final TestCaseDto tc : updatedItems)
                indexer.putTestCase(path, tc);

            Services.getInstance(project, Notifier.class).softShow(project, "Updated..");

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

                if (cg.isSelected()) {
                    final GeneratorType gt = cg.getGt();
                    Log.trace("Code generator selected: " + gt);

                    if (gt != null) {
                        final GeneratorAction action = gt.getAction();
                        final TestCaseDto firstItem = updatedItems.getFirst();

                        if (action instanceof UpdateTestMethod utm) utm.setGt(gt);

                        ApplicationManager.getApplication().executeOnPooledThread(() -> action.execute(project, firstItem));
                    }
                } else {
                    Log.trace("Code generator is NOT selected or is null.");
                }
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