package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.*;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;


public class ShortcutHandler {
    public static void register(Directory dir, JBList<TestCase> list, CollectionListModel<TestCase> model) {

        // 1. Create (Ctrl + M)
        new CreateTestCase(dir, list, model);

        // 2. Delete (Delete Key)
        new DeleteTestCase(dir, list, model);

        // 3. Open View Details (Enter Key)
        new OpenTestCaseDetails(list);

        // 4. Context Menu (Menu Key)
        new ShowTestCaseContextMenu(dir, list, model);

        // 5. Close View Panel on ESCAPE
        new CloseTestCaseDetails(list);

    }


}