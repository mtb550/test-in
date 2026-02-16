package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.CreateTestCase;
import testGit.actions.DeleteTestCase;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class ShortcutHandler {
    public static void register(Directory dir, JBList<TestCase> list, CollectionListModel<TestCase> model) {
        new CreateTestCase(dir, list, model).registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke("control M")), list);

        new DeleteTestCase(dir, list, model).registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)), list);
    }
}