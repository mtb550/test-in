package org.testin.editor.run;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.testin.actions.Declared;
import org.jetbrains.annotations.NotNull;
import org.testin.EscapeAction;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.open.OpenContextMenuAction;
import org.testin.report.GenerateReportAction;
import org.testin.testrun.SetTestCaseStatusAction;

import java.util.Arrays;

public class RunEditorContextMenu extends AbstractEditorContextMenu {

    private final @NotNull Project p;
    private final @NotNull TestinEditor ui;

    public RunEditorContextMenu(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull DirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        super("Run Editor Context Menu", true);
        this.p = p;
        this.ui = ui;

        // One action per user-settable status: a new TestStatus constant shows
        // up here automatically (issue #37).
        Arrays.stream(TestStatus.values())
                .filter(TestStatus::isVerdict)
                .forEach(status -> add(new SetTestCaseStatusAction(p, ui, list, status, status.getMenuEntry())));
        addSeparator();
        add(Declared.action("Testin.UpdateRunItem"));
        addSeparator();
        add(Declared.action("Testin.ViewDetails"));
        addSeparator();

        // The same seven the test set editor offers, in the same place, under
        // the same word. Three of them cannot work on a run and say so on the
        // entry rather than being left out - a tester who learns Actions in one
        // editor finds it in the other (#248).
        add(actions(p, ui, dir, list, model));

        addSeparator();
        add(Declared.action("Testin.RunTestCase"));
        add(Declared.action("Testin.NavigateToCode"));

        // No Start Manual Execution here. Every entry in this menu acts on the
        // cases the tester highlighted, and that one ignores them - it walks the
        // run from its first pending case whatever is selected. It belongs on
        // the toolbar, where it is a press about the run as a whole rather than
        // an answer to a right-click on a case.

    }

    /**
     * UC-EDITOR-PANEL-026, UC-EDITOR-PANEL-029.
     * <p>
     * Each of these registers its own shortcut on the list from its constructor,
     * so the action object is not needed afterward and is deliberately
     * discarded. It reads like a mistake and is not one — the alternative is a
     * factory method per action whose only job is to return something for the
     * caller to ignore.
     */
    @Override
    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull AbstractEditorContextMenu menu) {
        new EscapeAction(p, list);
        new OpenContextMenuAction(list, menu);
        new GenerateReportAction(p, ui, list);
    }
}