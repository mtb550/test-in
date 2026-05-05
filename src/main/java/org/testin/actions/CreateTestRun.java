package org.testin.actions;

import com.intellij.openapi.project.Project;
import org.testin.editorPanel.testRunEditor.RunEditor;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.TestRunStatus;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class CreateTestRun implements DirectoryType.NodeCreator {

    @Override
    public void execute(final CreateTestNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestRunDto metadata = new TestRunDto();
        metadata.setStatus(TestRunStatus.CREATED);

        TestRunDirectoryDto tr = new TestRunDirectoryDto()
                .setName(name)
                .setPath(newDirPath);

        TreeUtilImpl.createVf(this, parentDir.getPath(), name);
        TreeUtilImpl.createDataVf(this, newDirPath, DirectoryType.TR.getMarker());

        RunEditor.create(tr, action.getProjectPanel(), action.getProjectPanel().getTestProjectSelector().getSelectedTestProject().getItem(), metadata);
    }
}
