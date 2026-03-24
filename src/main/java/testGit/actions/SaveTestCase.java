package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;
import testGit.viewPanel.TestCaseDetailsPanel;

import javax.swing.*;

public class SaveTestCase extends DumbAwareAction {

    private final TestCaseDetailsPanel detailsPanel;

    public SaveTestCase(TestCaseDetailsPanel detailsPanel, JComponent targetComponent) {
        super("Save Test Case");
        this.detailsPanel = detailsPanel;
        this.registerCustomShortcutSet(KeyboardSet.Enter.getShortcut(), targetComponent);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (detailsPanel.isEditing()) {
            detailsPanel.saveChanges();
        }
    }
}