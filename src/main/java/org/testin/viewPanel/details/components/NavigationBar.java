package org.testin.viewPanel.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.Gray;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Bundle;
import org.testin.util.EditorUtil;
import org.testin.util.FontSyncUtil;
import org.testin.util.services.Services;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigationBar extends BaseDetails {

    private static final Color DEFAULT_TEXT_COLOR = Gray._120;
    private static final int SEPARATOR_BORDER_V = 0;
    private static final int SEPARATOR_BORDER_H = 6;
    private static final int PANEL_BORDER_TOP = 10;
    private static final int PANEL_BORDER_LEFT = 16;
    private static final int PANEL_BORDER_BOTTOM = 5;
    private static final int PANEL_BORDER_RIGHT = 0;
    private static final int GBC_INSETS_TOP = 12;
    private static final int GBC_INSETS_LEFT = 16;
    private static final int GBC_INSETS_BOTTOM = 0;
    private static final int GBC_INSETS_RIGHT = 16;

    private final Project project;
    private final Path currentPath;

    public NavigationBar(@Nullable final Project project, @Nullable final Path currentPath) {
        this.project = project;
        this.currentPath = currentPath;
    }

    @Override
    public int render(@NotNull final Project project, @NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow) {
        final JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pathPanel.setOpaque(false);

        float navFontSize = Math.max(8.0f, FontSyncUtil.getBaseFontSize() - 1.0f);

        if (currentPath != null) {
            final List<File> fileList = buildPathFileList(currentPath);
            for (int i = 0; i < fileList.size(); i++) {

                final File file = fileList.get(i);
                final String labelText = (i == 0) ? project.getName() : file.getName();
                final boolean isTestSet = (i == fileList.size() - 1);

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
                        final VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(file);
                        if (vf == null) return;

                        if (isTestSet) {
                            if (Services.getInstance(project, EditorUtil.class).isEditorOpen(project, file.getName())) {
                                return;
                            }

                            final TestSetDirectoryDto ts = new TestSetDirectoryDto();
                            ts.setPath(file.toPath()); // todo, why set path here?
                            ts.setName(file.getName()); // todo, why set name here ?
                            Services.getInstance(project, EditorUtil.class).openEditor(project, ts);
                        } else {
                            ProjectView.getInstance(project).select(null, vf, true);
                        }
                    }
                });

                pathPanel.add(folderLabel);
                if (i < fileList.size() - 1) {
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

    @NotNull
    private List<File> buildPathFileList(@NotNull final Path path) {
        final List<File> fileList = new ArrayList<>();
        final String projectName = project.getName();
        File currentDir = path.toFile();

        while (currentDir != null) {
            fileList.addFirst(currentDir);

            final String dirName = currentDir.getName();
            if (dirName.equalsIgnoreCase(projectName) || dirName.equalsIgnoreCase(Bundle.getPluginName())) {
                break;
            }

            currentDir = currentDir.getParentFile();
        }

        return fileList;
    }

    private void setUnderline(@NotNull final JLabel label, final boolean underline) {
        final Font font = label.getFont();
        final Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.UNDERLINE, underline ? TextAttribute.UNDERLINE_ON : -1);
        label.setFont(font.deriveFont(attributes));
    }
}