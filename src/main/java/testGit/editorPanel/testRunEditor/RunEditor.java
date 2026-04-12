package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.pojo.Config;
import testGit.pojo.DirectoryMapper;
import testGit.pojo.DirectoryType;
import testGit.pojo.EditorType;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.TestRunDto;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;
import testGit.pojo.dto.dirs.TestRunDirectoryDto;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class RunEditor {

    public static void open(final TestRunDirectoryDto tr, final ProjectPanel projectPanel) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Path jsonFilePath = tr.getPath().resolve(tr.getName() + ".json");

                if (!Files.exists(jsonFilePath)) {
                    System.err.println("JSON file not found: " + jsonFilePath);
                    return;
                }

                final TestRunDto metadata = Config.getMapper().readValue(jsonFilePath.toFile(), TestRunDto.class);

                final UnifiedVirtualFile virtualFile = new UnifiedVirtualFile(
                        tr,
                        new DefaultTreeModel(new DefaultMutableTreeNode("Loading...")),
                        new ArrayList<>(),
                        EditorType.TEST_RUN_OPENING,
                        projectPanel
                );
                virtualFile.setMetadata(metadata);

                ApplicationManager.getApplication().invokeLater(() ->
                        Optional.ofNullable(FileEditorManager.getInstance(Config.getProject()))
                                .ifPresent(manager -> manager.openFile(virtualFile, true))
                );

            } catch (final IOException e) {
                System.err.println("Failed to open Test Run: " + e.getMessage());
            }
        });
    }

    public static void create(final TestRunDirectoryDto tr, final ProjectPanel projectPanel, final TestProjectDirectoryDto tp, final TestRunDto metadata) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final Path testCasesPath = tp.getTestCasesDirectory().getPath();

            final DefaultTreeModel fullModel = new DefaultTreeModel(buildDirectoryTree(testCasesPath, true));

            final UnifiedVirtualFile virtualFile = new UnifiedVirtualFile(
                    tr,
                    fullModel,
                    new ArrayList<>(),
                    EditorType.TEST_RUN_CREATION,
                    projectPanel
            );
            virtualFile.setMetadata(metadata);

            ApplicationManager.getApplication().invokeLater(() ->
                    Optional.ofNullable(FileEditorManager.getInstance(Config.getProject()))
                            .ifPresent(manager -> manager.openFile(virtualFile, true))
            );
        });
    }

    private static DefaultMutableTreeNode buildDirectoryTree(final Path folder, final boolean isRoot) {
        final Object label = isRoot
                ? "Test Cases (" + folder.getParent().getFileName().toString() + ")"
                : resolveDirectoryObject(folder);

        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);

        if (!Files.exists(folder) || !Files.isDirectory(folder)) return node;

        try (final Stream<Path> paths = Files.list(folder)) {
            paths.sorted(Comparator
                            .comparing((Path p) -> !Files.isDirectory(p))
                            .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(child -> {
                        if (Files.isDirectory(child)) {
                            node.add(buildDirectoryTree(child, false));
                        } else if (child.toString().endsWith(".json")) {
                            try {
                                final TestCaseDto tc = Config.getMapper().readValue(child.toFile(), TestCaseDto.class);
                                node.add(new DefaultMutableTreeNode(tc));
                            } catch (final Exception e) {
                                System.err.println("Failed to parse test case: " + child.getFileName());
                            }
                        }
                    });
        } catch (final IOException e) {
            System.err.println("Failed to read directory tree: " + folder);
            e.printStackTrace(System.err);
        }

        return node;
    }

    private static Object resolveDirectoryObject(final Path folder) {
        if (Files.exists(folder.resolve(DirectoryType.TSP.getMarker())))
            return DirectoryMapper.testSetPackageNode(folder);
        if (Files.exists(folder.resolve(DirectoryType.TS.getMarker())))
            return DirectoryMapper.testSetNode(folder);

        return folder.getFileName().toString();
    }
}