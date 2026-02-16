package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.*;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;

public class ContextMenu extends DefaultActionGroup {
    public ContextMenu(Directory dir, JBList<TestCase> list, CollectionListModel<TestCase> model, TestCase tc) {
        super("Test Case Actions", false);

        add(new CopyTestCase(tc));
        add(new GenerateTestCase(tc));
        add(new RunTestCase(tc));
        add(new ViewDetails(tc));

        addSeparator();

        add(new DeleteTestCase(dir, list, model));
        add(new CreateTestCase(dir, list, model));
    }
}