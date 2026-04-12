package testGit.editorPanel.testRunEditor;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import testGit.editorPanel.BaseCard;
import testGit.pojo.CardHoverAction;
import testGit.pojo.TestStatus;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class RunCard extends BaseCard<RunCard> {
    public static final int ACTIONS_TOTAL_WIDTH = 210;
    private final JBPanel<?> actionPanel = new JBPanel<>();
    private final Map<TestStatus, JBLabel> statusLabels = new EnumMap<>(TestStatus.class);

    public RunCard() {
        super();
        actionPanel.setLayout(new GridLayout(1, 3, 0, 0));
        actionPanel.setOpaque(false);
        actionPanel.setPreferredSize(new Dimension(JBUI.scale(ACTIONS_TOTAL_WIDTH), 0));

        for (final TestStatus status : TestStatus.values()) {
            if (status != TestStatus.PENDING) {
                final JBLabel btn = createActionLabel(status.name());
                statusLabels.put(status, btn);
                actionPanel.add(btn);
            }
        }

        actionPanel.setVisible(false);
        actionPanel.setBorder(JBUI.Borders.empty());
        this.add(actionPanel, BorderLayout.EAST);
    }

    public void updateData(final int index, final TestCaseDto tc, final Set<String> activeDetails) {
        super.updateBaseData(index, tc, activeDetails);
        badgePanel.revalidate();
        badgePanel.repaint();
    }

    @Override
    public void setActionsState(final boolean isSelected, final boolean isRowHovered, final String hoveredAction) {
        super.setActionsState(isSelected, isRowHovered, hoveredAction);

        if (actionPanel.isVisible() != isSelected) {
            actionPanel.setVisible(isSelected);
        }

        for (final JBLabel label : statusLabels.values()) {
            label.setOpaque(false);
            label.setForeground(JBColor.GRAY);
            label.setBackground(null);
        }

        if (hoveredAction != null) {
            try {
                final TestStatus activeStatus = TestStatus.valueOf(hoveredAction);
                final JBLabel activeLabel = statusLabels.get(activeStatus);

                if (activeLabel != null) {
                    final CardHoverAction actionStyle = activeStatus.getHoverAction();
                    activeLabel.setOpaque(true);
                    activeLabel.setBackground(actionStyle.getBackground());
                    activeLabel.setForeground(actionStyle.getForeground());
                }
            } catch (final IllegalArgumentException ignored) {
            }
        }
    }

    private JBLabel createActionLabel(final String text) {
        final JBLabel lbl = new JBLabel(text, SwingConstants.CENTER);
        lbl.setOpaque(false);
        lbl.setFont(JBFont.regular().asBold());
        lbl.setBorder(JBUI.Borders.empty());
        return lbl;
    }
}