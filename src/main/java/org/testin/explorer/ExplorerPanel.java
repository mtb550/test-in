package org.testin.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.selector.TestProjectSelector;
import org.testin.explorer.tree.ExplorerTree;
import org.testin.explorer.version.BranchSelector;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.setting.SettingsConfigurable;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.awt.*;

@Getter
@Service(Service.Level.PROJECT)
public final class ExplorerPanel implements Disposable {
    private final @NotNull Project p;
    private final @NotNull JBPanelWithEmptyText panel = new JBPanelWithEmptyText(new BorderLayout());
    private final @NotNull TestProjectSelector testProjectSelector;
    private final @NotNull BranchSelector branchSelector;
    private final @NotNull ExplorerTree projectTree;

    public ExplorerPanel(final @NotNull Project p) {
        this.p = p;
        Logger.info("ExplorerPanel.ExplorerPanel()");

        testProjectSelector = new TestProjectSelector(p, this);
        branchSelector = new BranchSelector(p, this, testProjectSelector.getSelectedTestProject().getItem());
        projectTree = new ExplorerTree(p, this);
        Disposer.register(this, projectTree);

        testProjectSelector.init();
    }

    public void setupMainLayout() {
        panel.removeAll();
        panel.getEmptyText().clear();

        if (testProjectSelector.getTestProjectList().getSize() > 0) {
            Logger.info("ExplorerPanel.setupMainLayout(): projects found");

            panel.setLayout(new BorderLayout());
            final JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
            topBar.add(testProjectSelector.getSelectedTestProject(), BorderLayout.NORTH);

            topBar.add(branchSelector.getComponent(), BorderLayout.SOUTH);

            panel.add(topBar, BorderLayout.NORTH);

            panel.add(projectTree.getComponent(), BorderLayout.CENTER);

        } else {
            Logger.info("ExplorerPanel.setupMainLayout(): no projects found");
            showEmptyState();
        }

        panel.revalidate();
        panel.repaint();
    }

    public void showEmptyState() {
        panel.removeAll();
        panel.getEmptyText().clear();
        final StatusText emptyText = panel.getEmptyText();

        emptyText.clear();
        emptyText.setText(String.format("Welcome to %s", Bundle.getPluginName()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        emptyText.appendLine("");
        emptyText.appendSecondaryText("The new awesome test management tool", StatusText.DEFAULT_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("By", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("Muteb almughyiri", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("");

        if (Services.getInstance(p, TestinRoot.class).getPath().toString().isEmpty())
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
                    e -> new CreateTestProjectAction(p, this).execute()
            );

        panel.revalidate();
        panel.repaint();
    }

    @Override
    public void dispose() {
    }
}
