package testGit.editorPanel;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.actions.*;
import testGit.pojo.mappers.TestCase;
import testGit.pojo.tree.dirs.Directory;

import javax.swing.*;

public class EditorContextMenu extends DefaultActionGroup {
    public EditorContextMenu(final Directory dir, final JBList<TestCase> list, final CollectionListModel<TestCase> model) {
        super("Editor Context Menu", true);

        add(new CreateTestCase(dir, list, model));
        add(new ViewDetails(list));
        addSeparator();
        add(new UpdateTestCase(list));
        add(new CopyTestCase(list));
        add(new RemoveTestCase(dir, list, model));
        addSeparator();
        add(new GenerateTestCase(list));
        add(new RunTestCase(list));
        add(new NavigateToCode(list));
    }

    public static void registerShortcuts(Directory dir, JBList<TestCase> list, CollectionListModel<TestCase> model) {
        //new Escape(tree, transferHandler);
        //new OpenNodeCM(tree, treeContextMenu);
        new CreateTestCase(dir, list, model);
        new RemoveTestCase(dir, list, model);
        new OpenTestCaseDetails(list);
        new ShowTestCaseCM(dir, list, model);
        new CloseTestCaseDetails(list);
    }

    private DefaultActionGroup createSubGroup(final String title, final Icon icon, final AnAction... actions) {
        DefaultActionGroup group = new DefaultActionGroup(title, true);
        group.getTemplatePresentation().setIcon(icon);
        for (AnAction action : actions) {
            group.add(action);
        }
        return group;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}