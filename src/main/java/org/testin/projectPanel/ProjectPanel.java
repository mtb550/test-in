package org.testin.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.testin.actions.CreateTestProject;
import org.testin.pojo.Config;
import org.testin.projectPanel.projectSelector.TestProjectSelector;
import org.testin.projectPanel.tree.ProjectTree;
import org.testin.projectPanel.tree.TestCaseTreeBuilder;
import org.testin.projectPanel.tree.TestProjectTreeBuilder;
import org.testin.projectPanel.tree.TestRunTreeBuilder;
import org.testin.projectPanel.versionSelector.BranchSelector;
import org.testin.settings.AppSettingsConfigurable;
import org.testin.util.Bundle;
import org.testin.util.logger.Log;

import java.awt.*;

@Getter
@Service(Service.Level.PROJECT)
public class ProjectPanel implements Disposable {
    private final Project project;
    private final JBPanelWithEmptyText panel = new JBPanelWithEmptyText(new BorderLayout());
    private final TestProjectSelector testProjectSelector;
    private final TestProjectTreeBuilder testProjectTreeBuilder;
    private final TestCaseTreeBuilder testCaseTreeBuilder;
    private final TestRunTreeBuilder testRunTreeBuilder;
    private BranchSelector branchSelector;
    private ProjectTree projectTree;

    public ProjectPanel(Project project) {
        this.project = project;
        Log.info("ProjectPanel.ProjectPanel()");

        testProjectSelector = new TestProjectSelector(this);
        testProjectTreeBuilder = new TestProjectTreeBuilder(this);
        testCaseTreeBuilder = new TestCaseTreeBuilder(this);
        testRunTreeBuilder = new TestRunTreeBuilder(this);

        setupMainLayout();

    }

    public void setupMainLayout() {
        panel.removeAll();
        panel.getEmptyText().clear();

        boolean status = testProjectSelector.init();

        if (status) {
            Log.info("ProjectPanel(). projects found");

            panel.setLayout(new BorderLayout());
            JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
            topBar.add(testProjectSelector.getSelectedTestProject(), BorderLayout.NORTH);

            branchSelector = new BranchSelector(project, this, testProjectSelector.getSelectedTestProject().getItem());
            topBar.add(branchSelector.getComponent(), BorderLayout.SOUTH);

            panel.add(topBar, BorderLayout.NORTH);

            projectTree = new ProjectTree(project, this);
            panel.add(projectTree.getComponent(), BorderLayout.CENTER);

        } else {
            Log.info("ProjectPanel(). not projects found");
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
        emptyText.appendLine("Muteb Almughyiri", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("");

        if (Config.getTestinPath().toString().isEmpty())
            emptyText.appendLine(
                    AllIcons.General.Settings,
                    "Configure Testin settings",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> ShowSettingsUtil.getInstance().showSettingsDialog(project, AppSettingsConfigurable.class)
            );

        else
            emptyText.appendLine(
                    AllIcons.General.Add,
                    " Create your first test project",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> new CreateTestProject(this).execute(project)
            );

        panel.revalidate();
        panel.repaint();
    }

    @Override
    public void dispose() {
        if (projectTree != null && projectTree.getMainTree() != null) {
            projectTree.getMainTree().setModel(null);
        }
    }
}