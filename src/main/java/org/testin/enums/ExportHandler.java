package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.testin.importExport.exports.ExportAction;
import org.testin.mappers.dto.TestCaseDto;

import java.io.File;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ExportHandler {
    void execute(Project p, ExportAction exportAction, File destFile, Map<String, List<TestCaseDto>> sheetsData);
}
