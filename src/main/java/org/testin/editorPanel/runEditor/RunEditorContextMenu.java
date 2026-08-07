package org.testin.editorPanel.runEditor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.EscapeAction;
import org.testin.clipboard.CopyTestCaseAction;
import org.testin.editorPanel.EditorContextMenu;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.statusBar.NextPageAction;
import org.testin.editorPanel.statusBar.PrevPageAction;
import org.testin.generateReport.GenerateReportAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.run.RunTestCaseAction;
import org.testin.run.StartExecutionAction;
import org.testin.testRun.*;
import org.testin.viewPanel.CloseTestCaseDetailsAction;
import org.testin.viewPanel.ViewDetailsAction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RunEditorContextMenu extends EditorContextMenu {
    @Getter
    private static final Set<UUID> globalPendingCutIds = new HashSet<>();
    @Getter
    @Setter
    private static boolean globalCutAction = false;
    @Getter
    @Setter
    private static IEditor globalSourceEditorUI = null;
    private final @NotNull Project p;
    private final IEditor ui;

    public RunEditorContextMenu(final @NotNull Project p, final IEditor ui, final DirectoryDto dir, final JBList<TestCaseDto> list) {
        super("Editor Context Menu", true);
        this.p = p;
        this.ui = ui;

        add(new SetTestCaseStatusPassedAction(p, ui, list));
        add(new SetTestCaseStatusFailedAction(p, ui, list));
        add(new SetTestCaseStatusBlockedAction(p, ui, list));
        addSeparator();
        add(new SetActualResultAction(p, ui, list));
        add(new UpdateRunItemAction(p, ui, list));
        addSeparator();
        add(new ViewDetailsAction(p, list, dir.getPath2()));
        add(new StartExecutionAction(p, ui.getToolBar().getCallbacks()));
        addSeparator();
        add(new CopyTestCaseAction(p, list));
        addSeparator();
        add(new RunTestCaseAction(p, list));
        add(new NavigateToCodeAction(p, list));
        addSeparator();
        add(new NextPageAction(p, ui, list));
        add(new PrevPageAction(p, ui, list));
    }

    // todo: why not used!!
    public static void clearCutState() {
        globalCutAction = false;
        globalPendingCutIds.clear();

        if (globalSourceEditorUI != null && globalSourceEditorUI.getPreferredFocusedComponent() != null)
            globalSourceEditorUI.getPreferredFocusedComponent().repaint();

        globalSourceEditorUI = null;
    }

    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull RunEditorContextMenu cm) {
        new EscapeAction(p, list);
        new OpenContextMenuAction(p, list, cm);
        new CloseTestCaseDetailsAction(p, list);
        new GenerateReportAction(p, ui, list);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}