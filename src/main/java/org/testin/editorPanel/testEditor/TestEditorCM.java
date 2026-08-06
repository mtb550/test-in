package org.testin.editorPanel.testEditor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.Escape;
import org.testin.NavigateToCode;
import org.testin.OpenCM;
import org.testin.ViewDetails;
import org.testin.clipboard.CopyTestCase;
import org.testin.clipboard.CopyTestCaseNode;
import org.testin.clipboard.CutTestCaseNode;
import org.testin.clipboard.PasteTestCaseNode;
import org.testin.editorPanel.EditorContextMenu;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.statusBar.NextPageAction;
import org.testin.editorPanel.statusBar.PrevPageAction;
import org.testin.generateJavaCode.method.GenerateTestMethod;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.run.RunTestCase;
import org.testin.testCase.CreateTestCaseAction;
import org.testin.testCase.RemoveTestCaseAction;
import org.testin.testCase.UpdateTestCaseAction;
import org.testin.viewPanel.CloseTestCaseDetailsAction;

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
    private static IEditor globalSourceEditorUI = null;

    public TestEditorCM(final @NotNull Project project, final @NotNull IEditor ui, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        super("Editor Context Menu", true);

        add(new CreateTestCaseAction(ui, dir, list));
        add(new ViewDetails(list, dir.getPath2()));

        addSeparator();

        add(new UpdateTestCaseAction(ui, list, dir.getPath()));
        add(new CopyTestCase(list));
        add(new CopyTestCaseNode(list));
        add(new CutTestCaseNode(ui, list));
        add(new PasteTestCaseNode(ui, list));
        add(new RemoveTestCaseAction(project, dir, list, model));

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

    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull TestEditorCM testEditorCM) {
        new Escape(list);
        new OpenCM(list, testEditorCM);
        new CloseTestCaseDetailsAction(list);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}