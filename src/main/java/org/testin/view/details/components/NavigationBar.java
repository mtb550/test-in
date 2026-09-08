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
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.EditorUtil;
import org.testin.util.FontSync;

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

                    // UC-VIEW-PANEL-010, Rule-VIEW-PANEL-042, Rule-VIEW-PANEL-043
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        if (isLast) {
                            // Resolved by the class that owns it. This walked the
                            // segments off the raw stored root, which skips the
                            // two steps that make it a real path - falling back
                            // to the project directory when nothing is
                            // configured, and resolving a relative root against
                            // it. Either case yielded a relative path, the
                            // indexer lookup then threw, and it threw out of a
                            // Swing mouse listener.
                            final @NotNull Path lastStepPath = Services.getInstance(p, TestinRoot.class).resolve(currentPath);

                            // Whatever the step names, which is a test set when
                            // the panel was opened from the tree and a test run
                            // when it was opened from a run. Asking for a test
                            // set by path threw the second time: there is no test
                            // set where a run is, and a cache miss is an error the
                            // tester cannot read - out of a mouse listener again.
                            // Asked, not assumed, so the step opens what it says.
                            Services.getInstance(p, ProjectIndexer.class).find(lastStepPath)
                                    .filter(DirectoryDto::isOpenableInEditor)
                                    .ifPresent(dir -> Services.getInstance(p, EditorUtil.class).open(p, dir));

                            // No is-open guard. Opening a node that is already
                            // open focuses it, which is what the guard was for -
                            // and the guard matched on the name, so with two test
                            // sets called the same it focused the neighbor and
                            // then returned without opening this one.
                        }
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

