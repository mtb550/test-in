package org.testin.importExport.shared;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;
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
import java.util.function.Consumer;

public class FileDocumentListener implements DocumentListener {
    private final TextFieldWithBrowseButton fileField;
    private final @NotNull Project p;
    private final Consumer<Map<String, List<TestCaseDto>>> onDataLoaded;
    private final BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader;

    public FileDocumentListener(TextFieldWithBrowseButton fileField, Project p, Consumer<Map<String, List<TestCaseDto>>> onDataLoaded, BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader) {
        this.fileField = fileField;
        this.p = p;
        this.onDataLoaded = onDataLoaded;
        this.importLoader = importLoader;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        triggerLoadIfValid();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        triggerLoadIfValid();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        triggerLoadIfValid();
    }

    private void triggerLoadIfValid() {
        String filePath = fileField.getText().trim();
        if (filePath.isEmpty()) return;

        File importFile = new File(filePath);
        if (!importFile.exists() || !importFile.isFile()) return;

        loadFile(importFile);
    }

    private void loadFile(final File importFile) {
        String name = importFile.getName().toLowerCase();

        FileTypes fmt = null;
        for (FileTypes ft : FileTypes.values()) {
            if (name.endsWith(ft.getExtension())) {
                fmt = ft;
                break;
            }
        }

        if (fmt == null) return;
        if (importLoader == null) return;

        try {
            Map<String, List<TestCaseDto>> parsedData = importLoader.apply(importFile, fmt);

            if (parsedData == null || parsedData.isEmpty()) {
                Services.getInstance(p, Notifier.class).warn(p, "No Data", "No test cases found in the selected file.");
                return;
            }

            onDataLoaded.accept(parsedData);

        } catch (final Exception ex) {
            Logger.error("Import parse failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(
                    p, "Parse Error", ex.getMessage()
            );
        }
    }
}
