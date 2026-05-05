package org.testin.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunsMainDirectoryDto;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

public class TreeCellRenderer extends ColoredTreeCellRenderer {
    private final Set<DefaultMutableTreeNode> selectedNodes;

    public TreeCellRenderer(final Set<DefaultMutableTreeNode> selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    @Override
    public void customizeCellRenderer(@NotNull final JTree tree, final Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
        try {
            switch (value) {
                case DefaultMutableTreeNode node when node.getUserObject() instanceof DirectoryDto dir -> {

                    DirectoryType type = Arrays.stream(DirectoryType.values())
                            .filter(t -> t.getClazz() == dir.getClass())
                            .findFirst()
                            .orElse(null);

                    setIcon(type != null ? type.getIcon() : AllIcons.Nodes.Folder);
                    append(dir.getName(), getSimpleTextAttributes(node, dir));
                    append(" ");
                    statusTag(dir, tree);
                    // todo, here test set tag.
                }

                case DefaultMutableTreeNode node -> {
                    setIcon(AllIcons.Nodes.Unknown);
                    Object obj = node.getUserObject();
                    append(obj != null ? obj.toString() : "", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }

                default -> {
                    setIcon(AllIcons.Nodes.Unknown);
                    append(value.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
            }

        } catch (Exception e) {
            System.err.println("Error rendering tree node: " + e.getMessage());
            setIcon(AllIcons.General.Error);
            append(value != null ? value.toString() : "Error", SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }

    private @NotNull SimpleTextAttributes getSimpleTextAttributes(final DefaultMutableTreeNode node, final DirectoryDto dir) {
        return switch (dir) {
            case TestCasesMainDirectoryDto ignored -> SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

            case TestRunsMainDirectoryDto ignored -> SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

            default -> (selectedNodes != null && selectedNodes.contains(node))
                    ? SimpleTextAttributes.GRAYED_ATTRIBUTES
                    : SimpleTextAttributes.REGULAR_ATTRIBUTES;
        };
    }

    private void statusTag(final DirectoryDto dir, final JTree tree) {
        // todo, why dont move all test run configs in .tr and make the json for the added test cases ?
        if (dir instanceof TestRunDirectoryDto runDir) {
            if (runDir.getRunStatus() != null) {
                append(runDir.getRunStatus().getLabel(), SimpleTextAttributes.GRAY_ATTRIBUTES);

            } else if (runDir.getIsLoadingStatus().compareAndSet(false, true)) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        Path jsonFile = runDir.getPath().resolve(runDir.getName() + ".json");

                        if (Files.exists(jsonFile)) {
                            TestRunDto dto = Config.getMapper().readValue(jsonFile.toFile(), TestRunDto.class);
                            runDir.setRunStatus(dto.getStatus());
                            ApplicationManager.getApplication().invokeLater(tree::repaint);
                        }

                    } catch (Exception e) {
                        System.err.println("Failed to load status for " + runDir.getName() + ": " + e.getMessage());
                    }
                });
            }

        }

        // todo, if (dir instanceof TestSetDirectoryDto setDir) {
        // todo, later, make a tag for test set if it is approved or still, need to set business and plan before implement
    }


}