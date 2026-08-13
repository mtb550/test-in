package org.testin.settings.Dialogs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestinPathPanel {

    private final @NotNull Project p;
    private final @NotNull TextFieldWithBrowseButton pathField = new TextFieldWithBrowseButton();
    private final @NotNull JButton openFolderBtn = new JButton("Open");

    public TestinPathPanel(final @NotNull Project p) {
        this.p = p;
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

            } catch (final Exception ex) {
                Services.getInstance(p, Notifier.class).error(p, "Error", "Could not open folder: " + ex.getMessage());
            }
        });
    }

    private void setupValidationListener() {
        pathField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final @NotNull DocumentEvent e) {
                updateOpenButtonState();
            }

            @Override
            public void removeUpdate(final @NotNull DocumentEvent e) {
                updateOpenButtonState();
            }

            @Override
            public void changedUpdate(final @NotNull DocumentEvent e) {
                updateOpenButtonState();
            }
        });
    }

    private void updateOpenButtonState() {
        final String pathStr = pathField.getText();
        if (pathStr.trim().isEmpty()) {
            openFolderBtn.setEnabled(false);
            return;
        }
        try {
            final Path path = Path.of(pathStr);
            openFolderBtn.setEnabled(Files.exists(path) && Files.isDirectory(path));
        } catch (final Exception ex) {
            openFolderBtn.setEnabled(false);
        }
    }

    public @NotNull JBPanel<?> getComponent() {
        final JBPanel<?> panel = new JBPanel<>(new BorderLayout(5, 0));
        panel.add(pathField, BorderLayout.CENTER);
        panel.add(openFolderBtn, BorderLayout.EAST);
        return panel;
    }

    public @NotNull String getPathText() {
        return pathField.getText();
    }

    public void setPathText(final @Nullable String text) {
        pathField.setText(text != null ? text : "");
    }
}
