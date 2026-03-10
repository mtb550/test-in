package testGit.editorPanel;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class StatusBar extends JBPanel<StatusBar> {
    private final JBLabel statusLabel = new JBLabel();
    private final JBLabel syncLabel = new JBLabel();

    @Getter
    private final JButton firstButton = new JButton("<<");
    @Getter
    private final JButton prevButton = new JButton("<");
    private final JBLabel currentPageLabel = new JBLabel("1:1");
    @Getter
    private final JBTextField pageSizeField = new JBTextField("10", 3);
    @Getter
    private final JButton nextButton = new JButton(">");
    @Getter
    private final JButton lastButton = new JButton(">>");

    public StatusBar() {
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

        JPanel paginationPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, JBUI.scale(5), 0));
        paginationPanel.setOpaque(false);

        pageSizeField.setHorizontalAlignment(SwingConstants.CENTER);
        pageSizeField.setToolTipText("Test cases per page");

        makeCompact(firstButton);
        makeCompact(prevButton);
        makeCompact(nextButton);
        makeCompact(lastButton);

        paginationPanel.add(firstButton);
        paginationPanel.add(prevButton);
        paginationPanel.add(currentPageLabel);
        paginationPanel.add(new JBLabel(" | Per page:"));
        paginationPanel.add(pageSizeField);
        paginationPanel.add(nextButton);
        paginationPanel.add(lastButton);

        add(statusLabel, BorderLayout.WEST);
        add(paginationPanel, BorderLayout.CENTER);
        add(syncLabel, BorderLayout.EAST);
    }

    private void makeCompact(JButton button) {
        button.putClientProperty("ActionToolbar.smallVariant", true);
        button.setMargin(JBUI.insets(0, 4));
        button.setFont(JBUI.Fonts.smallFont());
        button.setFocusable(false);
    }

    public void updatePaginationState(int currentPage, int totalPages, int visibleCount, int totalCount) {
        statusLabel.setText(String.format("Showing %d of %d test cases", visibleCount, totalCount));
        syncLabel.setText("Last updated: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        currentPageLabel.setText("Page " + currentPage + " of " + Math.max(1, totalPages));

        firstButton.setEnabled(currentPage > 1);
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
        lastButton.setEnabled(currentPage < totalPages);
    }

}