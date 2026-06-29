package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.testEditor.TestEditorUI;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.ui.testCase.CreateTestCaseUI;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;
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
    private final @NotNull IEditorUI ui;
    private final @NotNull DirectoryDto dir;

    public CreateTestCase(final @NotNull IEditorUI ui, final @NotNull DirectoryDto dir, final @NotNull JBList<TestCaseDto> list) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.ui = ui;
        this.dir = dir;
        this.registerCustomShortcutSet(KeyboardSet.CreateTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        new CreateTestCaseUI(project, (newTc, codeGenerator) -> {
            List<TestCaseDto> allCases = ui.getAllTestCases();

            final boolean isEmpty = allCases.isEmpty();
            newTc.setIsHead(isEmpty);

            final TestCaseDto lastTc = isEmpty ? null : allCases.getLast();
            if (lastTc != null)
                lastTc.setNext(newTc.getId());

            newTc.setParent(dir);
            ui.appendNewTestCase(newTc);

            final List<TestCaseDto> affectedNodes = Stream.of(newTc, lastTc).filter(Objects::nonNull).toList();
            Services.getInstance(project, TestCaseCacheService.class).addNewItems(affectedNodes);

            Services.getInstance(project, TestCasePersistService.class).persist(dir.getPath(), affectedNodes);
            Services.getInstance(project, Notifier.class).softShow(project, "Created..");

            if (codeGenerator != null && codeGenerator.isSelected())
                GeneratorType.CREATE_TEST_METHOD.getAction().execute(project, newTc, Services.getInstance(project, Tools.class).buildFqcnMethod(newTc));

            SwingUtilities.invokeLater(() -> ui.selectTestCase(newTc));

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(ui instanceof TestEditorUI);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}