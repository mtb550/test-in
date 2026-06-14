package org.testin.settings.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBTextField;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestinPathPanel {

    private final TextFieldWithBrowseButton pathField = new TextFieldWithBrowseButton();
    private final JButton openFolderBtn = new JButton("Open");

    public TestinPathPanel() {
        setupField();
        setupOpenButton();
        setupValidationListener();
    }

    private void setupField() {
        ((JBTextField) pathField.getTextField()).getEmptyText()
                .setText("Example -> c:\\users\\{username}\\documents\\testin");

        pathField.addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Root Folder")
                        .withDescription("Choose the directory where your test projects are stored"),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );
    }

    private void setupOpenButton() {
        openFolderBtn.setIcon(AllIcons.Actions.MenuOpen);
        openFolderBtn.setDisabledIcon(IconLoader.getDisabledIcon(AllIcons.Actions.MenuOpen));
        openFolderBtn.setEnabled(false);
        openFolderBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(pathField.getText()));

            } catch (Exception ex) {
                // todo, change this to get the correct project direct.
                Project project = ProjectManager.getInstance().getDefaultProject();

                Services.getInstance(project, Notifier.class)
                        .error(project, "Error", "Could not open folder: " + ex.getMessage());
            }
        });
    }

    private void setupValidationListener() {
        pathField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateOpenButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateOpenButtonState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateOpenButtonState();
            }
        });
    }

    private void updateOpenButtonState() {
        String pathStr = pathField.getText();
        if (pathStr.trim().isEmpty()) {
            openFolderBtn.setEnabled(false);
            return;
        }
        try {
            Path path = Path.of(pathStr);
            openFolderBtn.setEnabled(Files.exists(path) && Files.isDirectory(path));
        } catch (Exception e) {
            openFolderBtn.setEnabled(false);
        }
    }

    public JPanel getComponent() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(pathField, BorderLayout.CENTER);
        panel.add(openFolderBtn, BorderLayout.EAST);
        return panel;
    }

    public String getPathText() {
        return pathField.getText();
    }

    public void setPathText(final String text) {
        pathField.setText(text != null ? text : "");
    }
}
