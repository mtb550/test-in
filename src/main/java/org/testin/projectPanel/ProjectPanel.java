package org.testin.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.testin.actions.CreateProject;
import org.testin.pojo.Config;
import org.testin.projectPanel.projectSelector.TestProjectSelector;
import org.testin.projectPanel.tree.ProjectTree;
import org.testin.projectPanel.tree.TestCaseTreeBuilder;
import org.testin.projectPanel.tree.TestProjectTreeBuilder;
import org.testin.projectPanel.tree.TestRunTreeBuilder;
import org.testin.projectPanel.versionSelector.VersionSelector;
import org.testin.settings.AppSettingsConfigurable;

import java.awt.*;

@Getter
public class ProjectPanel implements Disposable {
    private final JBPanelWithEmptyText panel = new JBPanelWithEmptyText(new BorderLayout());
    private final TestProjectSelector testProjectSelector;
    private final TestProjectTreeBuilder testProjectTreeBuilder;
    private final TestCaseTreeBuilder testCaseTreeBuilder;
    private final TestRunTreeBuilder testRunTreeBuilder;
    private VersionSelector versionSelector;
    private ProjectTree projectTree;

    public ProjectPanel(Project project) {
        System.out.println("ProjectPanel.ProjectPanel()");

        testProjectSelector = new TestProjectSelector(this);
        testProjectTreeBuilder = new TestProjectTreeBuilder(this);
        testCaseTreeBuilder = new TestCaseTreeBuilder(this);
        testRunTreeBuilder = new TestRunTreeBuilder(this);

        boolean status = testProjectSelector.init();

        if (status) {
            System.out.println("ProjectPanel(). projects found");

            panel.setLayout(new BorderLayout());
            JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
            topBar.add(testProjectSelector.getSelectedTestProject(), BorderLayout.NORTH);

            versionSelector = new VersionSelector(testProjectSelector.getSelectedTestProject().getItem());
            topBar.add(versionSelector.getComponent(), BorderLayout.SOUTH);

            panel.add(topBar, BorderLayout.NORTH);

            projectTree = new ProjectTree(this);
            panel.add(projectTree.getComponent(), BorderLayout.CENTER);

        } else {
            System.out.println("ProjectPanel(). not projects found");
            showEmptyState();
        }

    }

    public void setupMainLayout() {
        panel.removeAll();
        panel.getEmptyText().clear();

        boolean status = testProjectSelector.init();

        if (status) {
            System.out.println("ProjectPanel(). projects found");

            panel.setLayout(new BorderLayout());
            JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
            topBar.add(testProjectSelector.getSelectedTestProject(), BorderLayout.NORTH);

            versionSelector = new VersionSelector(testProjectSelector.getSelectedTestProject().getItem());
            topBar.add(versionSelector.getComponent(), BorderLayout.SOUTH);

            panel.add(topBar, BorderLayout.NORTH);


            projectTree = new ProjectTree(this);
            panel.add(projectTree.getComponent(), BorderLayout.CENTER);

        } else {
            System.out.println("ProjectPanel(). not projects found");
            showEmptyState();
        }

        panel.revalidate();
        panel.repaint();
    }

    public void init() {
        //testProjectSelector.loadTestProjectList();
        //testProjectSelector.init();
        //testCaseTabController.init();
        //testRunTabController.init();
        //this = new ProjectPanel(Config.getProject());
    }

    public void showEmptyState() {
        panel.removeAll();
        panel.getEmptyText().clear();
        StatusText emptyText = panel.getEmptyText();

        emptyText.clear();
        emptyText.setText("Welcome to QC plugin", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        emptyText.appendLine("");
        emptyText.appendLine("By", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("Muteb Almughyiri", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("");

        if (Config.getTestinPath() == null)
            emptyText.appendLine(
                    AllIcons.General.Settings,
                    "Configure testin settings",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> ShowSettingsUtil.getInstance().showSettingsDialog(Config.getProject(), AppSettingsConfigurable.class)
            );

        else
            emptyText.appendLine(
                    AllIcons.General.Add,
                    " Create your first test project",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> new CreateProject(this).execute()
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