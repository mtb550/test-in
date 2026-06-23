package org.testin.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryNode extends SimpleNode {

    private final Project project;
    private final DirectoryDto directoryDto;
    private final ProjectPanel projectPanel;

    public DirectoryNode(Project project, DirectoryDto directoryDto, ProjectPanel projectPanel) {
        super(project);
        this.project = project;
        this.directoryDto = directoryDto;
        this.projectPanel = projectPanel;
    }

    @Override
    public SimpleNode @NotNull [] getChildren() {
        if (directoryDto == null) return new SimpleNode[0];

        List<SimpleNode> children = new ArrayList<>();

        if (directoryDto instanceof TestProjectDirectoryDto projectDto) {
            if (projectDto.getTestCasesDirectory() != null) {
                children.add(new DirectoryNode(myProject, projectDto.getTestCasesDirectory(), projectPanel));
            }
            if (projectDto.getTestRunsDirectory() != null) {
                children.add(new DirectoryNode(myProject, projectDto.getTestRunsDirectory(), projectPanel));
            }
            return children.toArray(new SimpleNode[0]);
        }

        final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
        final Path currentPath = directoryDto.getPath();

        final List<DirectoryDto> childDtos = indexer.getChildren(currentPath);
        for (final DirectoryDto childDto : childDtos) {
            children.add(new DirectoryNode(myProject, childDto, projectPanel));
        }

        return children.toArray(new SimpleNode[0]);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        super.update(presentation);
        if (directoryDto == null) return;

        DirectoryType type = Arrays.stream(DirectoryType.values())
                .filter(t -> t.getClazz() == directoryDto.getClass())
                .findFirst()
                .orElse(null);

        presentation.setIcon(type != null ? type.getIcon() : AllIcons.Nodes.Folder);

        SimpleTextAttributes textAttr = (directoryDto instanceof TestCasesMainDirectoryDto || directoryDto instanceof TestRunsMainDirectoryDto)
                ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                : SimpleTextAttributes.REGULAR_ATTRIBUTES;

        presentation.addText(directoryDto.getName(), textAttr);
    }

    public DirectoryDto getValue() {
        return directoryDto;
    }
}