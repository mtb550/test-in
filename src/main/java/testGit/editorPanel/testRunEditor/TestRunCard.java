package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import lombok.Setter;
import testGit.actions.RunTestCase;
import testGit.actions.ViewDetails;
import testGit.editorPanel.Shared;
import testGit.pojo.GroupType;
import testGit.pojo.TestCase;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;

public class TestRunCard extends JBPanel<TestRunCard> {

    private static final int CARD_HEIGHT = 130;
    private static final int BORDER_THICKNESS = 1;
    final TestCase tc;
    private final JBLabel titleLabel = new JBLabel();
    private final JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
    private final JBLabel expectedLabel = createDetailLabel();
    private final JBLabel stepsLabel = createDetailLabel();
    private final JBLabel automationRefLabel = createDetailLabel();
    private final JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 8));
    private final Color defaultBackground;
    private final Color selectedBackground;
    private final Border defaultBorder;
    private final Border selectedBorder;
    private boolean isSelected = false;
    @Setter
    private SelectionListener selectionListener;

    public TestRunCard(int index, TestCase tc) {
        super(new BorderLayout());
        this.tc = tc;

        setOpaque(true);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(CARD_HEIGHT)));
        setPreferredSize(new Dimension(100, JBUI.scale(CARD_HEIGHT)));

        updateBackgrounds(index);
        defaultBackground = getBackground();
        selectedBackground = new JBColor(
                new Color(defaultBackground.getRed(), defaultBackground.getGreen(), defaultBackground.getBlue(), 220),
                new Color(defaultBackground.getRed(), defaultBackground.getGreen(), defaultBackground.getBlue(), 220)
        );
        defaultBorder = JBUI.Borders.empty(BORDER_THICKNESS);
        selectedBorder = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(new JBColor(new Color(0, 120, 215), new Color(75, 110, 175)), BORDER_THICKNESS),
                JBUI.Borders.empty()
        );

        titleLabel.setFont(JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f));
        titleLabel.setForeground(JBColor.namedColor("Label.foreground", UIUtil.getLabelForeground()));
        badgePanel.setOpaque(false);

        BorderLayoutPanel titleLine = new BorderLayoutPanel();
        titleLine.setOpaque(false);
        titleLine.addToLeft(titleLabel);
        titleLine.addToCenter(badgePanel);

        JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLine);
        content.add(expectedLabel);
        content.add(stepsLabel);
        content.add(automationRefLabel);

        styleActionButtons();

        BorderLayoutPanel wrapper = new BorderLayoutPanel();
        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12, 16));
        wrapper.addToCenter(content);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new OverlayLayout(layeredPane));
        layeredPane.add(wrapper, JLayeredPane.DRAG_LAYER);
        layeredPane.add(actionButtonPanel);

        add(layeredPane, BorderLayout.CENTER);
        setBorder(defaultBorder);

        setupClickListener();
        addMouseListener(createContextMenuListener());
    }

    public void deselect() {
        isSelected = false;
        actionButtonPanel.setVisible(false);
        setBackground(defaultBackground);
        setBorder(defaultBorder);
        repaint();
    }

    /**
     * Updates the card's displayed content, driven by the current EditorHeader state.
     *
     * @param index         absolute index across all pages (used for the numbered title)
     * @param tc            the test case to render
     * @param showGroups    whether group badges should be visible
     * @param showPriority  whether the priority badge should be visible
     * @param activeDetails which detail rows are visible ("Expected Result", "Steps", "Automation Ref")
     */
    public void updateData(int index, TestCase tc,
                           boolean showGroups, boolean showPriority,
                           Set<String> activeDetails) {
        titleLabel.setText(String.format("%d. %s", index + 1, tc.getTitle()));

        expectedLabel.setText("Expected Result: " + tc.getExpected());
        stepsLabel.setText("Steps: " + tc.getSteps());
        automationRefLabel.setText("Automation Reference: " + tc.getAutoRef());

        expectedLabel.setVisible(activeDetails.contains("Expected Result"));
        stepsLabel.setVisible(activeDetails.contains("Steps"));
        automationRefLabel.setVisible(activeDetails.contains("Automation Ref"));

        badgePanel.removeAll();
        if (showPriority) badgePanel.add(Shared.createPriorityBadge(tc));
        if (showGroups && tc.getGroups() != null)
            for (GroupType group : tc.getGroups())
                badgePanel.add(Shared.createGroupBadge(group));
        badgePanel.revalidate();
        badgePanel.repaint();

        updateBackgrounds(index);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        Dimension btn = actionButtonPanel.getPreferredSize();
        actionButtonPanel.setBounds(
                getWidth() - btn.width - JBUI.scale(8),
                getHeight() - btn.height - JBUI.scale(8),
                btn.width, btn.height
        );
    }

    private void setupClickListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ViewPanel.show(tc);
                    return;
                }
                isSelected = !isSelected;
                if (isSelected) {
                    select();
                    if (selectionListener != null) selectionListener.onSelected(TestRunCard.this);
                } else {
                    deselect();
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private MouseListener createContextMenuListener() {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }

            private void showContextMenu(MouseEvent e) {
                DefaultActionGroup group = new DefaultActionGroup("Test Run Actions", false);
                group.add(new ViewDetails(tc));
                group.addSeparator();
                group.add(new RunTestCase(tc, null));
                ActionManager.getInstance()
                        .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group)
                        .getComponent().show(e.getComponent(), e.getX(), e.getY());
            }
        };
    }

    private void select() {
        actionButtonPanel.setVisible(true);
        setBackground(selectedBackground);
        setBorder(selectedBorder);
        repaint();
    }

    private void styleActionButtons() {
        actionButtonPanel.setOpaque(false);
        actionButtonPanel.setVisible(false);
        actionButtonPanel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.NORMAL));
        actionButtonPanel.add(createStatusButton("PASSED",
                new JBColor(new Color(39, 174, 96, 150), new Color(46, 125, 50, 150))));
        actionButtonPanel.add(createStatusButton("FAILED",
                new JBColor(new Color(192, 57, 43, 150), new Color(183, 28, 28, 150))));
        actionButtonPanel.add(createStatusButton("BLOCKED",
                new JBColor(new Color(243, 156, 18, 150), new Color(237, 108, 2, 150))));
    }

    private JButton createStatusButton(String status, Color bg) {
        JButton btn = new JButton(status);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("JButton.backgroundColor", bg);
        btn.setBackground(bg);
        btn.setForeground(JBColor.WHITE);
        btn.setFont(JBFont.regular().asBold());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorder(JBUI.Borders.empty(6, 16));
        btn.addActionListener(e -> System.out.println("Test Case [" + tc.getTitle() + "] updated to: " + status));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.brighter());
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
                btn.repaint();
            }
        });
        return btn;
    }

    private void updateBackgrounds(int index) {
        setBackground(index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45));
    }

    private JBLabel createDetailLabel() {
        JBLabel label = new JBLabel();
        label.setFont(UIUtil.getLabelFont(UIUtil.FontSize.NORMAL));
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public interface SelectionListener {
        void onSelected(TestRunCard card);
    }
}
