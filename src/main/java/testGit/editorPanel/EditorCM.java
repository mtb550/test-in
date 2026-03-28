package testGit.editorPanel;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.actions.*;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.DirectoryDto;

import javax.swing.*;

public class EditorCM extends DefaultActionGroup {

    public EditorCM(final BaseEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Editor Context Menu", true);

        add(new CreateTestCase(ui, dir, list, model));
        add(new ViewDetails(list, dir.getPath()));
        addSeparator();
        add(new EditTestCase(list, dir.getPath()));
        add(new CopyTestCase(list));
        add(new RemoveTestCase(dir, list, model));
        addSeparator();
        add(new GenerateTestCase(list));
        add(new RunTestCase(list));
        add(new NavigateToCode(list));
    }

    public static void registerShortcuts(final BaseEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model, final EditorCM editorCM) {
        new Escape(list);
        new OpenCM(list, editorCM);
        new CreateTestCase(ui, dir, list, model);
        new EditTestCase(list, dir.getPath());
        new RemoveTestCase(dir, list, model);
        new OpenTestCaseDetails(list, dir.getPath());
        new CloseTestCaseDetails(list);
        new CopyTestCaseTitle(list);
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