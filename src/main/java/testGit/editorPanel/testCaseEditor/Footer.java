package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Footer extends JBPanel<Footer> {
    private final JBLabel statusLabel = new JBLabel();
    private final JBLabel syncLabel = new JBLabel();

    public Footer() {
        super(new BorderLayout());
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
        setBackground(JBUI.CurrentTheme.EditorTabs.background());
        setPreferredSize(new Dimension(-1, JBUI.scale(28)));

        statusLabel.setFont(JBUI.Fonts.smallFont());
        statusLabel.setForeground(UIUtil.getContextHelpForeground());
        statusLabel.setBorder(JBUI.Borders.emptyLeft(10));

        syncLabel.setFont(JBUI.Fonts.smallFont());
        syncLabel.setForeground(UIUtil.getInactiveTextColor());
        syncLabel.setBorder(JBUI.Borders.emptyRight(10));

        add(statusLabel, BorderLayout.WEST);
        add(syncLabel, BorderLayout.EAST);
    }

    public void updateStatus(int visibleCount, int totalCount) {
        statusLabel.setText(String.format("Showing %d of %d test cases", visibleCount, totalCount));
        syncLabel.setText("Last updated: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }
}