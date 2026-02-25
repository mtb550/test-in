package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.util.ShortcutSet;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;

public class CloseTestCaseDetails extends DumbAwareAction {

    public CloseTestCaseDetails(final JComponent component) {
        super("Close View Panel");
        this.registerCustomShortcutSet(ShortcutSet.Escape.get(), component);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Assuming your ViewPanel has a hide/close method.
        // If it's a singleton or static manager:
        ViewPanel.hide();
        ViewPanel.reset();
    }
}