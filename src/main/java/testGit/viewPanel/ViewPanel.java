package testGit.viewPanel;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;

public class ViewPanel {

    public static ToolWindow getToolWindow() {
        return ToolWindowManager.getInstance(Config.getProject()).getToolWindow("Details");
    }

    public static void show(TestCaseDto testCaseDto) {
        ToolWindow tw = getToolWindow();
        if (tw != null) {
            if (!tw.isVisible()) tw.show();

            selectContent(tw);

            TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
            if (viewer != null) {
                viewer.update(testCaseDto);
            }
        }
    }

    public static void hide() {
        ToolWindow tw = getToolWindow();
        if (tw != null && tw.isVisible()) {
            tw.hide(null);
        }
    }

    public static void reset() {
        TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
        if (viewer != null) {
            viewer.update(null);
        }
    }

    private static void selectContent(ToolWindow tw) {
        Content[] contents = tw.getContentManager().getContents();
        for (Content content : contents) {
            if ("Details".equals(content.getDisplayName())) {
                tw.getContentManager().setSelectedContent(content);
                break;
            }
        }
    }

    public static void hideIfShowing(TestCaseDto testCaseDtoToMatch) {
        ToolWindow tw = getToolWindow();
        if (tw == null || !tw.isVisible()) return;

        TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
        if (viewer != null) {
            TestCaseDto currentlyShown = viewer.getCurrentTestCaseDto();

            if (currentlyShown != null && testCaseDtoToMatch != null &&
                    currentlyShown.getId().equals(testCaseDtoToMatch.getId())) {
                reset();
                hide();
            }
        }
    }
}