package org.testin.editor.run;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.EscapeAction;
import org.testin.clipboard.CopyTestCaseAction;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.editor.statusbar.NextPageAction;
import org.testin.editor.statusbar.PrevPageAction;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.report.GenerateReportAction;
import org.testin.run.RunTestCaseAction;
import org.testin.run.StartExecutionAction;
import org.testin.testrun.SetTestCaseStatusAction;
import org.testin.testrun.UpdateRunItemAction;
import org.testin.view.CloseTestCaseDetailsAction;
import org.testin.view.ViewDetailsAction;

public class RunEditorContextMenu extends AbstractEditorContextMenu {

    private final @NotNull Project p;
    private final @NotNull TestinEditor ui;

    public RunEditorContextMenu(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull DirectoryDto dir, final @NotNull JBList<TestCaseDto> list) {
        super("Run Editor Context Menu", true);
        this.p = p;
        this.ui = ui;

        // One action per user-settable status: a new TestStatus constant shows
        // up here automatically (issue #37).
        for (final TestStatus status : TestStatus.values()) {
            if (status.isAddedToMenu()) add(new SetTestCaseStatusAction(p, ui, list, status, status.getMenuEntry()));
        }
        addSeparator();
        add(new UpdateRunItemAction(p, ui, list));
        addSeparator();
        add(new ViewDetailsAction(p, list, dir.getPath2()));
        add(new StartExecutionAction(ui.getToolBar().getCallbacks()));
        addSeparator();
        add(new CopyTestCaseAction(p, list));
        addSeparator();
        add(new RunTestCaseAction(p, list));
        add(new NavigateToCodeAction(p, list));
        addSeparator();
        add(new NextPageAction(ui, list));
        add(new PrevPageAction(ui, list));
    }

    /**
     * Each of these registers its own shortcut on the list from its constructor,
     * so the action object is not needed afterwards and is deliberately
     * discarded. It reads like a mistake and is not one — the alternative is a
     * factory method per action whose only job is to return something for the
     * caller to ignore.
     */
    @Override
    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull AbstractEditorContextMenu menu) {
        new EscapeAction(p, list);
        new OpenContextMenuAction(list, menu);
        new CloseTestCaseDetailsAction(list);
        new GenerateReportAction(p, ui, list);
    }
}