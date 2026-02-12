package testGit.editorPanel;

import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.AddTestCaseAction;
import testGit.actions.DeleteTestCaseAction;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class ShortcutHandler {
    public static void register(final String featurePath, final JBList<TestCase> list, final CollectionListModel<TestCase> model) {

        // Add
        KeyStroke ctrlM = KeyStroke.getKeyStroke("control M");
        AddTestCaseAction addAction = new AddTestCaseAction(featurePath, list, model);
        addAction.registerCustomShortcutSet(new CustomShortcutSet(ctrlM), list);

        // Delete
        KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        DeleteTestCaseAction deleteAction = new DeleteTestCaseAction(featurePath, list, model);
        deleteAction.registerCustomShortcutSet(new CustomShortcutSet(deleteKey), list);
    }
}