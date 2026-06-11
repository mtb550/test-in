package org.testin.editorPanel;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.testin.pojo.RunEditorAttributes;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class BaseCard extends JBPanel<BaseCard> {
    protected final JBLabel descriptionLabel = new JBLabel();
    protected final JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
    protected final Map<RunEditorAttributes, JBLabel> attributeLabels = new HashMap<>();

    protected final JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
    protected final BorderLayoutPanel wrapper = new BorderLayoutPanel();

    protected boolean isSelected;
    protected boolean isRowHovered;
    protected String hoveredAction;

    public BaseCard() {
        setLayout(new BorderLayout());
        setOpaque(true);

        descriptionLabel.setForeground(UIUtil.getLabelForeground());

        badgePanel.setOpaque(false);
        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JBPanel<?> titleLine = new JBPanel<>();
        titleLine.setLayout(new BoxLayout(titleLine, BoxLayout.X_AXIS));
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLine.add(descriptionLabel);

        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLine);
        content.add(badgePanel);

        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12, 16));
        wrapper.addToCenter(content);

        add(wrapper, BorderLayout.CENTER);
    }

    public void applyListFont(final Font listFont) {
        float baseSize = listFont.getSize2D();

        descriptionLabel.setFont(listFont.deriveFont(Font.BOLD, baseSize + 3.0f));

        for (JBLabel lbl : attributeLabels.values()) {
            lbl.setFont(listFont.deriveFont(baseSize));
        }

        float badgeSize = Math.max(8.0f, baseSize - 2.0f);
        for (Component c : badgePanel.getComponents()) {
            c.setFont(listFont.deriveFont(Font.BOLD, badgeSize));
        }
    }

    protected void updateUI(final int index, final String title, final List<JComponent> badges, final Map<String, String> details) {
        descriptionLabel.setText(String.format(Locale.ENGLISH, "%d. %s", index + 1, title));

        final Color currentRowColor = index % 2 == 0 ? new JBColor(Gray._245, Gray._60) : new JBColor(Gray._230, Gray._45);
        setBackground(currentRowColor);
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        badgePanel.removeAll();
        badges.forEach(badgePanel::add);

        attributeLabels.values().forEach(lbl -> lbl.setVisible(false));

        details.forEach((attrName, value) -> {
            RunEditorAttributes attr = Arrays.stream(RunEditorAttributes.values())
                    .filter(a -> a.getName().equals(attrName))
                    .findFirst()
                    .orElse(null);

            if (attr == null) return;

            JBLabel lbl = attributeLabels.computeIfAbsent(attr, k -> {
                JBLabel newLbl = createDetailLabel();
                content.add(newLbl);
                return newLbl;
            });

            lbl.setText(attrName + ": " + value);
            lbl.setVisible(true);
        });

        badgePanel.revalidate();
        badgePanel.repaint();
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
            final FontMetrics fm = descriptionLabel.getFontMetrics(descriptionLabel.getFont());
            final int titleWidth = fm.stringWidth(descriptionLabel.getText());
            Shared.drawDescriptionActionIcons(this, g, titleWidth, JBUI.scale(12), hoveredAction);
        }
    }

    private JBLabel createDetailLabel() {
        final JBLabel label = new JBLabel();
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}