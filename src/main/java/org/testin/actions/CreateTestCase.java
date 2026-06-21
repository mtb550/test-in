package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CreateTestCase extends DumbAwareAction {
    private final CollectionListModel<TestCaseDto> model;
    private final JBList<TestCaseDto> list;
    private final IEditorUI ui;
    private final DirectoryDto dir;

    public CreateTestCase(final IEditorUI ui, final DirectoryDto pDir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.model = model;
        this.list = list;
        this.ui = ui;
        this.dir = pDir;
        this.registerCustomShortcutSet(KeyboardSet.CreateTestCase.getCustomShortcut(), list);
    }

    public static void execute(final @NotNull Project project, final IEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        performCreation(project, ui, dir, list, model);
    }

    private static void performCreation(final @NotNull Project project, final @NotNull IEditorUI ui, final @NotNull DirectoryDto pDir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        new CreateTestCaseUI(project, (newTc, codeGenerator) -> {
            final boolean isEmpty = model.isEmpty();
            newTc.setIsHead(isEmpty);

            final TestCaseDto lastTc = isEmpty ? null : model.getElementAt(model.getSize() - 1);
            if (lastTc != null) lastTc.setNext(newTc.getId());

            List<String> generatedFqcn = new ArrayList<>(pDir.getFqcn());

            if (!generatedFqcn.isEmpty()) {
                int lastIdx = generatedFqcn.size() - 1;
                String className = Services.getInstance(project, Tools.class).sanitizeClassName(generatedFqcn.get(lastIdx));
                generatedFqcn.set(lastIdx, className);
            }

            String methodName = Services.getInstance(project, Tools.class).sanitizeMethodName(newTc.getDescription());
            generatedFqcn.add(methodName);

            newTc.setFqcn(generatedFqcn);
            newTc.setPath(pDir.getPath2());

            ui.appendNewTestCase(newTc);

            final List<TestCaseDto> affectedNodes = Stream.of(newTc, lastTc).filter(Objects::nonNull).toList();
            Services.getInstance(project, TestCaseCacheService.class).addNewItems(affectedNodes);

            Services.getInstance(project, TestCasePersistService.class).persist(pDir.getPath(), affectedNodes);
            Services.getInstance(project, Notifier.class).softShow(project, "Created..");

            if (codeGenerator != null && codeGenerator.isSelected()) {
                GeneratorType.CREATE_TEST_CASE.getAction().execute(project, newTc, newTc.getFqcn());
            }

            SwingUtilities.invokeLater(() -> ui.selectTestCase(newTc));

        }).show();
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        performCreation(project, ui, dir, list, model);
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