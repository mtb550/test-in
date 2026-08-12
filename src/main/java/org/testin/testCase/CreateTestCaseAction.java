package org.testin.testCase;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.testEditor.TestEditor;
import org.testin.codegen.GeneratorType;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.services.TestCasePersistService;
import org.testin.testCase.createDialog.CreateTestCaseDialog;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CreateTestCaseAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull IEditor editor;
    private final @NotNull TestSetDirectoryDto dir;

    public CreateTestCaseAction(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.p = p;
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
            List<TestCaseDto> tcs = editor.getAllTestCases();

            final boolean isEmpty = tcs.isEmpty();
            tc.setIsHead(isEmpty);

            final TestCaseDto lastTc = isEmpty ? null : tcs.getLast();
            if (lastTc != null) lastTc.setNext(tc.getId());

            tc.setParent(dir);
            editor.appendNewTestCase(tc);

            final List<TestCaseDto> affectedNodes = Stream.of(tc, lastTc).filter(Objects::nonNull).toList();
            Services.getInstance(p, TestCaseCacheService.class).addNewItems(affectedNodes);

            Services.getInstance(p, TestCasePersistService.class).persist(dir.getPath(), affectedNodes);
            Services.getInstance(p, Notifier.class).softShow(p, "Created..");

            GeneratorType.CREATE_TEST_CASE.getAction().execute(p, tc);

            ApplicationManager.getApplication().invokeLater(() -> editor.selectTestCase(tc));

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(editor instanceof TestEditor);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}