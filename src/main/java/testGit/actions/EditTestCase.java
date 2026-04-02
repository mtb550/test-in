package testGit.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.bulk.BulkEditMenu;
import testGit.ui.single.SingleEditMenu;
import testGit.util.KeyboardSet;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import java.util.List;
import java.util.Set;

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

        UnifiedVirtualFile unifiedFile = null;
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (virtualFile instanceof UnifiedVirtualFile)
            unifiedFile = (UnifiedVirtualFile) virtualFile;

        Set<String> stepCache = unifiedFile != null ? unifiedFile.getUniqueSteps() : null;
        final UnifiedVirtualFile finalUnifiedFile = unifiedFile;

        Runnable onUpdate = () -> {
            list.repaint();

            ViewPanel detailsPanel = ViewToolWindowFactory.getViewPanel();
            if (detailsPanel != null && detailsPanel.getCurrentTestCaseDto() != null) {

                boolean isCurrentAffected = selectedItems.stream()
                        .anyMatch(item -> item.getId().equals(detailsPanel.getCurrentTestCaseDto().getId()));

                if (isCurrentAffected)
                    detailsPanel.refreshCurrentView();
            }
        };

        if (selectedItems.size() == 1) {
            SingleEditMenu.show(selectedItems.getFirst(), updatedDto -> {

                if (updatedDto.getSteps() != null && finalUnifiedFile != null)
                    updatedDto.getSteps().forEach(finalUnifiedFile::addNewStepToCache);

                onUpdate.run();

            }, stepCache);

        } else
            BulkEditMenu.show(selectedItems, onUpdate);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}