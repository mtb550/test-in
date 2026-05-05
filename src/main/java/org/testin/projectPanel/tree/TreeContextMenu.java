package org.testin.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.*;
import org.testin.projectPanel.ProjectPanel;

import javax.swing.*;
import java.util.List;

@NoArgsConstructor
public class TreeContextMenu extends DefaultActionGroup {

    public TreeContextMenu(final ProjectPanel projectPanel, final SimpleTree tree) {
        super("Tree Popup Menu", true);

        add(new Open(projectPanel, tree));
        add(new CreateTestNode(projectPanel, tree));
        addSeparator();

        add(createSubGroup("Actions", AllIcons.Actions.Edit,
                List.of(new UndoNode(tree),
                        new RedoNode(tree),
                        new Remove(tree),
                        new Rename(projectPanel, tree),
                        new CopyNode(tree),
                        new CutNode(tree),
                        new PasteNode(tree))
        ));

        addSeparator();
        add(new RunTestSet(tree));
        addSeparator();

        add(createSubGroup("Export", AllIcons.ToolbarDecorator.Export,
                List.of(new ExportCsv(),
                        new ExportHtml(),
                        new ExportExcel(),
                        new ExportJson())
        ));

        add(createSubGroup("Import", AllIcons.ToolbarDecorator.Import,
                List.of(new ImportCsv(),
                        new ImportExcel(tree),
                        new ImportJson())
        ));

        add(createSubGroup("Integrate", AllIcons.Nodes.Related,
                List.of(new IntegrateTestRail(),
                        new IntegrateJira(),
                        new IntegrateAzure())
        ));

        addSeparator();

        add(new OpenOldVersions());
        add(new ViewCommits());
        add(new TestRuns());
        addSeparator();

        add(createSubGroup(
                "Generate Report",
                AllIcons.ToolbarDecorator.Export,
                List.of(
                        new ReportHtml(tree),
                        new ReportPdf(tree),
                        new ReportExcel(tree),
                        new ReportJson(tree),
                        new ReportXml(tree)
                )
        ));

    }

    public static void registerShortcuts(final SimpleTree tree, final TreeTransferHandler transferHandler, final TreeContextMenu treeContextMenu) {
        new Escape(tree, transferHandler);
        new OpenCM(tree, treeContextMenu);

    }

    // TODO: to be removed in all context menus. the below is the new that use list.of()
    private DefaultActionGroup createSubGroup(final String title, final Icon icon, final AnAction... actions) {
        DefaultActionGroup group = new DefaultActionGroup(title, true);
        group.getTemplatePresentation().setIcon(icon);
        for (AnAction action : actions) {
            group.add(action);
        }
        return group;
    }

    // TODO: move it to abstract parent class and put it in util, then make any context menu use it
    private DefaultActionGroup createSubGroup(final String title, final Icon icon, final List<? extends DumbAwareAction> actions) {
        DefaultActionGroup group = new DefaultActionGroup(title, true);
        group.getTemplatePresentation().setIcon(icon);
        for (AnAction action : actions)
            group.add(action);
        return group;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}