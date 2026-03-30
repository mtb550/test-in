package testGit.viewPanel.openBugs;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class OpenBugsTab {
    public static void load(@NotNull JBPanel<?> bugTab) {
        bugTab.removeAll();
        bugTab.add(new JBLabel("No bugs found for this test case."), BorderLayout.NORTH);
    }
}