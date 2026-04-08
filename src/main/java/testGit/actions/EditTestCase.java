package testGit.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.TestCase.TestCaseEditMenu;
import testGit.util.KeyboardSet;
import testGit.util.cache.TestCaseCacheService;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import java.util.List;
import java.util.function.Consumer;

public class EditTestCase extends DumbAwareAction {

    private final JBList<TestCaseDto> list;

    public EditTestCase(final JBList<TestCaseDto> list) {
        super("Edit Test Case");
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (list == null) return;

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        Runnable onBulkUpdate = () -> {
            list.repaint();

            ViewPanel detailsPanel = ViewToolWindowFactory.getViewPanel();
            if (detailsPanel != null && detailsPanel.getCurrentTestCaseDto() != null) {

                boolean isCurrentAffected = selectedItems.stream()
                        .anyMatch(item -> item.getId().equals(detailsPanel.getCurrentTestCaseDto().getId()));

                if (isCurrentAffected)
                    detailsPanel.refreshCurrentView();
            }
        };

        // update cache
        Consumer<TestCaseDto> onSingleUpdate = updatedDto -> {
            TestCaseCacheService cache = TestCaseCacheService.getInstance(Config.getProject());
            cache.addTitle(updatedDto.getTitle());
            cache.addExpected(updatedDto.getExpected());
            if (updatedDto.getSteps() != null)
                updatedDto.getSteps().forEach(cache::addStep);

            onBulkUpdate.run();
        };

        TestCaseEditMenu.show(selectedItems, onSingleUpdate, onBulkUpdate);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}