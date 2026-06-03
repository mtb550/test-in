package org.testin.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.Refresh;
import org.testin.pojo.Config;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.projectPanel.projectSelector.RendererImpl;
import org.testin.settings.service.ProjectPanelService;
import org.testin.util.Bundle;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class AppSettingsConfigurable implements Configurable {

    private final TextFieldWithBrowseButton rootTestinPathField = new TextFieldWithBrowseButton();
    private final JBTextField rootAutomationPathField = new JBTextField();

    private final DefaultComboBoxModel<TestProjectDirectoryDto> testProjectList = new DefaultComboBoxModel<>();
    private final ComboBox<TestProjectDirectoryDto> projectComboBox = new ComboBox<>(testProjectList);

    private final JBCheckBox readModeCheckBox = new JBCheckBox("Enable read mode (view only)");

    private final JButton openFolderBtn = new JButton("Open");
    private final JButton activateBtn = new JButton("Activate");
    private final JButton deactivateBtn = new JButton("Deactivate");
    private final JButton archiveBtn = new JButton("Archive");
    private final JButton renameBtn = new JButton("Rename");

    @Override
    public String getDisplayName() {
        return Bundle.getPluginName();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ((JBTextField) rootTestinPathField.getTextField()).getEmptyText().setText("Example -> c:\\users\\{username}\\documents\\testin");
        rootTestinPathField.addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Root Folder")
                        .withDescription("Choose the directory where your test projects are stored"),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        rootTestinPathField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateOpenFolderBtnState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateOpenFolderBtnState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateOpenFolderBtnState();
            }
        });

        openFolderBtn.setIcon(AllIcons.Actions.MenuOpen);
        openFolderBtn.setDisabledIcon(IconLoader.getDisabledIcon(AllIcons.Actions.MenuOpen));
        openFolderBtn.setEnabled(false);
        openFolderBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(rootTestinPathField.getText()));
            } catch (Exception ex) {
                Notifier.getInstance().error("Error", "Could not open folder: " + ex.getMessage());
            }
        });

        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.add(rootTestinPathField, BorderLayout.CENTER);
        pathPanel.add(openFolderBtn, BorderLayout.EAST);

        rootAutomationPathField.setEnabled(false);
        rootAutomationPathField.setToolTipText("Automatically detected base package path for your automation framework");

        projectComboBox.setRenderer(new RendererImpl());

        activateBtn.addActionListener(e -> updateProjectStatus(ProjectStatus.ACTIVE));
        deactivateBtn.addActionListener(e -> updateProjectStatus(ProjectStatus.INACTIVE));
        archiveBtn.addActionListener(e -> updateProjectStatus(ProjectStatus.ARCHIVED));
        //renameBtn.addActionListener(e -> new Rename().actionPerformed(ProjectStatus.AR));

        refreshProjectList();

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.add(activateBtn);
        buttonPanel.add(new Box.Filler(new Dimension(5, 0), new Dimension(5, 0), new Dimension(5, 0)));
        buttonPanel.add(deactivateBtn);
        buttonPanel.add(new Box.Filler(new Dimension(5, 0), new Dimension(5, 0), new Dimension(5, 0)));
        buttonPanel.add(archiveBtn);
        buttonPanel.add(new Box.Filler(new Dimension(5, 0), new Dimension(5, 0), new Dimension(5, 0)));
        buttonPanel.add(renameBtn);

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Root testin folder: "), pathPanel, 1, false)
                .addLabeledComponent(new JBLabel("Root Automation folder: "), rootAutomationPathField, 1, false)
                .addVerticalGap(10)
                .addComponent(new TitledSeparator("Project Management"))
                .addLabeledComponent("Select test project: ", projectComboBox)
                .addComponent(buttonPanel)
                .addVerticalGap(5)
                .addComponent(readModeCheckBox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private void updateOpenFolderBtnState() {
        String pathStr = rootTestinPathField.getText();
        if (pathStr.trim().isEmpty()) {
            openFolderBtn.setEnabled(false);
            return;
        }

        try {
            Path path = Path.of(pathStr);
            openFolderBtn.setEnabled(Files.exists(path) && Files.isDirectory(path));
        } catch (Exception ex) {
            openFolderBtn.setEnabled(false);
        }
    }

    @Deprecated(forRemoval = true, since = "after remove status from name, this method logic should be moved to .pr with remove split and _")
    private void updateProjectStatus(ProjectStatus newProjectStatus) {
        DirectoryDto selected = (DirectoryDto) projectComboBox.getSelectedItem();
        if (selected == null) return;

        Path oldPath = selected.getPath();
        String currentFileName = selected.getName();

        if (Files.exists(oldPath) && currentFileName.contains("_")) { // todo, to be removed _ , no need after move project status logic to .pr.
            String baseName = currentFileName.substring(0, currentFileName.lastIndexOf("_"));
            String newName = baseName + "_" + newProjectStatus.name(); //todo, no need for _

            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    VirtualFile oldDirVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(oldPath.toFile());
                    if (oldDirVFile != null) {
                        oldDirVFile.rename(this, newName);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            refreshProjectList();

                            for (int i = 0; i < testProjectList.getSize(); i++) {
                                if (testProjectList.getElementAt(i).getPathName().equals(newName)) {
                                    projectComboBox.setSelectedIndex(i);
                                    break;
                                }
                            }

                            ProjectPanel panel = ProjectPanelService.getInstance(Config.getProject()).getPanel();
                            if (panel != null) {
                                new Refresh(panel).execute();
                                Log.info("ToolWindow refresh triggered successfully.");
                            }
                        });
                    }
                } catch (IOException ex) {
                    Notifier.getInstance().error("Status Update Failed", "Could not rename project directory: " + ex.getMessage());
                }
            });
        }
    }

    private void refreshProjectList() {
        testProjectList.removeAllElements();

        String pathStr = rootTestinPathField.getText();
        if (pathStr.trim().isEmpty()) return;

        try {
            Path rootPath = Path.of(pathStr);

            if (Files.exists(rootPath) && Files.isDirectory(rootPath)) {
                try (Stream<Path> paths = Files.list(rootPath)) {
                    paths.filter(Files::isDirectory)
                            //.filter(path -> path.getFileName().toString().startsWith("PR_"))
                            .map(DirectoryMapper.getInstance()::testProjectNode)
                            .filter(Objects::nonNull)
                            .forEach(testProjectList::addElement);

                }
            }
        } catch (Exception e) {
            Log.error("Failed to refresh project list: " + e.getMessage());
            Log.error("Exception: " + e.getMessage());
        }
    }

    @Override
    public boolean isModified() {
        AppSettingsState settings = AppSettingsState.getInstance();
        boolean modified = !rootTestinPathField.getText().equals(settings.rootTestinPath);
        modified |= !rootAutomationPathField.getText().equals(settings.rootAutomationPath);
        modified |= readModeCheckBox.isSelected() != settings.readMode;
        return modified;
    }

    @Override
    public void apply() {
        AppSettingsState settings = AppSettingsState.getInstance();

        settings.rootTestinPath = rootTestinPathField.getText();
        settings.rootAutomationPath = rootAutomationPathField.getText();
        settings.readMode = readModeCheckBox.isSelected();

        if (settings.rootTestinPath != null && !settings.rootTestinPath.trim().isEmpty())
            Config.setTestinPath(Path.of(settings.rootTestinPath));
        else
            Config.setTestinPath(Path.of(""));

        if (settings.rootAutomationPath != null && !settings.rootAutomationPath.trim().isEmpty())
            Config.setAutomationPath(Path.of(settings.rootAutomationPath));
        else
            Config.setAutomationPath(null);

        ProjectPanel panel = ProjectPanelService.getInstance(Config.getProject()).getPanel();
        if (panel != null) {
            new Refresh(panel).execute();
            Log.info("ToolWindow refresh triggered successfully.");
        }
    }

    @Override
    public void reset() {
        AppSettingsState settings = AppSettingsState.getInstance();
        rootTestinPathField.setText(settings.rootTestinPath != null ? settings.rootTestinPath : "");

        Project project = Config.getProject();
        VirtualFile mainSourceRoot = Tools.getInstance().getTestSourceRoot(project);

        if (mainSourceRoot != null) {
            rootAutomationPathField.setText(mainSourceRoot.getPath());
        } else {
            rootAutomationPathField.setText(settings.rootAutomationPath != null ? settings.rootAutomationPath : "No source root detected");
        }

        readModeCheckBox.setSelected(settings.readMode);

        updateOpenFolderBtnState();
        refreshProjectList();
    }
}