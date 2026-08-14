package org.testin.editorPanel.testEditor;

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
import org.testin.editorPanel.AbstractEditorContextMenu;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.statusBar.NextPageAction;
import org.testin.editorPanel.statusBar.PrevPageAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.run.RunTestCaseAction;
import org.testin.testCase.CreateTestCaseAction;
import org.testin.testCase.RemoveTestCaseAction;
import org.testin.testCase.UpdateTestCaseAction;
import org.testin.viewPanel.CloseTestCaseDetailsAction;
import org.testin.viewPanel.ViewDetailsAction;

public class TestEditorContextMenu extends AbstractEditorContextMenu {

    private final @NotNull Project p;

    public TestEditorContextMenu(final @NotNull Project p, final @NotNull IEditor ui, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
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

        addSeparator();

        add(new AutomateTestCaseAction(list));
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
        new CloseTestCaseDetailsAction(list);
    }
}