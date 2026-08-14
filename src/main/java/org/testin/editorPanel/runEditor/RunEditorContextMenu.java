package org.testin.editorPanel.runEditor;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.EscapeAction;
import org.testin.clipboard.CopyTestCaseAction;
import org.testin.editorPanel.AbstractEditorContextMenu;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.statusBar.NextPageAction;
import org.testin.editorPanel.statusBar.PrevPageAction;
import org.testin.enums.TestStatus;
import org.testin.generateReport.GenerateReportAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.run.RunTestCaseAction;
import org.testin.run.StartExecutionAction;
import org.testin.testRun.SetTestCaseStatusAction;
import org.testin.testRun.UpdateRunItemAction;
import org.testin.viewPanel.CloseTestCaseDetailsAction;
import org.testin.viewPanel.ViewDetailsAction;

public class RunEditorContextMenu extends AbstractEditorContextMenu {

    private final @NotNull Project p;
    private final @NotNull IEditor ui;

    public RunEditorContextMenu(final @NotNull Project p, final @NotNull IEditor ui, final @NotNull DirectoryDto dir, final @NotNull JBList<TestCaseDto> list) {
        super("Run Editor Context Menu", true);
        this.p = p;
        this.ui = ui;

        // One action per user-settable status: a new TestStatus constant shows
        // up here automatically (issue #37).
        for (final TestStatus status : TestStatus.values()) {
            final TestStatus.MenuEntry entry = status.getMenuEntry();
            if (entry != null) add(new SetTestCaseStatusAction(p, ui, list, status, entry));
        }
        addSeparator();
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
        add(new NextPageAction(ui, list));
        add(new PrevPageAction(ui, list));
    }

    @Override
    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull AbstractEditorContextMenu menu) {
        new EscapeAction(p, list);
        new OpenContextMenuAction(p, list, menu);
        new CloseTestCaseDetailsAction(p, list);
        new GenerateReportAction(p, ui, list);
    }
}