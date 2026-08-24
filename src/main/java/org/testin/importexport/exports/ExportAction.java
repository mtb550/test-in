package org.testin.importexport.exports;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.ui.dialogs.DestinationForm;
import org.testin.util.BackgroundWork;
import org.testin.util.Mapper;

import java.io.InputStream;
import java.util.*;

public class ExportAction extends AbstractProjectTreeAction {

    protected final @NotNull List<TestEditorAttributes> exportAttributes = Arrays.stream(TestEditorAttributes.values())
            .filter(a -> a.can(Can.EXPORT))
            .toList();

    public ExportAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Export", "Export test cases to a file", AllIcons.ToolbarDecorator.Export);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        TreeValueUtil.selectedDirectory(tree).ifPresent(this::exportFrom);
    }

    /**
     * Everything the action does once it knows which node it is exporting from.
     */
    private void exportFrom(final @NotNull DirectoryDto dirDto) {
        final @NotNull Optional<VirtualFile> resolved = resolveTargetDir(dirDto);
        if (resolved.isEmpty()) return;
        final @NotNull VirtualFile targetDir = resolved.get();

        BackgroundWork.run(p, "Reading test cases in " + dirDto.getName(), "Export Failed", gathering -> {
            final @NotNull Map<String, List<TestCaseDto>> sheets = gatherData(targetDir, dirDto);
            if (sheets.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(p, Notifier.class).softShow(p, "Export Empty", "No test cases found."));
                return;
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                // The framework dialog reports through this callback rather
                // than a return code, so the destination is never read back
                // out of a dialog that was canceled. It hands back the cases
                // the tester left ticked, not the ones gathered above.
                new ExportDialog(p, exportAttributes, sheets, targetDir, this::writeExport).show();
            });
        });
    }

    /**
     * The write, once the tester has chosen a file. Under its own bar and after
     * the dialog has closed: a workbook of several hundred cases took the EDT
     * with it, and the dialog sat there for all of it (#87).
     */
    private void writeExport(final DestinationForm.@NotNull Destination destination, final @NotNull Map<String, List<TestCaseDto>> selected) {
        final int cases = selected.values().stream().mapToInt(List::size).sum();

        BackgroundWork.run(p, "Exporting " + cases + " test cases to " + destination.file().getName(),
                "Export Failed", indicator -> {
                    destination.format().exportToFile(p, ExportAction.this, destination.file(), selected);

                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).softShowCounted(p, "Exported", cases));
                });
    }

    public @NotNull Map<String, List<TestCaseDto>> gatherData(final @NotNull VirtualFile targetDirectory, final @NotNull DirectoryDto dirDto) {
        final @NotNull Map<String, List<TestCaseDto>> allSheets = new LinkedHashMap<>();

        if (dirDto instanceof TestSetDirectoryDto) {
            allSheets.put(targetDirectory.getName(), loadTestCasesInOrder(p, targetDirectory));
        } else {
            for (final VirtualFile child : childrenOf(targetDirectory)) {
                if (child.isDirectory()) {
                    final @NotNull List<TestCaseDto> tcs = loadTestCasesInOrder(p, child);
                    if (!tcs.isEmpty()) {
                        allSheets.put(child.getName(), tcs);
                    }
                }
            }
        }
        return allSheets;
    }

    /**
     * Empty when the path is not in the VFS; a file resolves to its parent
     * directory.
     */
    public @NotNull Optional<VirtualFile> resolveTargetDir(final @NotNull DirectoryDto dirDto) {
        return Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString()))
                .map(target -> target.isDirectory() ? target : target.getParent());
    }

    /**
     * What a VFS directory holds. The platform answers "nothing readable here"
     * with a null array, which is the same as holding nothing.
     */
    private static VirtualFile @NotNull [] childrenOf(final @NotNull VirtualFile dir) {
        return Objects.requireNonNullElse(dir.getChildren(), VirtualFile.EMPTY_ARRAY);
    }

    public @NotNull List<TestCaseDto> loadTestCasesInOrder(final @NotNull Project p, final @NotNull VirtualFile dir) {
        final @NotNull List<TestCaseDto> loaded = new ArrayList<>();

        for (final VirtualFile file : childrenOf(dir)) {
            if (!file.isDirectory() && file.getName().endsWith(".json")) {
                try (InputStream is = file.getInputStream()) {
                    loaded.add(Services.getInstance(p, Mapper.class).readValue(is, TestCaseDto.class));
                } catch (final Exception ex) {
                    Logger.error("Loading test cases failed: " + ex.getMessage());
                }
            }
        }

        // The same order the editor shows, from the same rule - a sheet whose
        // rows are in a different order from the screen they were exported from
        // is a sheet nobody trusts.
        return new ArrayList<>(TestCaseOrder.ordered(loaded));
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeValueUtil.singleSelectedDirectory(tree)
                .filter(DirectoryDto::isTestCaseContainer)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
