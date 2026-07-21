package org.testin.actions.exports;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.FilesUtil;
import org.testin.util.services.Services;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ExportJson extends Export {

    public ExportJson(final @NotNull SimpleTree tree) {
        super(tree);
    }

    public void exportToFile(final @NotNull Project project, final File destFile, final Map<String, List<TestCaseDto>> sheetsData) {
        Services.getInstance(project, FilesUtil.class).write(project, destFile.toPath(), sheetsData);
    }
}
