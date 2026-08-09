package org.testin.viewPanel.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.settings.Setting;
import org.testin.util.EditorUtil;
import org.testin.util.FontSync;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NavigationBar extends BaseDetails {

    final Color DEFAULT_TEXT_COLOR = Gray._120;
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

    private final ArrayList<String> currentPath;

    public NavigationBar(final @Nullable ArrayList<String> currentPath) {
        this.currentPath = currentPath;
    }

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final JBPanel<?> pathPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pathPanel.setOpaque(false);

        float navFontSize = Math.max(8.0f, FontSync.getBaseFontSize() - 1.0f);

        if (currentPath != null) {
            for (int i = 0; i < currentPath.size(); i++) {

                final String labelText = currentPath.get(i);
                final boolean isLast = (i == currentPath.size() - 1);

                final JBLabel folderLabel = new JBLabel(labelText);
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

                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        if (isLast) {
                            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                            Path testSetPath = Services.getInstance(p, Setting.class).getTestinPath();
                            for (final String segment : currentPath) {
                                testSetPath = testSetPath.resolve(segment);
                            }

                            final TestSetDirectoryDto ts = indexer.getTestSetByPath(testSetPath);
                            if (ts == null || Services.getInstance(p, EditorUtil.class).isOpen(p, ts.getName()))
                                return;

                            Services.getInstance(p, EditorUtil.class).open(p, ts);
                        }
                    }
                });

                pathPanel.add(folderLabel);
                if (!isLast) {
                    final JBLabel separator = new JBLabel(AllIcons.General.ArrowRight);
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
        final Font font = label.getFont();
        final Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.UNDERLINE, underline ? TextAttribute.UNDERLINE_ON : -1);
        label.setFont(font.deriveFont(attributes));
    }
}
