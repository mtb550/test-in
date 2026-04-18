package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.IEditorUI;
import testGit.editorPanel.testCaseEditor.TestEditorUI;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.TestCase.CreateTestCaseUI;
import testGit.util.KeyboardSet;
import testGit.util.services.TestCaseCacheService;
import testGit.util.services.TestCasePersistService;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CreateTestCase extends DumbAwareAction {
    private final CollectionListModel<TestCaseDto> model;
    private final IEditorUI ui;
    private final Path path;

    public CreateTestCase(final IEditorUI ui, final Path path, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.model = model;
        this.ui = ui;
        this.path = path;
        this.registerCustomShortcutSet(KeyboardSet.CreateTestCase.getCustomShortcut(), list);
    }

    // todo: isolate the bedy as it same action performed, create new method and call it in both.
    public static void execute(final IEditorUI ui, final Path path, final CollectionListModel<TestCaseDto> model) {
        new CreateTestCaseUI().show(newTc -> {

            boolean isEmpty = model.isEmpty();
            newTc.setIsHead(isEmpty);
            TestCaseDto lastTc = isEmpty ? null : model.getElementAt(model.getSize() - 1);
            if (lastTc != null) lastTc.setNext(newTc.getId());

            if (ui != null) ui.appendNewTestCase(newTc);
            else model.add(newTc);

            Project project = Config.getProject();
            List<TestCaseDto> affectedNodes = Stream.of(newTc, lastTc).filter(Objects::nonNull).toList();
            TestCaseCacheService.getInstance(project).addNewItems(affectedNodes);
            TestCasePersistService.getInstance(Config.getProject()).persist(path, affectedNodes);

        });
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        new CreateTestCaseUI().show(newTc -> {
            boolean isEmpty = model.isEmpty();
            newTc.setIsHead(isEmpty);
            TestCaseDto lastTc = isEmpty ? null : model.getElementAt(model.getSize() - 1);
            if (lastTc != null) lastTc.setNext(newTc.getId());

            if (ui != null) ui.appendNewTestCase(newTc);
            else model.add(newTc);

            Project project = Config.getProject();
            List<TestCaseDto> affectedNodes = Stream.of(newTc, lastTc).filter(Objects::nonNull).toList();
            TestCaseCacheService.getInstance(project).addNewItems(affectedNodes);
            TestCasePersistService.getInstance(Config.getProject()).persist(path, affectedNodes);

            /// to be implemented by use brodcasting
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
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(ui instanceof TestEditorUI);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}