package testGit.projectPanel.testRunTab;

import com.intellij.ui.treeStructure.SimpleTree;
import testGit.actions.DeletePackage;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class ShortcutHandler {
    public static void register(final SimpleTree tree) {

        new DeletePackage(tree);


        InputMap inputMap = tree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = tree.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), "cut");
        actionMap.put("cut", TransferHandler.getCutAction());

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");
        actionMap.put("copy", TransferHandler.getCopyAction());

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "paste");
        actionMap.put("paste", TransferHandler.getPasteAction());

    }
}
