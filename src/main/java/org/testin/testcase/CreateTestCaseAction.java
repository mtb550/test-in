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
import org.testin.editor.testEditor.TestEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.services.TestCasePersistService;
import org.testin.testcase.createDialog.CreateTestCaseDialog;
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

            final boolean isEmpty = tcs.isEmpty();
            tc.setIsHead(isEmpty);

            final TestCaseDto lastTc = isEmpty ? null : tcs.getLast();
            if (lastTc != null) lastTc.setNext(tc.getId());

            tc.setParent(dir);
            editor.appendNewTestCase(tc);

            final List<TestCaseDto> affectedNodes = Stream.of(tc, lastTc).filter(Objects::nonNull).toList();
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
