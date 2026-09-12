package org.testin.editor.run;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.testin.actions.Declared;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.EscapeAction;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.open.OpenContextMenuAction;
import org.testin.report.GenerateReportAction;
import org.testin.testrun.SetTestCaseStatusGroup;

public class RunEditorContextMenu extends AbstractEditorContextMenu {

    private final @NotNull Project p;
    private final @NotNull TestinEditor ui;

    public RunEditorContextMenu(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull DirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        super();
        this.p = p;
        this.ui = ui;

        // One entry per user-settable status: a new TestStatus constant shows up
        // here automatically (#37), which is why the group generates its
        // children from the enum rather than plugin.xml listing them (#119).
        add(Declared.forMenu("Testin.SetTestCaseStatus"));
        addSeparator();
        add(Declared.forMenu("Testin.UpdateRunItem"));
        addSeparator();
        add(Declared.forMenu("Testin.ViewDetails"));
        addSeparator();

        // The same seven the test set editor offers, in the same place, under
        // the same word. Three of them cannot work on a run and say so on the
        // entry rather than being left out - a tester who learns Actions in one
        // editor finds it in the other (#248).
        add(actions(p, ui, dir, list, model));

        addSeparator();
        add(Declared.forMenu("Testin.RunTestCase"));
        add(Declared.forMenu("Testin.NavigateToCode"));

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

        // P, F and B carry no modifier, so they are this list's gesture rather
        // than keys the whole IDE answers - the group puts them here (#119).
        SetTestCaseStatusGroup.bindLettersTo(list);
    }
}