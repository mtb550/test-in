package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.*;

public class CloseTestCaseDetails extends DumbAwareAction {

    public CloseTestCaseDetails(JComponent component) {
        super("Close View Panel");
        this.registerCustomShortcutSet(KeyboardSet.Escape.getShortcut(), component);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ViewPanel viewer = ViewToolWindowFactory.getViewPanel();
        if (viewer != null) {
            viewer.hide().reset();
        }
    }
}