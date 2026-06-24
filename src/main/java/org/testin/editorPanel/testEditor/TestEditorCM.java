package org.testin.editorPanel.testEditor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.*;
import org.testin.editorPanel.EditorContextMenu;
import org.testin.editorPanel.IEditorUI;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TestEditorCM extends EditorContextMenu {
    @Getter
    private static final Set<UUID> globalPendingCutIds = new HashSet<>();

    @Getter
    @Setter
    private static boolean globalCutAction = false;

    @Getter
    @Setter
    private static IEditorUI globalSourceEditorUI = null;

    public TestEditorCM(final Project project, final IEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Editor Context Menu", true);

        add(new CreateTestCase(ui, dir, list, model));
        add(new ViewDetails(list, dir.getPath2()));
        addSeparator();
        add(new UpdateTestCase(ui, list, dir.getPath()));
        add(new CopyTestCase(list));
        add(new CopyTestCaseNode(list));
        add(new CutTestCaseNode(ui, list));
        add(new PasteTestCaseNode(ui, list));
        add(new RemoveTestCase(project, dir, list, model));
        addSeparator();
        add(new GenerateTestMethod(list));
        add(new RunTestCase(list));
        add(new NavigateToCode(list));
        addSeparator();
        add(new NextPageAction(ui, list));
        add(new PrevPageAction(ui, list));
    }

    public static void clearCutState() {
        globalCutAction = false;
        globalPendingCutIds.clear();

        if (globalSourceEditorUI != null && globalSourceEditorUI.getPreferredFocusedComponent() != null)
            globalSourceEditorUI.getPreferredFocusedComponent().repaint();

        globalSourceEditorUI = null;
    }

    public void registerShortcuts(final JBList<TestCaseDto> list, final TestEditorCM testEditorCM) {
        new Escape(list);
        new OpenCM(list, testEditorCM);
        new CloseTestCaseDetails(list);
        new CopyTestCaseDescription(list);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}