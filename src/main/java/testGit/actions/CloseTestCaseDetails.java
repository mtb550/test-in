package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.util.KeyboardSet;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;

public class CloseTestCaseDetails extends DumbAwareAction {

    public CloseTestCaseDetails(final JComponent component) {
        super("Close View Panel");
        this.registerCustomShortcutSet(KeyboardSet.Escape.get(), component);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ViewPanel.hide();
        ViewPanel.reset();
    }
}