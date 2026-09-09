package org.testin.view.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.search.GoTo;
import org.testin.search.Hit;
import org.testin.ui.FontSync;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.nio.file.Path;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class NavigationBar extends BaseDetails {

    final @NotNull Color DEFAULT_TEXT_COLOR = Gray._120;
    final int SEPARATOR_BORDER_V = 0;
    final int SEPARATOR_BORDER_H = 6;
    final int PANEL_BORDER_TOP = 10;
    final int PANEL_BORDER_LEFT = 16;
    final int PANEL_BORDER_BOTTOM = 5;
    final int PANEL_BORDER_RIGHT = 0;
    final int GBC_INSETS_TOP = 12;
    final int GBC_INSETS_LEFT = 16;
    final int GBC_INSETS_BOTTOM = 0;
    final int GBC_INSETS_RIGHT = 16;

    /**
     * The folders above the case, and empty when the caller had none to give -
     * the bar then draws the test set alone.
     */
    private final @NotNull List<String> currentPath;

    // UC-VIEW-PANEL-010, Rule-VIEW-PANEL-041
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull JBPanel<?> pathPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pathPanel.setOpaque(false);

        final float navFontSize = Math.max(8.0f, FontSync.getBaseFontSize() - 1.0f);

        {
            for (int i = 0; i < currentPath.size(); i++) {

                final @NotNull String labelText = currentPath.get(i);
                final boolean isLast = (i == currentPath.size() - 1);

                // Captured: the listener below runs long after the loop has ended.
                final int index = i;

                final @NotNull JBLabel folderLabel = new JBLabel(labelText);
                folderLabel.setFont(JBUI.Fonts.label(navFontSize));
                folderLabel.setForeground(DEFAULT_TEXT_COLOR);
                folderLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                folderLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(final MouseEvent e) {
                        folderLabel.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
                        setUnderline(folderLabel, true);
                    }

                    @Override
                    public void mouseExited(final MouseEvent e) {
                        folderLabel.setForeground(DEFAULT_TEXT_COLOR);
                        setUnderline(folderLabel, false);
                    }

                    /**
                     * UC-VIEW-PANEL-010, Rule-VIEW-PANEL-042, Rule-VIEW-PANEL-043.
                     * <p>
                     * Every step goes where it says, not only the last one.
                     * <p>
                     * All of them took a hand pointer, turned the link colour and
                     * underlined themselves, and only the last did anything -
                     * clicking Test Cases to go up a level did nothing and said
                     * nothing (#228). The steps above the last are the test
                     * project, the Test Cases folder and the packages, and none
                     * of those has an editor to open, so what going to one means
                     * is the tree going there.
                     * <p>
                     * Which is what {@code GoTo} already does, for the global
                     * search: bring the tool window up, expand to the node, and
                     * open its editor if it has one. A node that has none is
                     * refused there rather than guessed at, so the last step
                     * opens a test set and a step above it simply reveals - one
                     * call, and this bar does not have to know which kind it is
                     * looking at.
                     */
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        final @NotNull Path stepPath = Services.getInstance(p, TestinRoot.class).resolve(currentPath.subList(0, index + 1));

                        Services.getInstance(p, ProjectIndexer.class).find(stepPath).map(Hit::of).ifPresent(hit -> GoTo.the(p, hit));
                    }
                });

                pathPanel.add(folderLabel);
                if (!isLast) {
                    final @NotNull JBLabel separator = new JBLabel(AllIcons.General.ArrowRight);
                    separator.setBorder(JBUI.Borders.empty(SEPARATOR_BORDER_V, SEPARATOR_BORDER_H));
                    pathPanel.add(separator);
                }
            }
        }

        pathPanel.setBorder(JBUI.Borders.empty(PANEL_BORDER_TOP, PANEL_BORDER_LEFT, PANEL_BORDER_BOTTOM, PANEL_BORDER_RIGHT));

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(GBC_INSETS_TOP, GBC_INSETS_LEFT, GBC_INSETS_BOTTOM, GBC_INSETS_RIGHT);

        panel.add(pathPanel, gbc);

        return currentRow + 1;
    }

    private void setUnderline(final @NotNull JBLabel label, final boolean underline) {
        final @NotNull Font font = label.getFont();
        final @NotNull Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.UNDERLINE, underline ? TextAttribute.UNDERLINE_ON : -1);
        label.setFont(font.deriveFont(attributes));
    }
}

