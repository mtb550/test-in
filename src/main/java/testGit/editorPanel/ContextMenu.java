package testGit.editorPanel;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.editorPanel.*;
import testGit.pojo.TestCase;

public class ContextMenu extends DefaultActionGroup {

    public ContextMenu(final String featurePath, final JBList<TestCase> list, CollectionListModel<TestCase> model, TestCase tc) {
        super("Test Case Actions", false);

        add(new CopyTestCaseAction(tc));
        add(new RunTestCaseAction(tc));
        add(new ViewDetailsAction(tc));

        addSeparator();

        add(new DeleteTestCaseAction(featurePath, list, model));
        add(new AddTestCaseAction(featurePath, list, model));

    }

}
