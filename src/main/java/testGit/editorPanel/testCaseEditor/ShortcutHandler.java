package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.*;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;


public class ShortcutHandler {
    public static void register(Directory dir, JBList<TestCase> list, CollectionListModel<TestCase> model) {

        new CreateTestCase(dir, list, model);
        new DeleteTestCase(dir, list, model);
        new OpenTestCaseDetails(list);
        new ShowTestCaseContextMenu(dir, list, model);
        new CloseTestCaseDetails(list);

    }


}