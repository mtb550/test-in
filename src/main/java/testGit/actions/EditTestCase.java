package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;
import testGit.viewPanel.TestCaseDetailsPanel;
import testGit.viewPanel.ToolWindowFactoryImpl;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;

public class EditTestCase extends DumbAwareAction {

    private final JBList<TestCaseDto> list;
    private final TestCaseDetailsPanel panelContext;

    public EditTestCase(JBList<TestCaseDto> list) {
        super("Edit Test Case");
        this.list = list;
        this.panelContext = null;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), list);
    }

    public EditTestCase(final TestCaseDetailsPanel panelContext, final JComponent targetComponent) {
        super("Edit Test Case");
        this.panelContext = panelContext;
        this.list = null;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), targetComponent);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCaseDto targetDto = null;

        if (list != null) {
            targetDto = list.getSelectedValue();

        } else if (panelContext != null) {
            targetDto = panelContext.getCurrentTestCaseDto();
        }

        if (targetDto == null) return;

        ViewPanel.show(targetDto);

        TestCaseDetailsPanel detailsPanel = ToolWindowFactoryImpl.getDetailsInstance();
        if (detailsPanel != null && !detailsPanel.isEditing()) {
            detailsPanel.toggleEditMode(true);
        }
    }
}