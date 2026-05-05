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
import org.testin.editorPanel.testCaseEditor.TestEditorUI;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.ui.testCase.CreateTestCaseUI;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;
import org.testin.util.services.TestCaseCacheService;
import org.testin.util.services.TestCasePersistService;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CreateTestCase extends DumbAwareAction {
    private final CollectionListModel<TestCaseDto> model;
    private final JBList<TestCaseDto> list;
    private final IEditorUI ui;
    private final Path path;

    public CreateTestCase(final IEditorUI ui, final Path path, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.model = model;
        this.list = list;
        this.ui = ui;
        this.path = path;
        this.registerCustomShortcutSet(KeyboardSet.CreateTestCase.getCustomShortcut(), list);
    }

    public static void execute(final IEditorUI ui, final Path path, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        performCreation(ui, path, list, model);
    }

    private static void performCreation(final @NotNull IEditorUI ui, final @NotNull Path path, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        new CreateTestCaseUI().show((newTc, shouldGenerateOrUpdateCode) -> {
            boolean isEmpty = model.isEmpty();
            newTc.setIsHead(isEmpty);

            TestCaseDto lastTc = isEmpty ? null : model.getElementAt(model.getSize() - 1);
            if (lastTc != null) lastTc.setNext(newTc.getId());

            List<String> logicalPath = Tools.extractLogicalPath(path);
            newTc.setPath(logicalPath);

            List<String> generatedFqcn = Tools.generateFqcn(logicalPath);
            newTc.setFqcn(generatedFqcn);

            ui.appendNewTestCase(newTc);

            Project project = Config.getProject();
            List<TestCaseDto> affectedNodes = Stream.of(newTc, lastTc).filter(Objects::nonNull).toList();
            TestCaseCacheService.getInstance(project).addNewItems(affectedNodes);
            TestCasePersistService.getInstance(project).persist(path, affectedNodes);

            if (shouldGenerateOrUpdateCode)
                Tools.createJavaMethodInClass(project, newTc.getFqcn(), newTc.getDescription());

            SwingUtilities.invokeLater(() -> ui.selectTestCase(newTc));

            // todo,to be implemented by use broadcasting
            /*
            Config.getProject().getMessageBus()
                  .syncPublisher(TestCaseEventListener.TEST_CASE_ADDED_TOPIC)
                  .onTestCaseAdded(newTc);
            });
            */
            //if (tree != null && parentNode != null) TreeUtilImpl.createNode(tree, parentNode, newTc);

        });
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        performCreation(ui, path, list, model);
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