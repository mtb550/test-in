package org.testin.importexport.imports;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.codegen.GenType;
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
import org.testin.util.BackgroundWork;
import org.testin.util.EditorUtil;
import org.testin.util.NameSanitizer;
import org.testin.util.OptionalPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ImportAction extends AbstractProjectTreeAction {

    /**
     * How many test methods go into one write command. Small enough that the
     * EDT comes back between batches, large enough that a sheet is not a
     * thousand separate undo entries.
     */
    private static final int METHODS_PER_COMMAND = 25;

    protected final @NotNull List<TestEditorAttributes> importAttributes = Arrays.stream(TestEditorAttributes.values())
            .filter(a -> a.can(Can.IMPORT))
            .toList();

    public ImportAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Import", "Import test cases from a file", AllIcons.ToolbarDecorator.Import);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreeValueUtil.directoryAt(tree.getSelectionPath())
                .filter(DirectoryDto::isTestCaseContainer)
                .ifPresentOrElse(this::openImportDialog, () ->
                        Services.getInstance(p, Notifier.class).softShow(p, "Nothing to Import Into",
                                "Select a Test Set, a Test Set Package, or the Test Cases directory."));
    }

    private void openImportDialog(final @NotNull DirectoryDto dirDto) {
        // The framework dialog reports through this callback rather than a
        // return code, and only ever with a non-empty selection.
        new ImportDialog(p, importAttributes,
                (file, format) -> format.importToFile(p, ImportAction.this, file),
                selectedCasesBySheet -> executeImportWriteAction(p, dirDto, selectedCasesBySheet))
                .show();
    }

    private void executeImportWriteAction(final @NotNull Project p, final @NotNull DirectoryDto selectedDirDto,
                                          final @NotNull Map<String, List<TestCaseDto>> selectedCasesBySheet) {

        final @NotNull Path targetPath = selectedDirDto.getPath();

        // Checked once up front: without the Java plugin the import still runs,
        // only the test-method generation is skipped (with a one-time notice).
        final boolean generateCode = OptionalPlugin.JAVA.isAvailableOrWarnOnce(p);

        final int total = selectedCasesBySheet.values().stream().mapToInt(List::size).sum();

        // Off the EDT and under a bar. Every case is a file of its own, written
        // through java.nio by the indexer, so the loop belongs on a background
        // thread; what needs the EDT asks for it by name below (#87).
        BackgroundWork.run(p, "Importing " + total + " test cases into " + selectedDirDto.getName(),
                "Import Failed", indicator -> {
            indicator.setIndeterminate(false);
            final long startedAt = System.currentTimeMillis();

            // Once for the import, not once per sheet. Adding a test method
            // resolves the references inside it, resolving reads the stub index,
            // and reading the index while it is being rebuilt waits for the
            // rebuild. Waiting here costs a background thread; waiting inside a
            // write action costs the whole IDE.
            if (generateCode) {
                indicator.setText2("Waiting for indexing to finish");
                DumbService.getInstance(p).waitForSmartMode();
            }
            final long readyAt = System.currentTimeMillis();

            if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                final @NotNull List<TestCaseDto> flatList = new ArrayList<>();
                selectedCasesBySheet.values().forEach(flatList::addAll);

                linkAndSaveTestCases(p, targetPath, flatList, rankOfTail(p, targetPath), indicator, 0, total);

                for (final TestCaseDto tc : flatList) tc.setParent(ts);

                if (generateCode) generateTestMethods(p, flatList, ts.getName(), indicator);

                onEdt(() -> Services.getInstance(p, EditorUtil.class).closeThenOpen(p, ts));
                Services.getInstance(p, Notifier.class).softShowCounted(p, "Imported", flatList.size());

            } else {
                int totalImported = 0;
                for (final Map.Entry<String, List<TestCaseDto>> entry : selectedCasesBySheet.entrySet()) {
                    final @NotNull List<TestCaseDto> sheetCases = entry.getValue();

                    final @NotNull String cName = NameSanitizer.removeSpecialChars(entry.getKey());
                    final @NotNull Path newDirPath = targetPath.resolve(cName);
                    // A test set creator always answers with the set it made. On
                    // the EDT: making one generates its Java class, which is a
                    // write command.
                    final @NotNull TestSetDirectoryDto sheetDto = onEdtCompute(() ->
                            (TestSetDirectoryDto) new CreateTestSet(p)
                                    .execute(cName, selectedDirDto, newDirPath)
                                    .orElseThrow());

                    linkAndSaveTestCases(p, newDirPath, sheetCases, rankOfTail(p, newDirPath),
                            indicator, totalImported, total);

                    for (final TestCaseDto tc : sheetCases) tc.setParent(sheetDto);

                    if (generateCode) generateTestMethods(p, sheetCases, cName, indicator);

                    totalImported += sheetCases.size();
                }
                // Same wording as the single-test-set branch above: the tester
                // chose which shape to import into, so the count is the news (#62).
                Services.getInstance(p, Notifier.class).softShowCounted(p, "Imported", totalImported);
            }

            report(total, startedAt, readyAt);

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
     * What the import spent, in the log, every time one runs.
     * <p>
     * Here because the alternative is guessing. An import is a file per case and
     * a PSI method per case, and which of the two dominates depends on the sheet
     * and on whether the IDE was indexing - so the answer is measured rather
     * than assumed, and it is in the log the next time somebody asks.
     */
    private static void report(final int cases, final long startedAt, final long readyAt) {
        final long finishedAt = System.currentTimeMillis();
        Logger.info("Import: " + cases + " cases in " + (finishedAt - startedAt) + "ms"
                + " (waiting for the index " + (readyAt - startedAt) + "ms,"
                + " writing and generating " + (finishedAt - readyAt) + "ms)");
    }

    /**
     * Runs work that must be on the EDT and waits for it, so the steps of an
     * import stay in the order the import wrote them. Safe to wait from here:
     * the bar is a background task, so the EDT is not waiting on us.
     */
    private static void onEdt(final @NotNull Runnable work) {
        // Explicitly non-modal. The default modality is the one captured where
        // the task was started, which was the import dialog - and that dialog is
        // closed by now, so waiting on it would wait forever.
        ApplicationManager.getApplication().invokeAndWait(work, ModalityState.nonModal());
    }

    /**
     * The same, for a step whose answer the next one needs.
     */
    private static <T> @NotNull T onEdtCompute(final @NotNull Supplier<T> work) {
        final @NotNull List<T> answer = new ArrayList<>(1);
        onEdt(() -> answer.add(work.get()));
        return answer.getFirst();
    }

    /**
     * Generates the automation test method for each imported case, in batches.
     * The target name only labels the log line.
     * <p>
     * Through the registry like every other caller: naming the generator class
     * made this the one place that could keep working its own way while the
     * rest of the plugin changed how a method is made.
     * <p>
     * Batched rather than one command for the whole sheet, which is what #51
     * asked for and what froze the IDE for forty-nine seconds on a sheet of five
     * hundred and fifty. A write action cannot be interrupted, so one command
     * around every case holds the EDT until the last one is written - the
     * progress bar cannot even repaint. Twenty-five at a time releases it
     * between batches. The cost is an undo entry per batch instead of one for
     * the sheet, and a single undo of a fifty-second operation was not a thing
     * anyone could use.
     */
    private void generateTestMethods(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases,
                                     final @NotNull String targetName, final @NotNull ProgressIndicator indicator) {
        Logger.info("Import: generating test methods for '" + targetName + "' with " + testCases.size() + " cases");
        final long startedAt = System.currentTimeMillis();

        for (int from = 0; from < testCases.size(); from += METHODS_PER_COMMAND) {
            final @NotNull List<TestCaseDto> batch =
                    testCases.subList(from, Math.min(from + METHODS_PER_COMMAND, testCases.size()));
            final int written = from + batch.size();

            indicator.setText2("Generating test methods: " + written + " of " + testCases.size());
            onEdt(() -> WriteCommandAction.runWriteCommandAction(p, "Create Test Methods", null, () -> {
                for (final TestCaseDto tc : batch) {
                    GenType.CREATE_TEST_CASE.getAction().execute(p, tc);
                }
            }));
        }

        Logger.info("Import: generated " + testCases.size() + " test methods in "
                + (System.currentTimeMillis() - startedAt) + "ms");
    }

    private void linkAndSaveTestCases(final @NotNull Project p, final @NotNull Path dirPath,
                                      final @NotNull List<TestCaseDto> testCases,
                                      final @NotNull String tailRank,
                                      final @NotNull ProgressIndicator indicator,
                                      final int done, final int total) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        // After what is already in the set, in the order the sheet listed them.
        // Nothing that was there is touched: an import used to rewrite the case
        // that happened to be last.
        String rank = tailRank;

        for (final TestCaseDto currentTestCase : testCases) {
            rank = Rank.after(rank);
            currentTestCase.setOrder(rank);
        }

        // The imported cases keep the audit their file carried; the tail is an
        // existing case whose link changed, so it is an ordinary save and is
        // recorded as modified by whoever ran the import.
        int written = 0;
        for (final TestCaseDto tc : testCases) {
            indexer.putImportedTestCase(dirPath, tc);

            written++;
            indicator.setFraction((done + written) / (double) total);
            indicator.setText2(tc.getDescription());
        }
    }

    /**
     * The last case in the set, which is what an import lands after. From the
     * indexer, which is the source of truth for what is already there.
     */
    private @NotNull String rankOfTail(final @NotNull Project p, final @NotNull Path directory) {
        return findExistingTail(p, directory).map(TestCaseDto::getOrder).orElse("");
    }

    private @NotNull Optional<TestCaseDto> findExistingTail(final @NotNull Project p, final @NotNull Path directory) {
        final @NotNull List<TestCaseDto> existing =
                TestCaseOrder.ordered(Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(directory));

        return existing.isEmpty() ? Optional.empty() : Optional.of(existing.getLast());
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
