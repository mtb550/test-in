package org.testin.viewPanel.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CardHoverAction;
import org.testin.enums.RunStatus;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.util.Shortcuts;

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
    final @NotNull Icon navIconRaw = AllIcons.Nodes.Class;

    public ActionIcons() {
    }

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final JBPanel<?> actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionsPanel.setOpaque(false);

        final JBLabel navLabel = hoverIcon(navIconRaw,
                CardHoverAction.NAVIGATE_TO_TEST_METHOD.getTooltip(),
                Shortcuts.NavigateToCode.getShortcutText(),
                () -> new NavigateToCodeAction(p, null).execute(p, dto));

        final RunStatus currentStatus = dto.getTempStatus();
        final JBLabel runLabel = hoverIcon(currentStatus.getIcon(),
                currentStatus.getTooltip(),
                Shortcuts.RunTestCase.getShortcutText(),
                () -> currentStatus.executeAction(p, dto, null));

        actionsPanel.add(navLabel);
        actionsPanel.add(Box.createHorizontalStrut(JBUI.scale(STRUT_WIDTH)));
        actionsPanel.add(runLabel);

        return addFullWidthRow(panel, gbc, actionsPanel,
                JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT), currentRow);
    }

    /**
     * An icon label that grows on hover and acts on click. Sized to the hovered
     * icon from the start, so growing it does not reflow the row.
     */
    private @NotNull JBLabel hoverIcon(final @NotNull Icon raw, final @NotNull String tooltip,
                                       final @NotNull String shortcut, final @NotNull Runnable onClick) {
        final JBLabel label = new JBLabel();
        final Icon base = IconUtil.scale(raw, label, BASE_SCALE);
        final Icon hover = IconUtil.scale(raw, label, HOVER_SCALE);
        label.setIcon(base);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        new HelpTooltip()
                .setDescription(HtmlChunk.text(tooltip))
                .setShortcut(shortcut)
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
                onClick.run();
            }
        });

        return label;
    }
}
