package org.testin.importExport.shared;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.testin.enums.FileTypes;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class FileDocumentListener implements DocumentListener {
    private final TextFieldWithBrowseButton fileField;
    private final Project project;
    private final Consumer<Map<String, List<TestCaseDto>>> onDataLoaded;
    private final BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader;

    public FileDocumentListener(TextFieldWithBrowseButton fileField, Project project, Consumer<Map<String, List<TestCaseDto>>> onDataLoaded, BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader) {
        this.fileField = fileField;
        this.project = project;
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
                Services.getInstance(project, Notifier.class).warn(project, "No Data", "No test cases found in the selected file.");
                return;
            }

            onDataLoaded.accept(parsedData);

        } catch (final Exception ex) {
            Logger.error("Import parse failed: " + ex.getMessage());
            Services.getInstance(project, Notifier.class).error(
                    project, "Parse Error", ex.getMessage()
            );
        }
    }
}
