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
import java.util.stream.Collectors;
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

    public FileDocumentListener(final @NotNull TextFieldWithBrowseButton fileField, final @NotNull Project p, final @NotNull BiConsumer<FileTypes, Map<String, List<TestCaseDto>>> onDataLoaded, final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader) {
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

    // UC-SHARE-005, Rule-SHARE-028
    private void triggerLoadIfValid() {
        final @NotNull String filePath = fileField.getText().trim();
        if (filePath.isEmpty()) return;

        final @NotNull File importFile = new File(filePath);
        if (!importFile.exists() || !importFile.isFile()) return;

        loadFile(importFile);
    }

    private void loadFile(final @NotNull File importFile) {
        // Said out loud. A file no format can read fell through here in silence,
        // so a tester who picked a .pdf watched the dialog do nothing at all and
        // had no way to tell that from a file Testin was still reading (#267).
        // The formats are named rather than counted: the tester's next move is
        // to go and find one.
        importableFormatOf(importFile.getName().toLowerCase())
                .ifPresentOrElse(format -> loadFile(importFile, format),
                        () -> Services.getInstance(p, Notifier.class).softRefuse(p, "Cannot Be Imported",
                                importFile.getName() + " is not a kind of file Testin can read. It reads "
                                        + importableFormats() + "."));
    }

    /**
     * The extensions an import understands, as a tester would say them.
     */
    private static @NotNull String importableFormats() {
        return Arrays.stream(FileTypes.values())
                .filter(FileTypes::isImportable)
                .map(FileTypes::getExtension)
                .collect(Collectors.joining(", "));
    }

    private void loadFile(final @NotNull File importFile, final @NotNull FileTypes format) {

        // Parsing a workbook is heavy I/O; keep it off the EDT — this fires per keystroke.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull Map<String, List<TestCaseDto>> parsedData = importLoader.apply(importFile, format);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (parsedData.isEmpty()) {
                        Services.getInstance(p, Notifier.class).softRefuse(p, "No Data", "No test cases found in the selected file.");
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
