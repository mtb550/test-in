package org.testin.editorPanel.testEditor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.EscapeAction;
import org.testin.NavigateToCodeAction;
import org.testin.OpenContextMenuAction;
import org.testin.ViewDetailsAction;
import org.testin.automate.AutomateTestCaseAction;
import org.testin.clipboard.CopyTestCaseAction;
import org.testin.clipboard.CopyTestCaseNodeAction;
import org.testin.clipboard.CutTestCaseNodeAction;
import org.testin.clipboard.PasteTestCaseNodeAction;
import org.testin.editorPanel.EditorContextMenu;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.statusBar.NextPageAction;
import org.testin.editorPanel.statusBar.PrevPageAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.run.RunTestCaseAction;
import org.testin.testCase.CreateTestCaseAction;
import org.testin.testCase.RemoveTestCaseAction;
import org.testin.testCase.UpdateTestCaseAction;
import org.testin.viewPanel.CloseTestCaseDetailsAction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TestEditorContextMenu extends EditorContextMenu {
    @Getter
    private static final Set<UUID> globalPendingCutIds = new HashSet<>();

    @Getter
    @Setter
    private static boolean globalCutAction = false;

    @Getter
    @Setter
    private static IEditor globalSourceEditorUI = null;

    public TestEditorContextMenu(final @NotNull Project p, final @NotNull IEditor ui, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        super("Editor Context Menu", true);

        add(new CreateTestCaseAction(p, ui, dir, list));
        add(new ViewDetailsAction(list, dir.getPath2()));

        addSeparator();

        add(new UpdateTestCaseAction(ui, list, dir.getPath()));
        add(new CopyTestCaseAction(list));
        add(new CopyTestCaseNodeAction(list));
        add(new CutTestCaseNodeAction(ui, list));
        add(new PasteTestCaseNodeAction(ui, list));
        add(new RemoveTestCaseAction(p, dir, list, model));

        addSeparator();

        add(new AutomateTestCaseAction(list));
        add(new RunTestCaseAction(list));
        add(new NavigateToCodeAction(list));

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

    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull TestEditorContextMenu testEditorContextMenu) {
        new EscapeAction(list);
        new OpenContextMenuAction(list, testEditorContextMenu);
        new CloseTestCaseDetailsAction(list);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}