package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.generateReport.GenerateReportAction;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.services.Services;

public class GenerateReportBtn extends AbstractButton implements IToolbarItem {

    public GenerateReportBtn(final @NotNull Project p) {
        super("Export Results", AllIcons.ToolbarDecorator.Export);

        addActionListener(e -> {
            final SimpleTree tree = Services.getInstance(p, ProjectPanel.class).getProjectTree().getMainTree();
            final GenerateReportAction action = new GenerateReportAction(p, tree);

            final AnActionEvent event = AnActionEvent.createEvent(
                    SimpleDataContext.builder().add(CommonDataKeys.PROJECT, p).build(),
                    action.getTemplatePresentation().clone(),
                    "GenerateReportBtn",
                    ActionUiKind.TOOLBAR,
                    null);

            action.update(event);
            if (event.getPresentation().isEnabled()) {
                action.actionPerformed(event);
            }

        });
    }
}