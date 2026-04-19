package testGit.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.IEditorUI;
import testGit.editorPanel.testCaseEditor.TestEditorUI;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.TestCase.TestCaseUpdateMenu;
import testGit.util.KeyboardSet;
import testGit.util.services.TestCaseCacheService;
import testGit.util.services.TestCasePersistService;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.List;

public class UpdateTestCase extends DumbAwareAction {

    private final JBList<TestCaseDto> list;
    private final Path path;
    private final IEditorUI ui;

    public UpdateTestCase(final IEditorUI ui, final JBList<TestCaseDto> list, final Path path) {
        super("Edit Test Case");
        this.list = list;
        this.path = path;
        this.ui = ui;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (list == null) return;

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        new TestCaseUpdateMenu().show(selectedItems, updatedItems -> {
            TestCaseCacheService.getInstance(Config.getProject()).addNewItems(updatedItems);
            TestCasePersistService.getInstance(Config.getProject()).persist(path, updatedItems);

            ApplicationManager.getApplication().invokeLater(() -> {
                list.repaint();

                ViewPanel detailsPanel = ViewToolWindowFactory.getViewPanel();
                if (detailsPanel != null && detailsPanel.getCurrentTestCaseDto() != null) {
                    boolean isCurrentAffected = updatedItems.stream()
                            .anyMatch(item -> item.getId().equals(detailsPanel.getCurrentTestCaseDto().getId()));

                    if (isCurrentAffected) {
                        detailsPanel.refreshCurrentView();
                    }
                }
            });
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