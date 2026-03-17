package testGit.viewPanel;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import testGit.pojo.Config;
import testGit.pojo.mappers.TestCaseJsonMapper;

import java.util.function.Consumer;

public class ViewPanel {

    public static ToolWindow getToolWindow() {
        return ToolWindowManager.getInstance(Config.getProject()).getToolWindow("Details");
    }

    public static void addTestCase(Consumer<TestCaseJsonMapper> onSaveCallback) {
        ToolWindow tw = getToolWindow();
        if (tw != null) {
            if (!tw.isVisible()) tw.show();

            selectContent(tw, "Create Test Case");

            AddTestCasePanel add = ToolWindowFactoryImpl.getAddInstance();
            if (add != null) {
                add.setOnSaveCallback(onSaveCallback);
            }
        }
    }

    public static void show(TestCaseJsonMapper testCaseJsonMapper) {
        ToolWindow tw = getToolWindow();
        if (tw != null) {
            if (!tw.isVisible())
                tw.show();

            selectContent(tw, "Details");

            TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
            if (viewer != null) {
                viewer.update(testCaseJsonMapper);
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

    private static void selectContent(ToolWindow tw, String displayName) {
        Content[] contents = tw.getContentManager().getContents();
        for (Content content : contents) {
            if (displayName.equals(content.getDisplayName())) {
                tw.getContentManager().setSelectedContent(content);
                break;
            }
        }
    }

    public static void hideIfShowing(TestCaseJsonMapper testCaseJsonMapperToMatch) {
        ToolWindow tw = getToolWindow();
        if (tw == null || !tw.isVisible()) return;

        TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
        if (viewer != null) {
            TestCaseJsonMapper currentlyShown = viewer.getCurrentTestCaseJsonMapper();

            if (currentlyShown != null && testCaseJsonMapperToMatch != null &&
                    currentlyShown.getId().equals(testCaseJsonMapperToMatch.getId())) {
                reset();
                hide();
            }
        }
    }

}