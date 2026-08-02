package org.testin.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.*;
import org.testin.clipboard.*;
import org.testin.crud.Remove;
import org.testin.enums.ProjectStatus;
import org.testin.generateReport.GenerateReportAction;
import org.testin.importExport.exports.ExportAction;
import org.testin.importExport.imports.ImportAction;
import org.testin.nodeCreator.CreateTreeNode;
import org.testin.projectPanel.ProjectPanel;
import org.testin.run.RunTestSet;
import org.testin.testProject.UpdateTestProjectStatus;
import org.testin.testRun.SetTestRunStatus;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import java.util.List;

public class TreeContextMenu extends DefaultActionGroup {
    final @NotNull Project project;

    public TreeContextMenu(final @NotNull Project project, final @NotNull ProjectPanel projectPanel, final @NotNull SimpleTree tree) {
        super("Tree Popup Menu", true);
        this.project = project;

        add(new Open(tree));
        add(new CreateTreeNode(projectPanel, tree));

        addSeparator();

        add(Services.getInstance(project, Tools.class).createSubGroup("Actions", AllIcons.Actions.Edit,
                List.of(
                        new UpdateTestProjectStatus(tree, ProjectStatus.ACTIVE),
                        new UpdateTestProjectStatus(tree, ProjectStatus.INACTIVE),
                        new UpdateTestProjectStatus(tree, ProjectStatus.ARCHIVED),
                        new UndoNode(tree),
                        new RedoNode(tree),
                        new Remove(tree, projectPanel),
                        new Rename(projectPanel, tree),
                        new CopyNode(tree),
                        new CutNode(tree),
                        new PasteNode(tree))
        ));

        addSeparator();

        add(new RunTestSet(tree));

        addSeparator();

        add(new ExportAction(tree));

        add(new ImportAction(tree));

        addSeparator();

        add(new Sync(tree, projectPanel));
        add(new ViewPendingCommits(tree));

        addSeparator();
        add(new SetTestRunStatus(tree));
        addSeparator();

        add(new GenerateReportAction(tree));

        add(new ShowNodeDetails(tree));

    }

    public void registerShortcuts(final @NotNull SimpleTree tree, final @NotNull TreeTransferHandler transferHandler) {
        new Escape(tree, transferHandler);
        new OpenCM(tree, this);

    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}