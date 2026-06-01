package org.testin.editorPanel.testRunEditor;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.testin.editorPanel.BaseCard;
import org.testin.pojo.CardHoverAction;
import org.testin.pojo.RunEditorAttributes;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class RunCard extends BaseCard {
    public static final int ACTIONS_TOTAL_WIDTH = 210;
    private final JBPanel<?> actionPanel = new JBPanel<>();
    private final Map<TestStatus, JBLabel> statusLabels = new EnumMap<>(TestStatus.class);
    private final List<JComponent> badges = new ArrayList<>();
    private final Map<String, String> details = new LinkedHashMap<>();

    public RunCard() {
        super();

        // specific layout for buttons: PASSED, FAILED, BLOCKED
        // todo, to be refactored later
        // todo, i think it can be removed and replaced with menu with shortcuts, like edit menu,
        // this approach is easier and maintainable than the implemented code.
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

        // specific layout for buttons: PASSED, FAILED, BLOCKED
        // todo, to be refactored later
        actionPanel.setVisible(false);
        actionPanel.setBorder(JBUI.Borders.empty());
        this.add(actionPanel, BorderLayout.EAST);
    }

    @Override
    public void applyListFont(final Font listFont) {
        super.applyListFont(listFont);
        float baseSize = listFont.getSize2D();
        for (JBLabel btn : statusLabels.values()) {
            btn.setFont(listFont.deriveFont(Font.BOLD, baseSize));
        }
    }

    public void updateData(final int index, final TestCaseDto tc, final Set<?> activeDetails, final TestRunItems runItem) {
        badges.clear();
        details.clear();

        Arrays.stream(RunEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(runItem, badges, details));

        updateUI(index, RunEditorAttributes.DESCRIPTION.getValueExtractor().apply(runItem), badges, details);
    }

    @Override
    // todo, why it is used by test renderer?
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
        // method to create PASSED, FAILED, BLOCKED
        final JBLabel lbl = new JBLabel(text, SwingConstants.CENTER);
        lbl.setOpaque(false);
        lbl.setBorder(JBUI.Borders.empty());
        return lbl;
    }
}