package testGit.editorPanel;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import testGit.pojo.TestCaseAttributes;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class BaseCard<T extends JBPanel<T>> extends JBPanel<T> {
    protected static final int CARD_HEIGHT = 130;

    protected final JBLabel titleLabel = new JBLabel();
    protected final JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));

    protected final Map<TestCaseAttributes, JBLabel> attributeLabels = new EnumMap<>(TestCaseAttributes.class);

    protected final JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
    protected final BorderLayoutPanel wrapper = new BorderLayoutPanel();

    protected boolean isSelected;
    protected boolean isRowHovered;
    protected String hoveredAction;

    public BaseCard() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(CARD_HEIGHT)));

        titleLabel.setFont(JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f));
        titleLabel.setForeground(UIUtil.getLabelForeground());
        badgePanel.setOpaque(false);

        final JBPanel<?> titleLine = new JBPanel<>();
        titleLine.setLayout(new BoxLayout(titleLine, BoxLayout.X_AXIS));
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLine.add(titleLabel);

        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(titleLine);
        content.add(badgePanel);

        for (TestCaseAttributes attr : TestCaseAttributes.values()) {
            /// TODO: if statement here to be removed as all will title will shown and use preferrences that store others shown attributes.
            /// backlog: put title in tool bar details options and make checked and enabled.
            if (attr == TestCaseAttributes.TITLE || attr == TestCaseAttributes.PRIORITY || attr == TestCaseAttributes.GROUPS) {
                continue;
            }
            final JBLabel label = createDetailLabel();
            attributeLabels.put(attr, label);
            content.add(label);
        }

        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12, 16));
        wrapper.addToCenter(content);
        add(wrapper, BorderLayout.CENTER);
    }

    protected void updateBaseData(final int index, final TestCaseDto tc, final Set<String> activeDetails) {
        titleLabel.setText(String.format("%d. %s", index + 1, tc.getTitle()));

        /// TODO: put backgroound color for odd and even in Config and use it in all project to unify the UI.
        final Color currentRowColor = index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45);
        setBackground(currentRowColor);
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        for (TestCaseAttributes attr : TestCaseAttributes.values()) {
            if (attr == TestCaseAttributes.TITLE || attr == TestCaseAttributes.PRIORITY || attr == TestCaseAttributes.GROUPS) {
                continue;
            }

            final JBLabel lbl = attributeLabels.get(attr);
            if (lbl != null) {
                final boolean isVisible = activeDetails.contains(attr.name());
                lbl.setVisible(isVisible);

                if (isVisible) {
                    final String value = attr.getValue(tc);
                    lbl.setText(attr.getDisplayName() + ": " + (value != null ? value : ""));
                }
            }
        }

        badgePanel.removeAll();

        if (activeDetails.contains(TestCaseAttributes.PRIORITY.name())) {
            badgePanel.add(Shared.createPriorityBadge(tc));
        }

        if (activeDetails.contains(TestCaseAttributes.GROUPS.name())) {
            Optional.ofNullable(tc.getGroups())
                    .ifPresent(groups -> groups.forEach(groupName -> badgePanel.add(Shared.createGroupBadge(groupName))));
        }
    }

    public void setActionsState(final boolean isSelected, final boolean isRowHovered, final String hoveredAction) {
        this.isSelected = isSelected;
        this.isRowHovered = isRowHovered;
        this.hoveredAction = hoveredAction;
    }

    @Override
    protected void paintChildren(final Graphics g) {
        super.paintChildren(g);
        if (isRowHovered) {
            final FontMetrics fm = titleLabel.getFontMetrics(titleLabel.getFont());
            final int titleWidth = fm.stringWidth(titleLabel.getText());
            Shared.drawTitleActionIcons(this, g, titleWidth, JBUI.scale(12), hoveredAction);
        }
    }

    protected JBLabel createDetailLabel() {
        final JBLabel label = new JBLabel();
        label.setFont(UIUtil.getLabelFont(UIUtil.FontSize.NORMAL));
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}