package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.bulk.BulkEditMenu;
import testGit.util.KeyboardSet;
import testGit.viewPanel.ToolWindowFactoryImpl;
import testGit.viewPanel.details.DetailsTab;

import java.util.List;

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

        BulkEditMenu.show(selectedItems, () -> {
            list.repaint();

            DetailsTab detailsPanel = ToolWindowFactoryImpl.getDetailsInstance();
            if (detailsPanel != null && detailsPanel.getCurrentTestCaseDto() != null) {

                boolean isCurrentAffected = selectedItems.stream()
                        .anyMatch(item -> item.getId().equals(detailsPanel.getCurrentTestCaseDto().getId()));

                if (isCurrentAffected) {
                    detailsPanel.update(detailsPanel.getCurrentTestCaseDto(), detailsPanel.getCurrentPath());
                }
            }
        });
    }
}