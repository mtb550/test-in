package org.testin.importexport.shared;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileTypes;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Arrays;
import java.util.Optional;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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
     * The format that can read this file name, empty when nothing can. Only
     * formats with an import handler count; matching an .html file would NPE
     * downstream.
     */
    private static @NotNull Optional<FileTypes> importableFormatOf(final @NotNull String fileName) {
        return Arrays.stream(FileTypes.values())
                .filter(type -> type.isImportable() && fileName.endsWith(type.getExtension()))
                .findFirst();
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
        importableFormatOf(importFile.getName().toLowerCase())
                .ifPresent(format -> loadFile(importFile, format));
    }

    private void loadFile(final @NotNull File importFile, final @NotNull FileTypes format) {

        // Parsing a workbook is heavy I/O; keep it off the EDT — this fires per keystroke.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Map<String, List<TestCaseDto>> parsedData = importLoader.apply(importFile, format);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (parsedData == null || parsedData.isEmpty()) {
                        Services.getInstance(p, Notifier.class).softShow(p, "No Data", "No test cases found in the selected file.");
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
