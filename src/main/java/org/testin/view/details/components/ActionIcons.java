package org.testin.view.details.components;

import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.CardHoverAction;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ActionIcons extends BaseDetails {
    final float BASE_SCALE = 1.3f;
    final float HOVER_SCALE = 1.8f;
    final int STRUT_WIDTH = 8;
    final int INSETS_TOP = 8;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 0;
    final int INSETS_RIGHT = 16;

    public ActionIcons() {
    }

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        // The run slot draws what clicking it does, not how the last run went: a
        // passed case used to show a green tick here, which reads as a verdict
        // and is one - the verdict is a badge now, below.
        final CardHoverAction navigate = CardHoverAction.NAVIGATE_TO_TEST_METHOD;
        final CardHoverAction run = CardHoverAction.runSlot(dto);

        // Neither icon is drawn in an IDE that cannot act on it, and a row with
        // no icons in it is no row at all.
        if (!navigate.isOffered() && !run.isOffered()) return currentRow;

        final JBPanel<?> actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionsPanel.setOpaque(false);

        if (navigate.isOffered()) actionsPanel.add(hoverIcon(navigate, p, dto));

        if (navigate.isOffered() && run.isOffered())
            actionsPanel.add(Box.createHorizontalStrut(JBUI.scale(STRUT_WIDTH)));

        if (run.isOffered()) actionsPanel.add(hoverIcon(run, p, dto));

        return addFullWidthRow(panel, gbc, actionsPanel,
                JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT), currentRow);
    }

    /**
     * One action's icon: a label that grows on hover and does that action's work
     * on click. Sized to the hovered icon from the start, so growing it does not
     * reflow the row.
     * <p>
     * Everything it draws comes off the action itself - the icon, the tooltip,
     * the key it names, and what the click does - so this panel and the cards
     * cannot end up disagreeing about a button they both show.
     */
    private @NotNull JBLabel hoverIcon(final @NotNull CardHoverAction action, final @NotNull Project p, final @NotNull TestCaseDto dto) {
        final JBLabel label = new JBLabel();
        final Icon base = IconUtil.scale(action.getIcon(), label, BASE_SCALE);
        final Icon hover = IconUtil.scale(action.getIcon(), label, HOVER_SCALE);
        label.setIcon(base);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        new HelpTooltip()
                .setDescription(HtmlChunk.text(action.getTooltip()))
                .setShortcut(action.getShortcut().getShortcutText())
                .installOn(label);

        // From the hovered icon itself: scaling 16px by 1.8 gives 28.8, which the
        // icon reports as 29 and the estimate truncated to 28, clipping a pixel.
        label.setPreferredSize(new Dimension(hover.getIconWidth(), hover.getIconHeight()));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                label.setIcon(hover);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                label.setIcon(base);
            }

            @Override
            public void mouseClicked(final MouseEvent e) {
                action.execute(p, dto);
            }
        });

        return label;
    }
}
