package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.testEditor.TestEditor;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.ui.testCase.CreateTestCaseDialog;
import org.testin.util.KeyboardSet;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;
import org.testin.util.services.TestCaseCacheService;
import org.testin.util.services.TestCasePersistService;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CreateTestCase extends DumbAwareAction {
    private final @NotNull IEditor editor;
    private final @NotNull TestSetDirectoryDto dir;

    public CreateTestCase(final @NotNull IEditor editor, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.editor = editor;
        this.dir = dir;
        this.registerCustomShortcutSet(KeyboardSet.CreateTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        new CreateTestCaseDialog(project, (tc, cg) -> {
            List<TestCaseDto> tcs = editor.getAllTestCases();

            final boolean isEmpty = tcs.isEmpty();
            tc.setIsHead(isEmpty);

            final TestCaseDto lastTc = isEmpty ? null : tcs.getLast();
            if (lastTc != null)
                lastTc.setNext(tc.getId());

            tc.setParent(dir);
            editor.appendNewTestCase(tc);

            final List<TestCaseDto> affectedNodes = Stream.of(tc, lastTc).filter(Objects::nonNull).toList();
            Services.getInstance(project, TestCaseCacheService.class).addNewItems(affectedNodes);

            Services.getInstance(project, TestCasePersistService.class).persist(dir.getPath(), affectedNodes);
            Services.getInstance(project, Notifier.class).softShow(project, "Created..");

            if (cg.isSelected())
                GeneratorType.CREATE_TEST_METHOD.getAction().execute(project, tc);

            SwingUtilities.invokeLater(() -> editor.selectTestCase(tc));

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