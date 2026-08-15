package org.testin.testcase;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.editor.toolbar.Toolbar;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.createDialog.TestCaseUpdateMenuDialog;
import org.testin.util.Shortcuts;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateTestCaseAction extends AbstractProjectAction {
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull Path path;
    private final @NotNull TestinEditor editor;

    public UpdateTestCaseAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list, final @NotNull Path path) {
        super(p, "Update");
        this.list = list;
        this.path = path;
        this.editor = editor;
        this.registerCustomShortcutSet(Shortcuts.UpdateItem.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        Logger.trace("update test cases: " + selectedItems.stream().map(TestCaseDto::getDescription).collect(Collectors.joining(", ")));

        new TestCaseUpdateMenuDialog(p, selectedItems, (updatedItems, gt) -> {

            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            for (final TestCaseDto tc : updatedItems)
                indexer.putTestCase(path, tc);

            Services.getInstance(p, Notifier.class).softShow(p, "Updated");

            if (editor instanceof Toolbar)
                ((Toolbar) editor).onToolBarFilterSelectionChanged();

            ApplicationManager.getApplication().invokeLater(() -> {
                list.repaint();
                TestCaseUpdateMenuDialog.applyAftermath(p, updatedItems, gt);
            });
        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
