package testGit.projectPanel.testCaseTab;

import com.intellij.ui.treeStructure.SimpleTree;
import testGit.actions.DeletePackage;
import testGit.actions.Escape;
import testGit.actions.OpenTestSet;
import testGit.actions.Rename;
import testGit.projectPanel.ProjectPanel;
import testGit.projectPanel.TransferHandlerImpl;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class ShortcutHandler {
    public static void register(final ProjectPanel projectPanel, final SimpleTree tree, TransferHandlerImpl transferHandler) {

        // Delete package (VK_DELETE)
        new DeletePackage(tree);

        // Open Test Set
        new OpenTestSet(tree);

        // Rename
        new Rename(projectPanel, tree);

        // escape
        new Escape(tree, transferHandler);

        // 2. Map standard keystrokes to TransferHandler actions
        InputMap inputMap = tree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = tree.getActionMap();

        // Simply map the keystrokes to the existing TransferHandler actions
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), "cut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "paste");
        //inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearClipboard");

        /*
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undoAction");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redoAction");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), "addNewNode");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "showContextMenu"); //TODO:: not working
*/

        actionMap.put("cut", TransferHandler.getCutAction());
        actionMap.put("copy", TransferHandler.getCopyAction());
        actionMap.put("paste", TransferHandler.getPasteAction());


    }
}
