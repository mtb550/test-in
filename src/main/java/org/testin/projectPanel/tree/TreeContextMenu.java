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

        add(new Open(tree));
        add(new CreateTreeNode(projectPanel, tree));
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
                        new ExportExcel(tree),
                        new ExportJson(tree))
        ));

        add(createSubGroup("Import", AllIcons.ToolbarDecorator.Import,
                List.of(new ImportCsv(tree),
                        new ImportExcel(tree),
                        new ImportJson(tree))
        ));

        addSeparator();

        add(new OpenOldVersions());
        add(new Sync(tree, projectPanel));
        add(new ViewPendingCommits(tree));

        addSeparator();
        add(new SetTestRunStatus(tree));
        addSeparator();

        add(createSubGroup(
                "Generate Report",
                AllIcons.ToolbarDecorator.Export,
                List.of(
                        new GenerateReportHtml(tree),
                        new GenerateReportPdf(tree),
                        new GenerateReportExcel(tree),
                        new GenerateReportJson(tree),
                        new GenerateReportXml(tree)
                )
        ));

    }

    public static void registerShortcuts(final SimpleTree tree, final TreeTransferHandler transferHandler, final TreeContextMenu treeContextMenu) {
        new Escape(tree, transferHandler);
        new OpenCM(tree, treeContextMenu);

    }

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