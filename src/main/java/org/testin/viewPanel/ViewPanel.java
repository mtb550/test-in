package org.testin.viewPanel;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.ViewTab;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.FontSyncUtil;
import org.testin.util.broadcasts.listeners.ITestCaseExecutionListener;
import org.testin.viewPanel.details.DetailsTab;
import org.testin.viewPanel.history.HistoryTab;
import org.testin.viewPanel.openBugs.OpenBugsTab;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ViewPanel implements Disposable {
    private final JBPanel<?> detailsTab;
    private final JBPanel<?> historyTab;
    private final JBPanel<?> openBugsTab;

    @Getter
    private final JBScrollPane detailsScrollPane;
    @Getter
    private final JBScrollPane historyScrollPane;
    @Getter
    private final JBScrollPane openBugsScrollPane;

    @Getter
    private final ViewPagination page;

    private final Project project;

    public ViewPanel(final @NotNull Project project) {
        this.project = project;
        Disposer.register(project, this);
        detailsTab = new JBPanel<>(new BorderLayout());
        historyTab = new JBPanel<>(new BorderLayout());
        openBugsTab = new JBPanel<>(new BorderLayout());

        FontSyncUtil.syncWithNativeEditor(project, detailsTab, this);
        FontSyncUtil.syncWithNativeEditor(project, historyTab, this);
        FontSyncUtil.syncWithNativeEditor(project, openBugsTab, this);

        detailsScrollPane = createScrollPane(detailsTab);
        historyScrollPane = createScrollPane(historyTab);
        openBugsScrollPane = createScrollPane(openBugsTab);

        page = new ViewPagination(this);

        refreshCurrentView();

        project.getMessageBus().connect(this).subscribe(ITestCaseExecutionListener.TOPIC, (testName, status, error) -> {
            final TestCaseDto currentDto = getCurrentTestCaseDto();

            if (currentDto != null && testName.contains(currentDto.getDescription())) {
                currentDto.setTempStatus(status);
                currentDto.setTempError(error);
                refreshCurrentView();
            }
        });
    }

    private JBScrollPane createScrollPane(Component view) {
        JBScrollPane sp = new JBScrollPane(view);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.setFocusable(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public void show(final Project project, final List<TestCaseDto> testCases, final ArrayList<String> path) {
        ToolWindow tw = ViewToolWindowFactory.getToolWindow(project);
        if (tw == null || testCases == null || testCases.isEmpty()) return;

        tw.show(() -> {
            this.selectContent(ViewTab.DETAILS);
            this.updateList(testCases, path);
        });
    }

    public void show(final Project project, final List<TestCaseDto> testCases, final ArrayList<String> path, final ViewTab tab) {
        ToolWindow tw = ViewToolWindowFactory.getToolWindow(project);
        if (tw == null) return;

        tw.show(() -> {
            this.selectContent(tab);
            this.updateList(testCases, path);
        });
    }

    public void show(final List<TestCaseDto> testCases, final ArrayList<String> path) {
        this.show(project, testCases, path);
    }

    public ViewPanel hide() {
        ToolWindow tw = ViewToolWindowFactory.getToolWindow(project);
        if (tw != null && tw.isVisible()) {
            tw.hide(null);
        }
        return this;
    }

    public void reset() {
        this.updateList(null, null);
    }

    private void selectContent(final ViewTab tab) {
        ToolWindow tw = ViewToolWindowFactory.getToolWindow(project);
        if (tw == null) return;

        Content[] contents = tw.getContentManager().getContents();
        for (Content content : contents) {
            if (tab.getDisplayName().equals(content.getDisplayName())) {
                tw.getContentManager().setSelectedContent(content);
                break;
            }
        }
    }

    public void hide(final TestCaseDto testCaseDtoToMatch) {
        ToolWindow tw = ViewToolWindowFactory.getToolWindow(project);
        if (tw == null || !tw.isVisible()) return;

        TestCaseDto currentlyShown = this.getCurrentTestCaseDto();

        if (currentlyShown != null && testCaseDtoToMatch != null &&
                currentlyShown.getId().equals(testCaseDtoToMatch.getId())) {
            this.reset();
            this.hide();
        }
    }

    public void updateList(final @Nullable List<TestCaseDto> testCases, final @Nullable ArrayList<String> path) {
        this.page.updateList(testCases, path);
        this.refreshCurrentView();
    }

    public void refreshCurrentView() {
        TestCaseDto currentTestCaseDto = this.getCurrentTestCaseDto();
        ArrayList<String> currentPath = this.page.getCurrentPath();

        new DetailsTab().load(project, detailsTab, currentTestCaseDto, currentPath);
        new HistoryTab().load(historyTab);
        new OpenBugsTab().load(openBugsTab);

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    public TestCaseDto getCurrentTestCaseDto() {
        return page.getCurrentItem();
    }

    public void focusDetailsTab() {
        this.selectContent(ViewTab.DETAILS);
        detailsTab.setFocusable(true);
        detailsTab.requestFocusInWindow();
    }

    @Override
    public void dispose() {
        detailsTab.removeAll();
        historyTab.removeAll();
        openBugsTab.removeAll();
    }
}