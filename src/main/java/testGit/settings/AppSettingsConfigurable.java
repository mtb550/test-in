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
import testGit.pojo.ProjectStatus;
import testGit.pojo.TestPackage;
import testGit.pojo.TestProject;
import testGit.projectPanel.ProjectPanel;
import testGit.projectPanel.projectSelector.RendererImpl;
import testGit.settings.service.ProjectPanelService;
import testGit.util.DirectoryMapper;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class AppSettingsConfigurable implements Configurable {

    private final TextFieldWithBrowseButton rootTestGitPathField = new TextFieldWithBrowseButton();
    private final JBTextField rootAutomationPathField = new JBTextField();

    // Store projects as Directory objects in a Model
    private final DefaultComboBoxModel<TestProject> testProjectList = new DefaultComboBoxModel<>();
    private final ComboBox<TestProject> projectComboBox = new ComboBox<>(testProjectList);

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
        // 1. Setup Browse Listener
        rootTestGitPathField.addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Root Folder")
                        .withDescription("Choose the directory where your test projects are stored"),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        // (Optional) Add a placeholder/tooltip to guide the user
        rootAutomationPathField.getEmptyText().setText("E.g. src.test");
        rootAutomationPathField.setToolTipText("Base package path for your automation framework");

        // 2. Setup Renderer
        projectComboBox.setRenderer(new RendererImpl());

        // 3. Setup Management Button Listeners
        activateBtn.addActionListener(e -> updateProjectStatus(ProjectStatus.AC));
        deactivateBtn.addActionListener(e -> updateProjectStatus(ProjectStatus.IN));
        archiveBtn.addActionListener(e -> updateProjectStatus(ProjectStatus.AR));
        //renameBtn.addActionListener(e -> new Rename().actionPerformed(ProjectStatus.AR));

        // 4. Initialize Data
        refreshProjectList();

        // 5. Layout the buttons
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

    private void updateProjectStatus(ProjectStatus newProjectStatus) {
        TestPackage selected = (TestPackage) projectComboBox.getSelectedItem();
        if (selected == null) return;

        File oldDir = selected.getFile();
        String currentFileName = selected.getFileName();

        if (oldDir.exists() && currentFileName.contains("_")) {
            // Extract the base (PR_Name) and append the new Status (AC/IN/AR)
            String baseName = currentFileName.substring(0, currentFileName.lastIndexOf("_"));
            String newName = baseName + "_" + newProjectStatus.name();

            File newDir = new File(oldDir.getParent(), newName);
            if (oldDir.renameTo(newDir)) {
                refreshProjectList();

                // Re-select the renamed project in the new list
                for (int i = 0; i < testProjectList.getSize(); i++) {
                    if (testProjectList.getElementAt(i).getFileName().equals(newName)) {
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
        File root = new File(rootTestGitPathField.getText());

        if (root.exists() && root.isDirectory()) {
            File[] projects = root.listFiles(f -> f.isDirectory() && f.getName().startsWith("PR_"));
            if (projects != null) {
                Arrays.stream(projects)
                        .map(DirectoryMapper::mapProject) // Maps File to Directory object
                        .filter(Objects::nonNull)
                        .forEach(testProjectList::addElement);
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