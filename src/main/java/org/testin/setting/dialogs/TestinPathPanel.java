/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import org.testin.util.Bundle;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestinPathPanel {

    private final @NotNull TextFieldWithBrowseButton pathField = new TextFieldWithBrowseButton();
    private final @NotNull JButton openFolderBtn = new JButton(Bundle.message("settings.path.open"));

    public TestinPathPanel() {
        setupField();
        setupOpenButton();
        setupValidationListener();
    }

    private void setupField() {
        ((JBTextField) pathField.getTextField()).getEmptyText()
                .setText(Bundle.message("settings.path.example"));

        pathField.addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle(Bundle.message("settings.path.root.title"))
                        .withDescription(Bundle.message("settings.path.root.description")),
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
                        Bundle.message("settings.path.open.failed", ex.getMessage()), Bundle.message("settings.path.open.failed.title"));
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
        final @NotNull String pathStr = pathField.getText();
        if (pathStr.trim().isEmpty()) {
            openFolderBtn.setEnabled(false);
            return;
        }
        try {
            final @NotNull Path path = Path.of(pathStr);
            openFolderBtn.setEnabled(Files.exists(path) && Files.isDirectory(path));
        } catch (final Exception ex) {
            openFolderBtn.setEnabled(false);
        }
    }

    // UC-SETTING-002, UC-SETTING-003
    public @NotNull JBPanel<?> getComponent() {
        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout(5, 0));
        panel.add(pathField, BorderLayout.CENTER);
        panel.add(openFolderBtn, BorderLayout.EAST);
        return panel;
    }

    public @NotNull String getPathText() {
        return pathField.getText();
    }

    public void setPathText(final @NotNull String text) {
        pathField.setText(text);
    }
}
