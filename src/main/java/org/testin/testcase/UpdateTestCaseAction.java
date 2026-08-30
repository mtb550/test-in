package org.testin.testcase;

import org.testin.notifications.Done;
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
import org.testin.testcase.create.TestCaseUpdateMenuDialog;
import org.testin.util.Shortcuts;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
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
        overSelection(TestCaseUpdateMenuDialog::show);
    }

    /**
     * The same update, started at one field instead of at the menu - what a
     * field's letter opens while a card is selected.
     */
    public void openField(final @NotNull UpdateTestCaseFields field) {
        overSelection(menu -> menu.open(field));
    }

    /**
     * The selected cases, and what follows an accepted update over them.
     * <p>
     * Both ways in need every line of it - the indexer, the balloon, the
     * toolbar's filter, the repaint - and neither has a reason to differ, so
     * the aftermath is written where they meet rather than in each of them.
     */
    private void overSelection(final @NotNull Consumer<TestCaseUpdateMenuDialog> open) {
        final @NotNull List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        Logger.trace("update test cases: " + selectedItems.stream().map(TestCaseDto::getDescription).collect(Collectors.joining(", ")));

        open.accept(new TestCaseUpdateMenuDialog(p, selectedItems, (updatedItems, gt) -> {

            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            for (final TestCaseDto tc : updatedItems)
                indexer.putTestCase(path, tc);

            Services.getInstance(p, Notifier.class).softShow(p, Done.UPDATED);

            if (editor instanceof Toolbar)
                ((Toolbar) editor).onToolBarFilterSelectionChanged();

            ApplicationManager.getApplication().invokeLater(() -> {
                // Ordered rather than repainted: the Order field writes a rank,
                // which moves the case and renumbers every card after it.
                editor.refreshOrdered();
                TestCaseUpdateMenuDialog.applyAftermath(p, updatedItems, gt);
            });
        }));
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
