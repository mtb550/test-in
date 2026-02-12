package testGit.util;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.ui.treeStructure.SimpleTree;
import testGit.actions.Delete;
import testGit.projectPanel.ProjectPanel;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class ShortcutRegistry {

    public static void Explorer(SimpleTree tree, ProjectPanel projectPanel) {
        registerShortcut(tree, new Delete(projectPanel, null), KeyEvent.VK_DELETE);
        // Add more shortcuts below as needed
        // registerShortcut(tree, new RunFeatureAction(), KeyEvent.VK_F5);
    }

    private static void registerShortcut(SimpleTree component, AnAction action, int keyCode) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, 0);
        action.registerCustomShortcutSet(new CustomShortcutSet(keyStroke), component);
    }
}
