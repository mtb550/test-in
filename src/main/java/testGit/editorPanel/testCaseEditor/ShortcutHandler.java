package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.*;
import testGit.pojo.Directory;
import testGit.pojo.mappers.TestCaseJsonMapper;


public class ShortcutHandler {
    public static void register(Directory dir, JBList<TestCaseJsonMapper> list, CollectionListModel<TestCaseJsonMapper> model) {

        new CreateTestCase(dir, list, model);
        new RemoveTestCase(dir, list, model);
        new OpenTestCaseDetails(list);
        new ShowTestCaseCM(dir, list, model);
        new CloseTestCaseDetails(list);

    }


}