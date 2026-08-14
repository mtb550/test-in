package org.testin.importExport.shared;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.FileTypes;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;

public class FileDocumentListener implements DocumentListener {
    private final @NotNull TextFieldWithBrowseButton fileField;
    private final @NotNull Project p;
    private final @NotNull BiConsumer<FileTypes, Map<String, List<TestCaseDto>>> onDataLoaded;
    private final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader;

    public FileDocumentListener(final @NotNull TextFieldWithBrowseButton fileField, final @NotNull Project p,
                                final @NotNull BiConsumer<FileTypes, Map<String, List<TestCaseDto>>> onDataLoaded,
                                final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader) {
        this.fileField = fileField;
        this.p = p;
        this.onDataLoaded = onDataLoaded;
        this.importLoader = importLoader;
    }

    /**
     * The format that can read this file name, or null when nothing can. Only
     * formats with an import handler count; matching e.g. .html would NPE downstream.
     */
    private static @Nullable FileTypes importableFormatOf(final @NotNull String fileName) {
        for (final FileTypes type : FileTypes.values()) {
            if (type.isImportable() && fileName.endsWith(type.getExtension())) return type;
        }
        return null;
    }

    @Override
    public void insertUpdate(final @NotNull DocumentEvent e) {
        triggerLoadIfValid();
    }

    @Override
    public void removeUpdate(final @NotNull DocumentEvent e) {
        triggerLoadIfValid();
    }

    @Override
    public void changedUpdate(final @NotNull DocumentEvent e) {
        triggerLoadIfValid();
    }

    private void triggerLoadIfValid() {
        final String filePath = fileField.getText().trim();
        if (filePath.isEmpty()) return;

        final File importFile = new File(filePath);
        if (!importFile.exists() || !importFile.isFile()) return;

        loadFile(importFile);
    }

    private void loadFile(final @NotNull File importFile) {
        final FileTypes format = importableFormatOf(importFile.getName().toLowerCase());
        if (format == null) return;

        // Parsing a workbook is heavy I/O; keep it off the EDT — this fires per keystroke.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Map<String, List<TestCaseDto>> parsedData = importLoader.apply(importFile, format);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (parsedData == null || parsedData.isEmpty()) {
                        Services.getInstance(p, Notifier.class).warn(p, "No Data", "No test cases found in the selected file.");
                        return;
                    }
                    onDataLoaded.accept(format, parsedData);
                });

            } catch (final Exception ex) {
                Logger.error("Import parse failed: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(p, Notifier.class).error(p, "Parse Error", ex.getMessage()));
            }
        });
    }
}
