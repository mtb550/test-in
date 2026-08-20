package org.testin.testcase;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.codegen.GenType;
import org.testin.editor.TestinEditor;
import org.testin.editor.test.TestEditor;
import org.testin.testcase.Rank;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.services.TestCasePersistService;
import org.testin.testcase.create.CreateTestCaseDialog;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CreateTestCaseAction extends AbstractProjectAction {
    private final @NotNull TestinEditor editor;
    private final @NotNull TestSetDirectoryDto dir;

    public CreateTestCaseAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.editor = editor;
        this.dir = dir;
        this.registerCustomShortcutSet(Shortcuts.CreateItem.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        openCreateDialog();
    }

    public void openCreateDialog() {
        new CreateTestCaseDialog(p, tc -> {
            final List<TestCaseDto> tcs = editor.getAllTestCases();

            // After the last one, and nothing else moves. The case that was last
            // used to be rewritten to point at this one, which is what made a
            // second tester adding a case at the same time conflict on a file
            // neither of them had opened.
            tc.setOrder(Rank.after(tcs.isEmpty() ? "" : tcs.getLast().getOrder()));

            tc.setParent(dir);
            editor.appendNewTestCase(tc);

            final List<TestCaseDto> affectedNodes = List.of(tc);
            Services.getInstance(p, TestCaseCacheService.class).addNewItems(affectedNodes);

            Services.getInstance(p, TestCasePersistService.class).persist(dir.getPath(), affectedNodes);
            Services.getInstance(p, Notifier.class).softShow(p, "Created");

            GenType.CREATE_TEST_CASE.getAction().execute(p, tc);

            ApplicationManager.getApplication().invokeLater(() -> editor.selectTestCase(tc));

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(editor instanceof TestEditor);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
