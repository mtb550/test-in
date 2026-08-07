package org.testin.nodeCreator;

import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.DirectoryDto;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

@FunctionalInterface
public interface NodeCreator {
    @NotNull DirectoryDto execute(final @NotNull SimpleTree tree, final @NotNull String name, final @NotNull DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final @NotNull Path newDirPath);
}
