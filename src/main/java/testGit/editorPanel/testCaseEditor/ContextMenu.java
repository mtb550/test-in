package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.*;
import testGit.pojo.Directory;
import testGit.pojo.mappers.TestCaseJsonMapper;

public class ContextMenu extends DefaultActionGroup {
    public ContextMenu(Directory dir, JBList<TestCaseJsonMapper> list, CollectionListModel<TestCaseJsonMapper> model, TestCaseJsonMapper tc) {
        super("Test Case Actions", false);

        add(new CreateTestCase(dir, list, model));
        add(new ViewDetails(tc));
        addSeparator();
        add(new UpdateTestCase(tc, list));
        add(new CopyTestCase(tc));
        add(new RemoveTestCase(dir, list, model));
        addSeparator();
        add(new GenerateTestCase(tc, list));
        add(new RunTestCase(tc, list));
        add(new NavigateToCode(tc, list));


    }
}