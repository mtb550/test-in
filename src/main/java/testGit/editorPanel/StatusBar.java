package testGit.editorPanel;

import com.intellij.ide.HelpTooltip;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class StatusBar extends JBPanel<StatusBar> {
    private final JBLabel statusLabel = new JBLabel();
    private final JBLabel syncLabel = new JBLabel();

    private final Timer clockTimer;

    @Getter
    private final JButton firstButton = new JButton("<<");
    @Getter
    private final JButton prevButton = new JButton("<");
    private final JBLabel currentPageLabel = new JBLabel("1:1");
    @Getter
    private final JBTextField pageSizeField = new JBTextField("50", 3);
    @Getter
    private final JButton nextButton = new JButton(">");
    @Getter
    private final JButton lastButton = new JButton(">>");

    public StatusBar() {
        super(new BorderLayout());

        clockTimer = new Timer(60000, e -> updateClock());
        clockTimer.start();
        updateClock();


        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
        setBackground(JBUI.CurrentTheme.EditorTabs.background());
        setPreferredSize(new Dimension(-1, JBUI.scale(28)));

        statusLabel.setFont(JBUI.Fonts.smallFont());
        statusLabel.setForeground(UIUtil.getContextHelpForeground());
        statusLabel.setBorder(JBUI.Borders.emptyLeft(10));

        syncLabel.setFont(JBUI.Fonts.smallFont());
        syncLabel.setForeground(UIUtil.getInactiveTextColor());
        syncLabel.setBorder(JBUI.Borders.emptyRight(10));

        final JPanel paginationPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, JBUI.scale(5), 0));
        paginationPanel.setOpaque(false);

        pageSizeField.setHorizontalAlignment(SwingConstants.CENTER);
        pageSizeField.setToolTipText("Test cases per page");

        ///makeCompact(firstButton);
        makeCompact(prevButton);
        makeCompact(nextButton);
        ///makeCompact(lastButton);

        new HelpTooltip()
                .setTitle("Previous page")
                .setShortcut(KeyboardSet.PreviousTestCase.getShortcutText())
                .installOn(prevButton);

        new HelpTooltip()
                .setTitle("Next page")
                .setShortcut(KeyboardSet.NextTestCase.getShortcutText())
                .installOn(nextButton);

        ///paginationPanel.add(firstButton);
        paginationPanel.add(prevButton);
        paginationPanel.add(currentPageLabel);
        ///paginationPanel.add(new JBLabel(" | Per page:"));
        paginationPanel.add(pageSizeField);
        paginationPanel.add(nextButton);
        ///paginationPanel.add(lastButton);

        add(statusLabel, BorderLayout.WEST);
        add(paginationPanel, BorderLayout.CENTER);
        add(syncLabel, BorderLayout.EAST);
    }

    private void updateClock() {
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        syncLabel.setText("System Time: " + currentTime);
    }

    private void makeCompact(final JButton button) {
        button.putClientProperty("ActionToolbar.smallVariant", true);
        button.setMargin(JBUI.insets(0, 4));
        button.setFont(JBUI.Fonts.smallFont());
        button.setFocusable(false);
    }

    /// TODO: remove visibleCount if not used later
    public void updatePaginationState(final int currentPage, final int totalPages, final int visibleCount, final int totalCount) {
        ///statusLabel.setText(String.format("Showing %d of %d test cases", visibleCount, totalCount));
        statusLabel.setText(String.format("0 of %d test cases", totalCount));

        currentPageLabel.setText(currentPage + " of " + Math.max(1, totalPages));

        syncLabel.setText("Last updated: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        currentPageLabel.setText(currentPage + " of " + Math.max(1, totalPages));

        firstButton.setEnabled(currentPage > 1);
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
        lastButton.setEnabled(currentPage < totalPages);
    }

    public void updateSelectionState(final int[] selectedIndices, final int currentPage, final int pageSize, final int totalCount) {
        final int selectedCount = selectedIndices.length;

        if (selectedCount > 1) {
            statusLabel.setText(String.format("%d selected of %d test cases", selectedCount, totalCount));

        } else if (selectedCount == 1) {
            final int globalIndex = ((currentPage - 1) * pageSize) + selectedIndices[0];
            statusLabel.setText(String.format("%d of %d test cases", globalIndex + 1, totalCount));

        } else {
            statusLabel.setText(String.format("0 of %d test cases", totalCount));
        }
    }

}