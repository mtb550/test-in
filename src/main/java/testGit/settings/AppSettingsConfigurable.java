package testGit.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import testGit.actions.Refresh;
import testGit.pojo.Config;
import testGit.pojo.DirectoryMapper;
import testGit.pojo.ProjectStatus;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;
import testGit.projectPanel.ProjectPanel;
import testGit.projectPanel.projectSelector.RendererImpl;
import testGit.settings.service.ProjectPanelService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class AppSettingsConfigurable implements Configurable {

    private final TextFieldWithBrowseButton rootTestGitPathField = new TextFieldWithBrowseButton();
    private final JBTextField rootAutomationPathField = new JBTextField();

    private final DefaultComboBoxModel<TestProjectDirectoryDto> testProjectList = new DefaultComboBoxModel<>();
    private final ComboBox<TestProjectDirectoryDto> projectComboBox = new ComboBox<>(testProjectList);

    private final JBCheckBox readModeCheckBox = new JBCheckBox("Enable read mode (view only)");

    private final JButton activateBtn = new JButton("Activate");
    private final JButton deactivateBtn = new JButton("Deactivate");
    private final JButton archiveBtn = new JButton("Archive");
    private final JButton renameBtn = new JButton("Rename");

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "TestGit Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        rootTestGitPathField.addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Root Folder")
                        .withDescription("Choose the directory where your test projects are stored"),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        rootAutomationPathField.getEmptyText().setText("E.g. src.test");
        rootAutomationPathField.setToolTipText("Base package path for your automation framework");

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
                .addLabeledComponent(new JBLabel("Root TestGit folder: "), rootTestGitPathField, 1, false)
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

    @Deprecated(forRemoval = true, since = "after remove status from name, this method logic should be moved to .pr with remove split and _")
    private void updateProjectStatus(ProjectStatus newProjectStatus) {
        DirectoryDto selected = (DirectoryDto) projectComboBox.getSelectedItem();
        if (selected == null) return;

        File oldDir = selected.getPath().toFile();
        String currentFileName = selected.getName();

        if (oldDir.exists() && currentFileName.contains("_")) { // todo, to be removed _ , no need after move project status logic to .pr.
            String baseName = currentFileName.substring(0, currentFileName.lastIndexOf("_")); // todo, no need for _
            String newName = baseName + "_" + newProjectStatus.name(); //todo, no need for _

            File newDir = new File(oldDir.getParent(), newName);
            if (oldDir.renameTo(newDir)) {
                refreshProjectList();

                for (int i = 0; i < testProjectList.getSize(); i++) {
                    if (testProjectList.getElementAt(i).getPathName().equals(newName)) {
                        projectComboBox.setSelectedIndex(i);
                        break;
                    }
                }

                VfsUtil.markDirtyAndRefresh(false, true, true, newDir);

                ProjectPanel panel = ProjectPanelService.getInstance(Config.getProject()).getPanel();
                if (panel != null) {
                    new Refresh(panel).actionPerformed(null);
                    System.out.println("ToolWindow refresh triggered successfully.");
                }
            }
        }
    }

    private void refreshProjectList() {
        testProjectList.removeAllElements();

        Path rootPath = Path.of(rootTestGitPathField.getText());

        if (Files.exists(rootPath) && Files.isDirectory(rootPath)) {

            try (Stream<Path> paths = Files.list(rootPath)) {
                paths.filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().startsWith("PR_"))
                        .map(DirectoryMapper::testProjectNode)
                        .filter(Objects::nonNull)
                        .forEach(testProjectList::addElement);

            } catch (Exception e) {
                System.err.println("Failed to refresh project list: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    @Override
    public boolean isModified() {
        AppSettingsState settings = AppSettingsState.getInstance();
        boolean modified = !rootTestGitPathField.getText().equals(settings.rootTestGitPath);
        modified |= !rootAutomationPathField.getText().equals(settings.rootAutomationPath);
        modified |= readModeCheckBox.isSelected() != settings.readMode;
        return modified;
    }

    @Override
    public void apply() {
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.rootTestGitPath = rootTestGitPathField.getText();
        settings.rootAutomationPath = rootAutomationPathField.getText();

        settings.readMode = readModeCheckBox.isSelected();
    }

    @Override
    public void reset() {
        AppSettingsState settings = AppSettingsState.getInstance();
        rootTestGitPathField.setText(settings.rootTestGitPath != null ? settings.rootTestGitPath : "");
        rootAutomationPathField.setText(settings.rootAutomationPath != null ? settings.rootAutomationPath : "");
        readModeCheckBox.setSelected(settings.readMode);
        refreshProjectList();
    }
}