package testGit.editorPanel;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.actions.*;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.DirectoryDto;

public class EditorCM extends DefaultActionGroup {

    public EditorCM(final IEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Editor Context Menu", true);

        add(new CreateTestCase(ui, dir.getPath(), list, model));
        add(new ViewDetails(list, dir.getPath()));
        add(new StartExecution(ui.getToolBar().getCallbacks()));
        addSeparator();
        add(new UpdateTestCase(ui, list, dir.getPath()));
        add(new CopyTestCase(list));
        add(new RemoveTestCase(dir, list, model));
        addSeparator();
        add(new GenerateTestCase(list));
        add(new RunTestCase(list));
        add(new NavigateToCode(list));
        addSeparator();
        add(new NextPageAction(ui, list));
        add(new PrevPageAction(ui, list));
    }

    public static void registerShortcuts(final IEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model, final EditorCM editorCM) {
        new Escape(list);
        new OpenCM(list, editorCM);
        new CreateTestCase(ui, dir.getPath(), list, model);
        new UpdateTestCase(ui, list, dir.getPath());
        new RemoveTestCase(dir, list, model);
        new OpenTestCaseDetails(list, dir.getPath());
        new CloseTestCaseDetails(list);
        new CopyTestCaseDescription(list);
        new NextPageAction(ui, list);
        new PrevPageAction(ui, list);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}