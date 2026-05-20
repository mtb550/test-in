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
import org.testin.util.autoGenerator.CreateJavaMethodInClass;
import org.testin.util.services.TestCaseCacheService;
import org.testin.util.services.TestCasePersistService;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
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
        new CreateTestCaseUI((newTc, codeGenerator) -> {
            final boolean isEmpty = model.isEmpty();
            newTc.setIsHead(isEmpty);

            final TestCaseDto lastTc = isEmpty ? null : model.getElementAt(model.getSize() - 1);
            if (lastTc != null) lastTc.setNext(newTc.getId());

            final List<String> logicalPath = Tools.getInstance().extractLogicalPath(path);
            newTc.setPath(logicalPath);

            final Project project = Config.getProject();
            final String projectName = project.getName();

            List<String> generatedFqcn;
            int startIndex = -1;

            for (int i = 0; i < logicalPath.size(); i++) {
                if (logicalPath.get(i).equalsIgnoreCase(projectName) || logicalPath.get(i).equalsIgnoreCase("testin")) {
                    startIndex = i;
                    break;
                }
            }

            if (startIndex != -1 && startIndex + 1 < logicalPath.size())
                generatedFqcn = new ArrayList<>(logicalPath.subList(startIndex + 1, logicalPath.size()));
            else
                generatedFqcn = new ArrayList<>(logicalPath);

            if (!generatedFqcn.isEmpty()) {
                int lastIdx = generatedFqcn.size() - 1;
                String lastElement = generatedFqcn.get(lastIdx);
                int dotIdx = lastElement.lastIndexOf('.');
                if (dotIdx > 0) {
                    generatedFqcn.set(lastIdx, lastElement.substring(0, dotIdx));
                }
            }

            String methodName = Tools.getInstance().formatMethodName(newTc.getDescription());
            generatedFqcn.add(methodName);

            newTc.setFqcn(generatedFqcn);
            ui.appendNewTestCase(newTc);

            final List<TestCaseDto> affectedNodes = Stream.of(newTc, lastTc).filter(Objects::nonNull).toList();
            TestCaseCacheService.getInstance(project).addNewItems(affectedNodes);
            TestCasePersistService.getInstance(project).persist(path, affectedNodes);

            if (codeGenerator != null && codeGenerator.isSelected())
                new CreateJavaMethodInClass().execute(project, newTc.getFqcn(), newTc);

            SwingUtilities.invokeLater(() -> ui.selectTestCase(newTc));

            // todo,to be implemented by use broadcasting
            /*
            Config.getProject().getMessageBus()
                  .syncPublisher(TestCaseEventListener.TEST_CASE_ADDED_TOPIC)
                  .onTestCaseAdded(newTc);
            });
            */
            //if (tree != null && parentNode != null) TreeUtilImpl.createNode(tree, parentNode, newTc);

        }).show();
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