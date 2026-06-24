package org.testin.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckedTreeNode;
import org.testin.pojo.*;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.ui.RunCreationForm;
import org.testin.util.EditorUtil;
import org.testin.util.FilesUtil;
import org.testin.util.Mapper;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class CreateTestRun implements NodeCreator {
    private Project project;
    private TestRunDirectoryDto tr;

    @Override
    public DirectoryDto execute(final CreateTreeNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        this.project = project;
        final TestProjectDirectoryDto tp = action.getProjectPanel().getTestProjectSelector().getSelectedTestProject().getItem();

        final DirectoryDto testCasesRoot = tp.getTestCasesDirectory();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            final Path testCasesPath = testCasesRoot.getPath();
            final DefaultMutableTreeNode fullModelNode = buildDirectoryTree(testCasesPath, true, testCasesRoot);
            final CheckedTreeNode root = convertToCheckedNodes(fullModelNode);

            ApplicationManager.getApplication().invokeLater(() -> {

                final RunCreationForm form = new RunCreationForm(name, root, Collections.emptyMap());

                DialogBuilder dialogBuilder = new DialogBuilder(project);
                dialogBuilder.setTitle("Create Test Run");
                dialogBuilder.setCenterPanel(form.getMainPanel());
                dialogBuilder.addOkAction().setText("Save Test Run");
                dialogBuilder.addCancelAction();

                dialogBuilder.setOkOperation(() -> {
                    dialogBuilder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);

                    tr = Services.getInstance(project, DirectoryMapper.class).readTestRunNode(project, newDirPath, parentDir);
                    saveSelectedToJSON(form, root, newDirPath, action.getProjectPanel(), tr);
                });

                dialogBuilder.show();
            });
        });

        return tr;
    }

    private DefaultMutableTreeNode buildDirectoryTree(final Path folder, final boolean isRoot, final DirectoryDto parentOfThisNode) {
        final Object label = isRoot ? parentOfThisNode : resolveDirectoryObject(folder, parentOfThisNode);
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);
        final DirectoryDto thisNodeDto = (label instanceof DirectoryDto) ? (DirectoryDto) label : null;

        if (thisNodeDto == null || !Files.exists(folder) || !Files.isDirectory(folder)) return node;

        final boolean isTestSet = Files.exists(folder.resolve(DirectoryType.TS.getMarker()));

        if (isTestSet) {
            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
            indexer.awaitIndexing();

            final List<TestCaseDto> testCases = indexer.getTestCasesForTestSet(folder);
            for (final TestCaseDto tc : testCases) {
                node.add(new DefaultMutableTreeNode(tc));
            }
        } else {
            try (final Stream<Path> paths = Files.list(folder)) {
                paths.sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p)).thenComparing(p -> p.getFileName().toString().toLowerCase()))
                        .forEach(child -> {
                            if (Files.isDirectory(child)) {
                                node.add(buildDirectoryTree(child, false, thisNodeDto));
                            } else if (child.toString().endsWith(".json")) {
                                try {
                                    final TestCaseDto tc = Services.getInstance(project, Mapper.class).readValue(child.toFile(), TestCaseDto.class);
                                    node.add(new DefaultMutableTreeNode(tc));
                                } catch (final Exception e1) {
                                    Log.info("Failed to parse " + child);
                                    Log.error("Exception: " + e1.getMessage());
                                }
                            }
                        });
            } catch (final Exception e2) {
                Log.error("Exception: " + e2.getMessage());
            }
        }

        return node;
    }

    private Object resolveDirectoryObject(final Path folder, final DirectoryDto parentDir) {
        if (Files.exists(folder.resolve(DirectoryType.TSP.getMarker())))
            return Services.getInstance(project, DirectoryMapper.class).readTestSetPackageNode(project, folder, parentDir);

        if (Files.exists(folder.resolve(DirectoryType.TS.getMarker())))
            return Services.getInstance(project, DirectoryMapper.class).readTestSetNode(project, folder, parentDir);

        if (Files.exists(folder.resolve(DirectoryType.TCD.getMarker())))
            return Services.getInstance(project, DirectoryMapper.class).readTestCasesRootNode(project, folder, parentDir);

        throw new RuntimeException("Could not resolve directory " + folder + ", parent: " + parentDir.getClass().getSimpleName());
    }

    private void collectCheckedItems(final CheckedTreeNode node, final List<TestRunItems> items) {
        if (node.getUserObject() instanceof TestCaseDto tc && node.isChecked()) {
            final TestRunItems item = new TestRunItems();
            item.setId(tc.getId());
            item.setStatus(TestStatus.PENDING);

            items.add(item);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectCheckedItems((CheckedTreeNode) node.getChildAt(i), items);
        }
    }


    private void saveSelectedToJSON(final RunCreationForm form, final CheckedTreeNode root, final Path savePath, final ProjectPanel projectPanel, final TestRunDirectoryDto tr) {
        final TestRunDto run = new TestRunDto();
        form.populateConfiguration(run);

        final List<TestRunItems> items = new ArrayList<>();
        collectCheckedItems(root, items);
        run.setResults(items);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Services.getInstance(project, FilesUtil.class).createDirectories(savePath);
            Services.getInstance(project, ProjectIndexer.class).putTestRun(savePath, run);

            TestRunMarker marker = TestRunMarker.builder()
                    .status(TestRunStatus.CREATED)
                    .createdBy(System.getProperty("user.name", ""))
                    .build();

            tr.setMarker(marker);

            Services.getInstance(project, ProjectIndexer.class).addTestRunDir(tr);
            Services.getInstance(project, ProjectIndexer.class).updateRunMarker(project, savePath, marker);

            VirtualFile virtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(savePath.toFile());
            if (virtualDir != null)
                virtualDir.refresh(false, true);

            ApplicationManager.getApplication().invokeLater(() -> {
                projectPanel.getTestRunTreeBuilder().buildTree(projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());
                Services.getInstance(project, EditorUtil.class).openEditorIfNotOpen(project, tr);

            });

        });
    }

    private CheckedTreeNode convertToCheckedNodes(final DefaultMutableTreeNode node) {
        final Object userObj = node.getUserObject();
        final CheckedTreeNode newNode = new CheckedTreeNode(userObj);
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }

}