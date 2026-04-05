package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.BaseEditorUI;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.editorPanel.testCaseEditor.TestEditorUI;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.ui.single.nnew.CreateTestCaseUI;
import testGit.util.KeyboardSet;
import testGit.util.Notifier;

import java.util.Set;
import java.util.UUID;

public class CreateTestCase extends DumbAwareAction {
    private final JBList<TestCaseDto> list;
    private final DirectoryDto dir;
    private final CollectionListModel<TestCaseDto> model;
    private final BaseEditorUI ui;

    public CreateTestCase(final BaseEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.list = list;
        this.dir = dir;
        this.model = model;
        this.ui = ui;
        this.registerCustomShortcutSet(KeyboardSet.CreateTestCase.getShortcut(), list);
    }

    /*@Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        CreateTestCaseDialog dialog = new CreateTestCaseDialog();
        if (dialog.showAndGet()) {
            TestCaseDto newTestCaseDto = new TestCaseDto();
            newTestCaseDto.setId(UUID.randomUUID().toString());
            newTestCaseDto.setTitle(dialog.getTitle());
            newTestCaseDto.setPriority(Priority.valueOf(dialog.getPriority()));
            newTestCaseDto.setGroups(dialog.getSelectedGroups());

            if (ui != null) {
                ui.appendNewTestCase(newTestCaseDto);
            } else {
                model.add(newTestCaseDto);
            }
        }
    }*/

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        UnifiedVirtualFile unifiedFile = null;
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (virtualFile instanceof UnifiedVirtualFile)
            unifiedFile = (UnifiedVirtualFile) virtualFile;

        Set<String> stepCache = unifiedFile != null ? unifiedFile.getUniqueSteps() : null;
        final UnifiedVirtualFile finalUnifiedFile = unifiedFile;

        new CreateTestCaseUI().show(newTestCaseDto -> {
            newTestCaseDto.setId(UUID.randomUUID().toString());

            if (newTestCaseDto.getSteps() != null && finalUnifiedFile != null)
                newTestCaseDto.getSteps().forEach(finalUnifiedFile::addNewStepToCache);

            if (ui != null)
                ui.appendNewTestCase(newTestCaseDto);
            else
                model.add(newTestCaseDto);

            Notifier.info("Test Case Created", newTestCaseDto.getTitle());

        }, stepCache);
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