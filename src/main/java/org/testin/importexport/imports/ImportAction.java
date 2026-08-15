package org.testin.importexport.imports;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.codegen.method.CreateTestMethod;
import org.testin.creator.CreateTestSet;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.OptionalPlugin;
import org.testin.util.Tools;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ImportAction extends AbstractProjectTreeAction {

    protected final @NotNull List<TestEditorAttributes> importAttributes = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isImportable)
            .toList();

    public ImportAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Import", "Import test cases from a file", AllIcons.ToolbarDecorator.Import);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Services.getInstance(p, Notifier.class).error(p, "Import Error", "Please select a directory in the Project Panel tree.");
            return;
        }

        final Object userObject = TreeValueUtil.valueOf(path.getLastPathComponent());

        if (!(userObject instanceof DirectoryDto dirDto) || !dirDto.isTestCaseContainer()) {
            Services.getInstance(p, Notifier.class).error(p, "Import Error", "Please select a valid Test Set, Test Set Package, or Test Cases Directory.");
            return;
        }

        // The framework dialog reports through this callback rather than a
        // return code, and only ever with a non-empty selection.
        new ImportDialog(p, importAttributes,
                (file, format) -> format.importToFile(p, ImportAction.this, file),
                selectedCasesBySheet -> executeImportWriteAction(p, dirDto, selectedCasesBySheet))
                .show();
    }

    private void executeImportWriteAction(final @NotNull Project p, final @NotNull DirectoryDto selectedDirDto,
                                          final @NotNull Map<String, List<TestCaseDto>> selectedCasesBySheet) {

        final Path targetPath = selectedDirDto.getPath();

        // Checked once up front: without the Java plugin the import still runs,
        // only the test-method generation is skipped (with a one-time notice).
        final boolean generateCode = OptionalPlugin.JAVA.isAvailableOrWarnOnce(p);

        ApplicationManager.getApplication().runWriteAction(() -> {
            if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                final TestCaseDto tail = findExistingTail(p, targetPath);
                final List<TestCaseDto> flatList = new ArrayList<>();
                selectedCasesBySheet.values().forEach(flatList::addAll);

                linkAndSaveTestCases(p, targetPath, flatList, tail);

                for (final TestCaseDto tc : flatList) tc.setParent(ts);

                if (generateCode) generateTestMethods(p, flatList, ts.getName());

                Services.getInstance(p, EditorUtil.class).closeThenOpen(p, ts);
                Services.getInstance(p, Notifier.class).info(p, "Import Complete", "Successfully imported " + flatList.size() + " test cases.");

            } else {
                int totalImported = 0;
                for (final Map.Entry<String, List<TestCaseDto>> entry : selectedCasesBySheet.entrySet()) {
                    final List<TestCaseDto> sheetCases = entry.getValue();

                    final String cName = Services.getInstance(p, Tools.class).removeSpecialChars(entry.getKey());
                    final Path newDirPath = targetPath.resolve(cName);
                    final DirectoryDto dir = new CreateTestSet(p).execute(cName, selectedDirDto, newDirPath);

                    final TestCaseDto tail = findExistingTail(p, newDirPath);
                    linkAndSaveTestCases(p, newDirPath, sheetCases, tail);

                    final TestSetDirectoryDto sheetDto = (TestSetDirectoryDto) dir;

                    for (final TestCaseDto tc : sheetCases) tc.setParent(sheetDto);

                    if (generateCode) generateTestMethods(p, sheetCases, cName);

                    totalImported += sheetCases.size();
                }
                Services.getInstance(p, Notifier.class).info(p, "Import Complete", "Successfully imported " + totalImported + " test cases into separate Test Sets.");
            }

            // Asynchronous refresh: a synchronous recursive VFS refresh inside a
            // write action is disallowed by the platform and can freeze the IDE.
            // The indexer owns the refresh and runs the whole call, lookup
            // included, off the EDT.
            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(targetPath);
            ApplicationManager.getApplication().invokeLater(() ->
                    Services.getInstance(p, ExplorerPanel.class).getProjectTree().refresh());

        });
    }

    /**
     * Generates the automation test method for each imported case. The target
     * name only labels the log line.
     */
    private void generateTestMethods(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases,
                                     final @NotNull String targetName) {
        Logger.info("Import: generating test methods for '" + targetName + "' with " + testCases.size() + " cases");

        final CreateTestMethod syncInjector = new CreateTestMethod();
        for (final TestCaseDto tc : testCases) {
            final List<String> fqcn = Services.getInstance(p, Tools.class).buildFqcnMethod(tc);
            syncInjector.executeSync(p, tc, fqcn);
        }
    }

    private void linkAndSaveTestCases(final @NotNull Project p, final @NotNull Path dirPath,
                                      final @NotNull List<TestCaseDto> testCases,
                                      final @Nullable TestCaseDto existingTail) {
        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        TestCaseDto previousNode = existingTail;

        for (final TestCaseDto currentTestCase : testCases) {
            if (previousNode == null) {
                currentTestCase.setIsHead(true);

            } else {
                currentTestCase.setIsHead(null);
                previousNode.setNext(currentTestCase.getId());
            }
            currentTestCase.setNext(null);
            previousNode = currentTestCase;
        }

        if (existingTail != null) {
            indexer.putTestCase(dirPath, existingTail);
        }

        for (final TestCaseDto tc : testCases) {
            indexer.putTestCase(dirPath, tc);
        }
    }

    /**
     * The indexer is the source of truth for existing test cases — no need to
     * re-read JSON files from disk to find the linked-list tail.
     */
    private @Nullable TestCaseDto findExistingTail(final @NotNull Project p, final @NotNull Path directory) {
        return Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(directory).stream()
                .filter(tc -> tc.getNext() == null)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final DirectoryDto selected = TreeValueUtil.singleSelectedDirectory(tree);

        e.getPresentation().setEnabled(selected != null && selected.isTestCaseContainer());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
