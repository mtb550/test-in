package org.testin.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.nodeCreator.CreateTestProjectAction;
import org.testin.projectPanel.projectSelector.TestProjectSelector;
import org.testin.projectPanel.tree.ProjectTree;
import org.testin.projectPanel.tree.TestCaseTreeBuilder;
import org.testin.projectPanel.tree.TestProjectTreeBuilder;
import org.testin.projectPanel.tree.TestRunTreeBuilder;
import org.testin.projectPanel.versionSelector.BranchSelector;
import org.testin.services.Services;
import org.testin.settings.Setting;
import org.testin.settings.SettingsConfigurable;
import org.testin.util.Bundle;

import java.awt.*;

@Getter
@Service(Service.Level.PROJECT)
public final class ProjectPanel implements Disposable {
    private final @NotNull Project p;
    private final @NotNull JBPanelWithEmptyText panel = new JBPanelWithEmptyText(new BorderLayout());
    private final @NotNull TestProjectSelector testProjectSelector;
    private final @NotNull TestProjectTreeBuilder testProjectTreeBuilder;
    private final @NotNull TestCaseTreeBuilder testCaseTreeBuilder;
    private final @NotNull TestRunTreeBuilder testRunTreeBuilder;
    private @NotNull BranchSelector branchSelector;
    private @NotNull ProjectTree projectTree;

    public ProjectPanel(final @NotNull Project p) {
        this.p = p;
        Logger.info("ProjectPanel.ProjectPanel()");

        testProjectSelector = new TestProjectSelector(p, this);
        testProjectTreeBuilder = new TestProjectTreeBuilder(p, this);
        testCaseTreeBuilder = new TestCaseTreeBuilder(p, this);
        testRunTreeBuilder = new TestRunTreeBuilder(p, this);
        branchSelector = new BranchSelector(p, this, testProjectSelector.getSelectedTestProject().getItem());
        projectTree = new ProjectTree(p, this);

        testProjectSelector.init();
        setupMainLayout();

    }

    public void setupMainLayout() {
        panel.removeAll();
        panel.getEmptyText().clear();

        if (testProjectSelector.getTestProjectList().getSize() > 0) {
            Logger.info("ProjectPanel.setupMainLayout(): projects found");

            panel.setLayout(new BorderLayout());
            JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
            topBar.add(testProjectSelector.getSelectedTestProject(), BorderLayout.NORTH);

            branchSelector = new BranchSelector(p, this, testProjectSelector.getSelectedTestProject().getItem());
            topBar.add(branchSelector.getComponent(), BorderLayout.SOUTH);

            panel.add(topBar, BorderLayout.NORTH);

            projectTree = new ProjectTree(p, this);
            panel.add(projectTree.getComponent(), BorderLayout.CENTER);

        } else {
            Logger.info("ProjectPanel.setupMainLayout(): no projects found");
            showEmptyState();
        }

        panel.revalidate();
        panel.repaint();
    }

    public void showEmptyState() {
        panel.removeAll();
        panel.getEmptyText().clear();
        StatusText emptyText = panel.getEmptyText();

        emptyText.clear();
        emptyText.setText(String.format("Welcome to %s", Bundle.getPluginName()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        emptyText.appendLine("");
        emptyText.appendSecondaryText("The new awesome test management tool", StatusText.DEFAULT_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("By", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("Muteb almughyiri", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("");

        if (Services.getInstance(p, Setting.class).getTestinPath().toString().isEmpty())
            emptyText.appendLine(
                    AllIcons.General.Settings,
                    "Configure Testin settings",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> ShowSettingsUtil.getInstance().showSettingsDialog(p, SettingsConfigurable.class)
            );

        else
            emptyText.appendLine(
                    AllIcons.General.Add,
                    "Create your first test project",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> {
                        final CreateTestProjectAction action = new CreateTestProjectAction(p, this);
                        final AnActionEvent event = AnActionEvent.createEvent(
                                SimpleDataContext.getProjectContext(p),
                                action.getTemplatePresentation().clone(),
                                "createTestProject",
                                ActionUiKind.NONE,
                                null
                        );
                        action.actionPerformed(event);
                    }
            );

        panel.revalidate();
        panel.repaint();
    }

    @Override
    public void dispose() {
        if (projectTree.getMainTree() != null) {
            projectTree.getMainTree().setModel(null);
        }
    }
}