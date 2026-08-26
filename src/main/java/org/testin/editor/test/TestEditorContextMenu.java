package org.testin.editor.test;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.EscapeAction;
import org.testin.automate.AutomateTestCaseAction;
import org.testin.clipboard.CopyTestCaseAction;
import org.testin.clipboard.CopyTestCaseNodeAction;
import org.testin.clipboard.CutTestCaseNodeAction;
import org.testin.clipboard.PasteTestCaseNodeAction;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.editor.statusbar.NextPageAction;
import org.testin.editor.statusbar.PrevPageAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.run.RunTestCaseAction;
import org.testin.testcase.CreateTestCaseAction;
import org.testin.testcase.RemoveTestCaseAction;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.util.OptionalPlugin;
import org.testin.view.ViewDetailsAction;

public class TestEditorContextMenu extends AbstractEditorContextMenu {

    private final @NotNull Project p;

    public TestEditorContextMenu(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        super("Test Editor Context Menu", true);
        this.p = p;

        add(new CreateTestCaseAction(p, ui, dir, list));
        add(new ViewDetailsAction(p, list, dir.getPath2()));

        addSeparator();

        add(new UpdateTestCaseAction(p, ui, list, dir.getPath()));
        add(new CopyTestCaseAction(p, list));
        add(new CopyTestCaseNodeAction(p, list));
        add(new CutTestCaseNodeAction(p, ui, list));
        add(new PasteTestCaseNodeAction(p, ui, list));
        add(new RemoveTestCaseAction(p, ui, dir, list, model));

        // Absent, not present and broken: in an IDE without the Java or TestNG
        // plugin these three cannot do anything, and a tester should not find
        // them on the menu to be told so by a balloon (#66).
        if (OptionalPlugin.JAVA.isAvailable() || OptionalPlugin.TESTNG.isAvailable()) {
            addSeparator();

            if (OptionalPlugin.JAVA.isAvailable()) add(new AutomateTestCaseAction(p, list));
            if (OptionalPlugin.TESTNG.isAvailable()) add(new RunTestCaseAction(p, ui, list));
            if (OptionalPlugin.JAVA.isAvailable()) add(new NavigateToCodeAction(p, list));
        }

        addSeparator();

        add(new NextPageAction(ui, list));
        add(new PrevPageAction(ui, list));
    }

    /**
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
    }
}