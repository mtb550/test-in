package org.testin.editorPanel.runEditor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.ui.components.JBList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.*;
import org.testin.clipboard.CopyTestCase;
import org.testin.editorPanel.EditorContextMenu;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.statusBar.NextPageAction;
import org.testin.editorPanel.statusBar.PrevPageAction;
import org.testin.generateReport.GenerateReportAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.run.RunTestCase;
import org.testin.testRun.*;
import org.testin.viewPanel.CloseTestCaseDetailsAction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RunEditorCM extends EditorContextMenu {
    @Getter
    private static final Set<UUID> globalPendingCutIds = new HashSet<>();

    @Getter
    @Setter
    private static boolean globalCutAction = false;

    @Getter
    @Setter
    private static IEditor globalSourceEditorUI = null;

    private final IEditor ui;

    public RunEditorCM(final IEditor ui, final DirectoryDto dir, final JBList<TestCaseDto> list) {
        super("Editor Context Menu", true);
        this.ui = ui;

        add(new SetTestCaseStatusPassed(ui, list));
        add(new SetTestCaseStatusFailed(ui, list));
        add(new SetTestCaseStatusBlocked(ui, list));
        addSeparator();
        add(new SetActualResultAction(ui, list));
        add(new UpdateRunItem(ui, list));
        addSeparator();
        add(new ViewDetails(list, dir.getPath2()));
        add(new StartExecution(ui.getToolBar().getCallbacks()));
        addSeparator();
        add(new CopyTestCase(list));
        addSeparator();
        add(new RunTestCase(list));
        add(new NavigateToCode(list));
        addSeparator();
        add(new NextPageAction(ui, list));
        add(new PrevPageAction(ui, list));
    }

    // todo: why not used!!
    public static void clearCutState() {
        globalCutAction = false;
        globalPendingCutIds.clear();

        if (globalSourceEditorUI != null && globalSourceEditorUI.getPreferredFocusedComponent() != null)
            globalSourceEditorUI.getPreferredFocusedComponent().repaint();

        globalSourceEditorUI = null;
    }

    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull RunEditorCM cm) {
        new Escape(list);
        new OpenCM(list, cm);
        new CloseTestCaseDetailsAction(list);
        new GenerateReportAction(ui, list);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}