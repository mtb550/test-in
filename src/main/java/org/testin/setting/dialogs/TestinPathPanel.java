package org.testin.setting.dialogs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestinPathPanel {

    private final @NotNull TextFieldWithBrowseButton pathField = new TextFieldWithBrowseButton();
    private final @NotNull JButton openFolderBtn = new JButton("Open");

    public TestinPathPanel() {
        setupField();
        setupOpenButton();
        setupValidationListener();
    }

    private void setupField() {
        ((JBTextField) pathField.getTextField()).getEmptyText()
                .setText("Example -> C:\\Users\\{username}\\Documents\\Testin");

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
                // A dialog rather than a Notifier balloon: the settings page is
                // application-level and has no project to notify through, and it
                // is modal anyway, so a balloon behind it would go unread (#70).
                Messages.showErrorDialog(openFolderBtn,
                        "Could not open folder: " + ex.getMessage(), "Open Folder Failed");
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
