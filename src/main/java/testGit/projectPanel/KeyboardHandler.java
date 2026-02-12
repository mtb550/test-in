package testGit.projectPanel;


import com.intellij.ui.treeStructure.SimpleTree;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Registers keyboard shortcuts for cut/copy/paste
 */
public class KeyboardHandler {

    public static void register(SimpleTree tree, TransferHandler handler) {
        InputMap inputMap = tree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = tree.getActionMap();

        // Ctrl+X - Cut
        KeyStroke cutKey = KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK);
        inputMap.put(cutKey, "cut");
        actionMap.put("cut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //handler.cut();
                // Ctrl+X - Cut
                actionMap.put("cut", javax.swing.TransferHandler.getCutAction());
            }
        });

        // Ctrl+C - Copy
        KeyStroke copyKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
        inputMap.put(copyKey, "copy");
        actionMap.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //handler.copy();
                // Ctrl+C - Copy
                actionMap.put("copy", javax.swing.TransferHandler.getCopyAction());
            }
        });

        // Ctrl+V - Paste
        KeyStroke pasteKey = KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK);
        inputMap.put(pasteKey, "paste");
        actionMap.put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //handler.paste();
                // Ctrl+V - Paste
                actionMap.put("paste", javax.swing.TransferHandler.getPasteAction());
            }
        });
    }
}
