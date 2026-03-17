package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.RunTestCase;
import testGit.actions.ViewDetails;
import testGit.pojo.Directory;
import testGit.pojo.mappers.TestCaseJsonMapper;

public class ContextMenu extends DefaultActionGroup {
    public ContextMenu(Directory dir, JBList<TestCaseJsonMapper> list, CollectionListModel<TestCaseJsonMapper> model, TestCaseJsonMapper tc) {
        super("Test Run Actions", false);
        System.out.println("testGit.editorPanel.testRunEditor.ContextMenu()");

        add(new ViewDetails(tc));
        addSeparator();
        add(new RunTestCase(tc, list));


    }
}