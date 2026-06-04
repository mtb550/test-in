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
import org.testin.util.Mapper;
import org.testin.util.logger.Log;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class CreateTestRun implements NodeCreator {
    private Project project;
    private TestRunDirectoryDto tr;

    @Override
    public DirectoryDto execute(final CreateTestNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
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

                    tr = DirectoryMapper.getInstance().testRunNode(project, newDirPath, parentDir);
                    saveSelectedToJSON(form, name, root, newDirPath, action.getProjectPanel(), tr);
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

        try (final Stream<Path> paths = Files.list(folder)) {
            paths.sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p)).thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(child -> {

                        if (Files.isDirectory(child)) {
                            node.add(buildDirectoryTree(child, false, thisNodeDto));

                        } else if (child.toString().endsWith(".json")) {
                            try {
                                final TestCaseDto tc = Mapper.readValue(child.toFile(), TestCaseDto.class);
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
        return node;
    }

    private Object resolveDirectoryObject(final Path folder, final DirectoryDto parentDir) {
        if (Files.exists(folder.resolve(DirectoryType.TSP.getMarker())))
            return DirectoryMapper.getInstance().testSetPackageNode(project, folder, parentDir);

        if (Files.exists(folder.resolve(DirectoryType.TS.getMarker())))
            return DirectoryMapper.getInstance().testSetNode(project, folder, parentDir);

        if (Files.exists(folder.resolve(DirectoryType.TCD.getMarker())))
            return DirectoryMapper.getInstance().testCasesRootNode(project, folder, parentDir);

        throw new RuntimeException("Could not resolve directory " + folder + ", parent: " + parentDir.getClass().getSimpleName());
    }

    private void collectCheckedItems(final CheckedTreeNode node, final List<TestRunItems> items) {
        if (node.getUserObject() instanceof TestCaseDto tc && node.isChecked()) {
            final TestRunItems item = new TestRunItems();
            item.setId(tc.getId());
            item.setStatus(TestStatus.PENDING);

            if (node.getParent() instanceof DefaultMutableTreeNode pNode && pNode.getUserObject() instanceof DirectoryDto dir) {
                item.setPath(dir.getPath2() != null ? new ArrayList<>(dir.getPath2()) : new ArrayList<>());
            }
            items.add(item);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectCheckedItems((CheckedTreeNode) node.getChildAt(i), items);
        }
    }


    private void saveSelectedToJSON(final RunCreationForm form, final String runName, final CheckedTreeNode root, final Path savePath, final ProjectPanel projectPanel, final TestRunDirectoryDto tr) {
        final TestRunDto run = new TestRunDto();
        form.populateConfiguration(run);

        final String fileName = runName + ".json";
        run.setRunName(fileName);
        run.setCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        run.setStatus(TestRunStatus.CREATED);

        final List<TestRunItems> items = new ArrayList<>();
        collectCheckedItems(root, items);
        run.setResults(items);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Files.createDirectories(savePath);
                Path newJsonPath = savePath.resolve(fileName);

                byte[] jsonBytes = Mapper.writeValueAsBytes(run);
                Files.write(newJsonPath, jsonBytes);

                Path trMarkerPath = savePath.resolve(DirectoryType.TR.getMarker());
                if (Files.notExists(trMarkerPath))
                    Files.createFile(trMarkerPath);

                VirtualFile virtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(savePath.toFile());
                if (virtualDir != null)
                    virtualDir.refresh(false, true);

                ApplicationManager.getApplication().invokeLater(() -> {
                    projectPanel.getTestRunTreeBuilder().buildTree(projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());
                    EditorUtil.getInstance().openEditorIfNotOpen(project, tr);

                });
            } catch (final Exception e) {
                Log.error("Exception: " + e.getMessage());
            }
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