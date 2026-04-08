package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.BaseEditorUI;
import testGit.editorPanel.testCaseEditor.TestEditorUI;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.ui.TestCase.CreateTestCaseUI;
import testGit.util.KeyboardSet;
import testGit.util.cache.TestCaseCacheService;
import testGit.util.persist.TestCasePersistService;

import java.util.UUID;

public class CreateTestCase extends DumbAwareAction {
    private final CollectionListModel<TestCaseDto> model;
    private final BaseEditorUI ui;
    private final DirectoryDto dir;

    public CreateTestCase(final BaseEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.model = model;
        this.ui = ui;
        this.dir = dir;
        this.registerCustomShortcutSet(KeyboardSet.CreateTestCase.getShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        new CreateTestCaseUI().show(newTc -> {
            newTc.setId(UUID.randomUUID().toString());
            TestCaseCacheService.getInstance(Config.getProject()).addNewCacheItems(newTc);

            TestCaseDto lastTc = null;
            if (model.isEmpty())
                newTc.setIsHead(true);
            else {
                newTc.setIsHead(false);
                lastTc = model.getElementAt(model.getSize() - 1);
                lastTc.setNext(UUID.fromString(newTc.getId()));
            }

            if (ui != null)
                ui.appendNewTestCase(newTc);
            else
                model.add(newTc);

            TestCasePersistService.getInstance(Config.getProject()).persistNewTestCase(dir.getPath(), newTc, lastTc);

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
        return ActionUpdateThread.EDT;
    }
}