package testGit.viewPanel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.ViewTab;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.Broadcasts.listeners.TestCaseExecutionListener;
import testGit.viewPanel.details.DetailsTab;
import testGit.viewPanel.history.HistoryTab;
import testGit.viewPanel.openBugs.OpenBugsTab;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;

public class ViewPanel {
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

    public ViewPanel() {
        detailsTab = new JBPanel<>(new BorderLayout());
        historyTab = new JBPanel<>(new BorderLayout());
        openBugsTab = new JBPanel<>(new BorderLayout());

        detailsScrollPane = createScrollPane(detailsTab);
        historyScrollPane = createScrollPane(historyTab);
        openBugsScrollPane = createScrollPane(openBugsTab);

        page = new ViewPagination(this);

        refreshCurrentView();

        Config.getProject().getMessageBus().connect().subscribe(TestCaseExecutionListener.TOPIC, new TestCaseExecutionListener() {
            @Override
            public void onStatusChanged(@NotNull final String testName, @NotNull final String status, final String error) {
                final TestCaseDto currentDto = getCurrentTestCaseDto();

                if (currentDto != null && testName.contains(currentDto.getTitle())) {
                    currentDto.setTempStatus(status);
                    currentDto.setTempError(error);
                    refreshCurrentView();
                }
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

    public void show(final Project project, final List<TestCaseDto> testCases, final Path path) {
        ToolWindow tw = ViewToolWindowFactory.getToolWindow(project);
        if (tw == null || testCases == null || testCases.isEmpty()) return;

        tw.show(() -> {
            this.selectContent(ViewTab.DETAILS);
            this.updateList(testCases, path);
        });
    }

    public void show(final Project project, final List<TestCaseDto> testCases, final Path path, final ViewTab tab) {
        ToolWindow tw = ViewToolWindowFactory.getToolWindow(project);
        if (tw == null) return;

        tw.show(() -> {
            this.selectContent(tab);
            this.updateList(testCases, path);
        });
    }

    public void show(final List<TestCaseDto> testCases, final Path path) {
        this.show(Config.getProject(), testCases, path);
    }

    public ViewPanel hide() {
        ToolWindow tw = ViewToolWindowFactory.getToolWindow();
        if (tw != null && tw.isVisible()) {
            tw.hide(null);
        }
        return this;
    }

    public void reset() {
        this.updateList(null, null);
    }

    private void selectContent(final ViewTab tab) {
        ToolWindow tw = ViewToolWindowFactory.getToolWindow();
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
        ToolWindow tw = ViewToolWindowFactory.getToolWindow();
        if (tw == null || !tw.isVisible()) return;

        TestCaseDto currentlyShown = this.getCurrentTestCaseDto();

        if (currentlyShown != null && testCaseDtoToMatch != null &&
                currentlyShown.getId().equals(testCaseDtoToMatch.getId())) {
            this.reset();
            this.hide();
        }
    }

    public void updateList(@Nullable final List<TestCaseDto> testCases, @Nullable final Path path) {
        this.page.updateList(testCases, path);
        this.refreshCurrentView();
    }

    public void refreshCurrentView() {
        TestCaseDto currentTestCaseDto = this.getCurrentTestCaseDto();
        Path currentPath = this.page.getCurrentPath();

        DetailsTab.load(detailsTab, currentTestCaseDto, currentPath);
        HistoryTab.load(historyTab);
        OpenBugsTab.load(openBugsTab);

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

    public void focusHistoryTab() {
        this.selectContent(ViewTab.HISTORY);
        historyTab.setFocusable(true);
        historyTab.requestFocusInWindow();
    }

    public void focusOpenBugsTab() {
        this.selectContent(ViewTab.OPEN_BUGS);
        openBugsTab.setFocusable(true);
        openBugsTab.requestFocusInWindow();
    }
}