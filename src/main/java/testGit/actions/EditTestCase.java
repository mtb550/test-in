package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.bulk.BulkEditMenu;
import testGit.util.KeyboardSet;
import testGit.viewPanel.TestCaseDetailsPanel;
import testGit.viewPanel.ToolWindowFactoryImpl;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

public class EditTestCase extends DumbAwareAction {

    private final JBList<TestCaseDto> list;
    private final Path path;
    private final TestCaseDetailsPanel panelContext;

    public EditTestCase(final JBList<TestCaseDto> list, final Path path) {
        super("Edit Test Case");
        this.list = list;
        this.path = path;
        this.panelContext = null;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), list);
    }

    public EditTestCase(final TestCaseDetailsPanel panelContext, final JComponent targetComponent) {
        super("Edit Test Case");
        this.panelContext = panelContext;
        this.list = null;
        this.path = null;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), targetComponent);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (panelContext != null) {
            if (!panelContext.isEditing()) {
                panelContext.toggleEditMode(true);
            }
            return;
        }

        if (list != null) {
            List<TestCaseDto> selectedItems = list.getSelectedValuesList();
            if (selectedItems.isEmpty()) return;

            if (selectedItems.size() > 1) {
                BulkEditMenu.show(selectedItems, list::repaint);
                return;

            }

            // single edit
            /// to be updated. remove view panel edit and use a new ui
            TestCaseDto targetDto = selectedItems.getFirst();
            ViewPanel.show(targetDto, path);

            SwingUtilities.invokeLater(() -> {
                TestCaseDetailsPanel detailsPanel = ToolWindowFactoryImpl.getDetailsInstance();
                if (detailsPanel != null && !detailsPanel.isEditing()) {
                    detailsPanel.toggleEditMode(true);
                }
            });

        }
    }
}