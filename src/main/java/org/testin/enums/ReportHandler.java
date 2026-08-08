package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;

import java.util.Map;
import java.util.UUID;

@FunctionalInterface
public interface ReportHandler {
    byte[] execute(Project p, TestRunDirectoryDto trDir, TestRunDto tr, Map<UUID, TestCaseDto> detailsMap);
}
