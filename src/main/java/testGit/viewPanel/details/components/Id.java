package testGit.viewPanel.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Id extends BaseDetails {

    private static final int BADGE_ARC_SIZE = 16;
    private static final int BADGE_BORDER_V = 3;
    private static final int BADGE_BORDER_H = 10;
    private static final int FLOW_GAP = 8;
    private static final int COPY_SUCCESS_DELAY_MS = 1500;
    private static final String COPY_TOOLTIP = "Copy ID";
    private static final Color BG_COLOR = new JBColor(Gray._230, Gray._80);
    private static final Color FG_COLOR = new JBColor(Gray._130, Gray._170);
    private static final int INSETS_TOP = 5;
    private static final int INSETS_LEFT = 16;
    private static final int INSETS_BOTTOM = 0;
    private static final int INSETS_RIGHT = 16;

    @Override
    public int render(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow) {
        JBLabel idBadge = new JBLabel(dto.getId().toString()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BADGE_ARC_SIZE, BADGE_ARC_SIZE);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        idBadge.setFont(JBUI.Fonts.smallFont());
        idBadge.setForeground(FG_COLOR);
        idBadge.setBorder(JBUI.Borders.empty(BADGE_BORDER_V, BADGE_BORDER_H));
        idBadge.setOpaque(false);

        JBLabel copyIcon = new JBLabel(AllIcons.Actions.Copy);
        copyIcon.setToolTipText(COPY_TOOLTIP);
        copyIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        copyIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                CopyPasteManager.getInstance().setContents(new StringSelection(dto.getId().toString()));
                copyIcon.setIcon(AllIcons.General.InspectionsOK);
                Timer timer = new Timer(COPY_SUCCESS_DELAY_MS, evt -> copyIcon.setIcon(AllIcons.Actions.Copy));
                timer.setRepeats(false);
                timer.start();
            }
        });

        JPanel idContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(FLOW_GAP), 0));
        idContainer.setOpaque(false);
        idContainer.add(idBadge);
        idContainer.add(copyIcon);

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT);

        panel.add(idContainer, gbc);
        return currentRow + 1;
    }
}