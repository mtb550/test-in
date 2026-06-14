package org.testin.settings.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Refresh;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.projectPanel.projectSelector.RendererImpl;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public final class ProjectManagementPanel {

    private final DefaultComboBoxModel<TestProjectDirectoryDto> projectListModel = new DefaultComboBoxModel<>();
    @Getter
    private final ComboBox<TestProjectDirectoryDto> projectComboBox = new ComboBox<>(projectListModel);
    private final JButton activateBtn = new JButton("Activate");
    private final JButton deactivateBtn = new JButton("Deactivate");
    private final JButton archiveBtn = new JButton("Archive");
    private final JButton renameBtn = new JButton("Rename");
    private final TestinPathPanel testinPathPanel;

    public ProjectManagementPanel(final @NotNull TestinPathPanel testinPathPanel) {
        this.testinPathPanel = testinPathPanel;
        setupComboBox();
        setupButtons();
    }

    private void setupComboBox() {
        projectComboBox.setRenderer(new RendererImpl());
    }

    private void setupButtons() {
        activateBtn.addActionListener(e -> updateProjectStatus(ProjectStatus.ACTIVE));
        deactivateBtn.addActionListener(e -> updateProjectStatus(ProjectStatus.INACTIVE));
        archiveBtn.addActionListener(e -> updateProjectStatus(ProjectStatus.ARCHIVED));
    }

    private Project getProject() {
        // todo, change this to get the correct project direct.
        return ProjectManager.getInstance().getDefaultProject();
    }

    public JPanel getButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.add(activateBtn);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(deactivateBtn);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(archiveBtn);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(renameBtn);
        return panel;
    }

    public void refreshProjectList() {
        Project project = getProject();

        projectListModel.removeAllElements();
        String pathStr = testinPathPanel.getPathText();
        if (pathStr.trim().isEmpty()) return;

        try {
            Path rootPath = Path.of(pathStr);
            if (Files.exists(rootPath) && Files.isDirectory(rootPath)) {
                try (Stream<Path> paths = Files.list(rootPath)) {
                    paths.filter(Files::isDirectory)
                            .map(p -> Services.getInstance(project, DirectoryMapper.class)
                                    .readTestProjectNode(project, p))
                            .filter(Objects::nonNull)
                            .forEach(projectListModel::addElement);
                }
            }
        } catch (Exception e) {
            Log.error("Failed to refresh project list: " + e.getMessage());
        }
    }

    // todo, to be removed.
    @Deprecated(forRemoval = true, since = "after remove status from name")
    private void updateProjectStatus(final ProjectStatus newProjectStatus) {
        DirectoryDto selected = (DirectoryDto) projectComboBox.getSelectedItem();
        if (selected == null) return;
        renameProjectDirectory(selected, newProjectStatus);
    }

    @Deprecated(forRemoval = true, since = "after remove status from name")
    private void renameProjectDirectory(final DirectoryDto selected, final ProjectStatus newProjectStatus) {
        Project project = getProject();

        Path oldPath = selected.getPath();
        String currentFileName = selected.getName();

        if (!Files.exists(oldPath) || !currentFileName.contains("_")) return;

        String baseName = currentFileName.substring(0, currentFileName.lastIndexOf("_"));
        String newName = baseName + "_" + newProjectStatus.name();

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile dirFile = LocalFileSystem.getInstance()
                        .refreshAndFindFileByIoFile(oldPath.toFile());
                if (dirFile != null) {
                    dirFile.rename(this, newName);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        refreshProjectList();
                        selectProjectByName(newName);
                        triggerProjectRefresh();
                    });
                }
            } catch (IOException ex) {
                Services.getInstance(project, Notifier.class)
                        .error(project, "Status Update Failed",
                                "Could not rename project directory: " + ex.getMessage());
            }
        });
    }

    private void selectProjectByName(final String name) {
        for (int i = 0; i < projectListModel.getSize(); i++) {
            if (projectListModel.getElementAt(i).getPathName().equals(name)) {
                projectComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private void triggerProjectRefresh() {
        Project project = getProject();
        ProjectPanel panel = Services.getInstance(project, ProjectPanel.class);
        if (panel != null) {
            new Refresh(panel).execute();
        }
    }
}
