package testGit.viewPanel.details.components;

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
import testGit.editorPanel.testCaseEditor.TestEditor;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.TestSetDirectoryDto;

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
    private final Path currentPath;

    public NavigationBar(@Nullable Path currentPath) {
        this.currentPath = currentPath;
    }

    @Override
    public int render(@NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, int currentRow) {
        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pathPanel.setOpaque(false);

        if (currentPath != null && Config.getProject() != null) {
            List<File> fileList = buildPathFileList(currentPath);
            for (int i = 0; i < fileList.size(); i++) {
                Project project = Config.getProject();
                File file = fileList.get(i);
                String labelText = (i == 0) ? project.getName() : file.getName();
                boolean isTestSet = (i == fileList.size() - 1);

                JBLabel folderLabel = new JBLabel(labelText);
                folderLabel.setFont(JBUI.Fonts.label(14));
                folderLabel.setForeground(Gray._120);
                folderLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                folderLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        folderLabel.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
                        setUnderline(folderLabel, true);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        folderLabel.setForeground(Gray._120);
                        setUnderline(folderLabel, false);
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(file);
                        if (vf == null) return;
                        if (isTestSet) {
                            TestSetDirectoryDto ts = new TestSetDirectoryDto();
                            ts.setPath(file.toPath());
                            ts.setName(file.getName());
                            TestEditor.open(ts);
                        } else {
                            ProjectView.getInstance(project).select(null, vf, true);
                        }
                    }
                });

                pathPanel.add(folderLabel);
                if (i < fileList.size() - 1) {
                    JBLabel separator = new JBLabel(AllIcons.General.ArrowRight);
                    separator.setBorder(JBUI.Borders.empty(0, 6));
                    pathPanel.add(separator);
                }
            }
        }

        pathPanel.setBorder(JBUI.Borders.empty(0, 16, 12, 16));

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insetsTop(12);
        panel.add(pathPanel, gbc);

        return currentRow + 1;
    }

    @NotNull
    private List<File> buildPathFileList(@NotNull Path path) {
        List<File> fileList = new ArrayList<>();
        if (Config.getProject() == null) return fileList;

        String basePathString = Config.getProject().getBasePath();
        File currentDir = path.toFile();

        if (basePathString != null) {
            File baseDir = new File(basePathString);
            while (currentDir != null && !currentDir.getAbsolutePath().equalsIgnoreCase(baseDir.getAbsolutePath())) {
                fileList.addFirst(currentDir);
                currentDir = currentDir.getParentFile();
            }
            fileList.addFirst(baseDir);
        } else {
            fileList.addFirst(currentDir);
        }
        return fileList;
    }

    private void setUnderline(@NotNull JLabel label, boolean underline) {
        Font font = label.getFont();
        Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.UNDERLINE, underline ? TextAttribute.UNDERLINE_ON : -1);
        label.setFont(font.deriveFont(attributes));
    }
}