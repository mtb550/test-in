package com.example.util;

import com.example.explorer.ExplorerContextMenu.DeleteNodeAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CustomShortcutSet;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class ShortcutRegistry {

    public static void Explorer(JTree tree) {
        registerShortcut(tree, new DeleteNodeAction(), KeyEvent.VK_DELETE);
        // Add more shortcuts below as needed
        // registerShortcut(tree, new RunFeatureAction(), KeyEvent.VK_F5);
    }

    private static void registerShortcut(JComponent component, AnAction action, int keyCode) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, 0);
        action.registerCustomShortcutSet(new CustomShortcutSet(keyStroke), component);
    }
}
