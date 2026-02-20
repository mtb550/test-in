package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class CloseTestCaseDetails extends DumbAwareAction {

    public CloseTestCaseDetails() {
        super("Close View Panel");
    }

    /**
     * Registers the Escape key to close the ViewPanel.
     * We attach it to the list so that if the list has focus and the user hits Esc, the panel closes.
     */
    public static void register(JComponent component) {
        CloseTestCaseDetails action = new CloseTestCaseDetails();
        action.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)),
                component
        );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Assuming your ViewPanel has a hide/close method.
        // If it's a singleton or static manager:
        ViewPanel.hide();
        ViewPanel.reset();
    }
}