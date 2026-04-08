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
import testGit.ui.TestCase.CreateTestCaseUI;
import testGit.util.KeyboardSet;
import testGit.util.Notifier;
import testGit.util.cache.TestCaseCacheService;

import java.util.UUID;

public class CreateTestCase extends DumbAwareAction {
    private final CollectionListModel<TestCaseDto> model;
    private final BaseEditorUI ui;

    public CreateTestCase(final BaseEditorUI ui, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.model = model;
        this.ui = ui;
        this.registerCustomShortcutSet(KeyboardSet.CreateTestCase.getShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        new CreateTestCaseUI().show(newTestCaseDto -> {
            newTestCaseDto.setId(UUID.randomUUID().toString());

            TestCaseCacheService cache = TestCaseCacheService.getInstance(Config.getProject());
            cache.addTitle(newTestCaseDto.getTitle());
            cache.addExpected(newTestCaseDto.getExpected());
            if (newTestCaseDto.getSteps() != null)
                newTestCaseDto.getSteps().forEach(cache::addStep);

            if (ui != null)
                ui.appendNewTestCase(newTestCaseDto);
            else
                model.add(newTestCaseDto);

            Notifier.info("Test Case Created", newTestCaseDto.getTitle());

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