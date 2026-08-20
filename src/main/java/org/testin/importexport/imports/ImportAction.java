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
import org.testin.codegen.Fqcn;
import org.testin.codegen.method.CreateTestMethod;
import org.testin.creator.CreateTestSet;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.testcase.Rank;
import org.testin.testcase.TestCaseOrder;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.NameSanitizer;
import org.testin.util.OptionalPlugin;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ImportAction extends AbstractProjectTreeAction {

    protected final @NotNull List<TestEditorAttributes> importAttributes = Arrays.stream(TestEditorAttributes.values())
            .filter(a -> a.can(Can.IMPORT))
            .toList();

    public ImportAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Import", "Import test cases from a file", AllIcons.ToolbarDecorator.Import);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Services.getInstance(p, Notifier.class).softShow(p, "Nothing to Import Into", "Select a directory in the Project Panel tree.");
            return;
        }

        final Object userObject = TreeValueUtil.valueOf(path.getLastPathComponent());

        if (!(userObject instanceof DirectoryDto dirDto) || !dirDto.isTestCaseContainer()) {
            Services.getInstance(p, Notifier.class).softShow(p, "Nothing to Import Into", "Select a Test Set, a Test Set Package, or the Test Cases directory.");
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
                Services.getInstance(p, Notifier.class).softShowCounted(p, "Imported", flatList.size());

            } else {
                int totalImported = 0;
                for (final Map.Entry<String, List<TestCaseDto>> entry : selectedCasesBySheet.entrySet()) {
                    final List<TestCaseDto> sheetCases = entry.getValue();

                    final String cName = NameSanitizer.removeSpecialChars(entry.getKey());
                    final Path newDirPath = targetPath.resolve(cName);
                    final DirectoryDto dir = new CreateTestSet(p).execute(cName, selectedDirDto, newDirPath);

                    final TestCaseDto tail = findExistingTail(p, newDirPath);
                    linkAndSaveTestCases(p, newDirPath, sheetCases, tail);

                    final TestSetDirectoryDto sheetDto = (TestSetDirectoryDto) dir;

                    for (final TestCaseDto tc : sheetCases) tc.setParent(sheetDto);

                    if (generateCode) generateTestMethods(p, sheetCases, cName);

                    totalImported += sheetCases.size();
                }
                // Same wording as the single-test-set branch above: the tester
                // chose which shape to import into, so the count is the news (#62).
                Services.getInstance(p, Notifier.class).softShowCounted(p, "Imported", totalImported);
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
            final List<String> fqcn = Fqcn.ofMethod(tc);
            syncInjector.executeSync(p, tc, fqcn);
        }
    }

    private void linkAndSaveTestCases(final @NotNull Project p, final @NotNull Path dirPath,
                                      final @NotNull List<TestCaseDto> testCases,
                                      final @Nullable TestCaseDto existingTail) {
        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        // After what is already in the set, in the order the sheet listed them.
        // Nothing that was there is touched: an import used to rewrite the case
        // that happened to be last.
        String rank = existingTail == null ? "" : existingTail.getOrder();

        for (final TestCaseDto currentTestCase : testCases) {
            rank = Rank.after(rank);
            currentTestCase.setOrder(rank);
        }

        // The imported cases keep the audit their file carried; the tail is an
        // existing case whose link changed, so it is an ordinary save and is
        // recorded as modified by whoever ran the import.
        for (final TestCaseDto tc : testCases) {
            indexer.putImportedTestCase(dirPath, tc);
        }
    }

    /**
     * The last case in the set, which is what an import lands after. From the
     * indexer, which is the source of truth for what is already there.
     */
    private @Nullable TestCaseDto findExistingTail(final @NotNull Project p, final @NotNull Path directory) {
        final List<TestCaseDto> existing =
                TestCaseOrder.ordered(Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(directory));

        return existing.isEmpty() ? null : existing.getLast();
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
