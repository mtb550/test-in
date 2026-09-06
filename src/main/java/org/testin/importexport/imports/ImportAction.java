package org.testin.importexport.imports;

import org.testin.notifications.Done;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.codegen.GenType;
import org.testin.creator.CreateTestSet;
import org.testin.model.DirectoryType;
import org.testin.explorer.TreePanel;
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
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ImportAction extends AbstractProjectTreeAction {

    /**
     * How many test methods go into one write command.
     * <p>
     * Larger than it was, because what a batch costs changed. Each one is now a
     * single edit and a single reparse of the class file, and a reparse is
     * proportional to the whole file - so twenty-two small batches reparse a
     * growing file twenty-two times, where three large ones do it three times.
     * Still batched rather than done in one go, so the EDT comes back in
     * between and the progress bar can move.
     */
    private static final int METHODS_PER_COMMAND = 200;

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
                        Services.getInstance(p, Notifier.class).softRefuse(p, "Nothing to Import Into",
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

    private void executeImportWriteAction(final @NotNull Project p, final @NotNull DirectoryDto selectedDirDto, final @NotNull Map<String, List<TestCaseDto>> selectedCasesBySheet) {

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

            int imported = 0;
            for (final Map.Entry<TestSetDirectoryDto, List<TestCaseDto>> set
                    : targetSets(p, selectedDirDto, targetPath, selectedCasesBySheet).entrySet()) {

                final @NotNull TestSetDirectoryDto into = set.getKey();
                final @NotNull List<TestCaseDto> cases = set.getValue();
                final @NotNull Path setPath = into.getPath();

                linkAndSaveTestCases(p, setPath, cases, rankOfTail(p, setPath), indicator, imported, total);

                for (final TestCaseDto tc : cases) tc.setParent(into);

                if (generateCode) generateTestMethods(p, cases, into.getName(), indicator);

                imported += cases.size();
            }

            // The set the tester was standing on is reopened, so what they just
            // imported is in front of them. A container has no editor of its own.
            if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                onEdt(() -> Services.getInstance(p, EditorUtil.class).closeThenOpen(p, ts));
            }

            // The count is the news, whichever shape was imported into (#62).
            Services.getInstance(p, Notifier.class).softShowCounted(p, Done.IMPORTED, imported);

            report(total, startedAt, readyAt);

            // Asynchronous refresh: a synchronous recursive VFS refresh inside a
            // write action is disallowed by the platform and can freeze the IDE.
            // The indexer owns the refresh and runs the whole call, lookup
            // included, off the EDT.
            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(targetPath);
            ApplicationManager.getApplication().invokeLater(() ->
                    Services.getInstance(p, TreePanel.class).getProjectTree().refresh());
        });
    }

    /**
     * Which test set each sheet's cases are going into.
     * <p>
     * Two shapes, and only this decides between them: a test set takes every
     * sheet into itself, and a container takes one new set per sheet, named
     * after it. What happens to a set's cases afterwards - written, parented,
     * generated, counted - is the same either way, and used to be written twice.
     * <p>
     * The sets are made before any case is written, and on the EDT, because
     * making one generates its Java class and that is a write command.
     */
    private @NotNull Map<TestSetDirectoryDto, List<TestCaseDto>> targetSets(final @NotNull Project p, final @NotNull DirectoryDto selectedDirDto, final @NotNull Path targetPath, final @NotNull Map<String, List<TestCaseDto>> casesBySheet) {

        if (selectedDirDto instanceof TestSetDirectoryDto ts) {
            final @NotNull List<TestCaseDto> everything = new ArrayList<>();
            casesBySheet.values().forEach(everything::addAll);

            return Map.of(ts, everything);
        }

        final @NotNull Map<TestSetDirectoryDto, List<TestCaseDto>> sets = new LinkedHashMap<>();
        casesBySheet.forEach((sheetName, cases) -> {
            final @NotNull String name = NameSanitizer.removeSpecialChars(sheetName);
            final @NotNull Path path = targetPath.resolve(name);

            // A test set creator always answers with the set it made.
            sets.put(onEdtCompute(() -> {
                final @NotNull TestSetDirectoryDto made = (TestSetDirectoryDto) new CreateTestSet(p)
                        .execute(name, selectedDirDto, path)
                        .orElseThrow();

                // Asked for here, because the creator no longer generates. The
                // tree route runs the node type's generator after creating, and
                // this route has no such follow-up - so without this line an
                // imported set would arrive with no class at all.
                try {
                    DirectoryType.TS.getCodegen().execute(p, made);
                } catch (final Exception ex) {
                    Logger.error("Failed to create Java class: " + ex.getMessage());
                }

                return made;
            }), cases);
        });

        return sets;
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
    private void generateTestMethods(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull String targetName, final @NotNull ProgressIndicator indicator) {
        Logger.info("Import: generating test methods for '" + targetName + "' with " + testCases.size() + " cases");
        final long startedAt = System.currentTimeMillis();

        for (int from = 0; from < testCases.size(); from += METHODS_PER_COMMAND) {
            final @NotNull List<TestCaseDto> batch =
                    testCases.subList(from, Math.min(from + METHODS_PER_COMMAND, testCases.size()));
            final int written = from + batch.size();

            indicator.setText2("Generating test methods: " + written + " of " + testCases.size());
            // The batch as one, not case by case: the generator finds the
            // class and reformats it once for the whole group.
            onEdt(() -> GenType.CREATE_TEST_CASE.executeAll(p, batch));
        }

        Logger.info("Import: generated " + testCases.size() + " test methods in "
                + (System.currentTimeMillis() - startedAt) + "ms");
    }

    private void linkAndSaveTestCases(final @NotNull Project p, final @NotNull Path dirPath, final @NotNull List<TestCaseDto> testCases, final @NotNull String tailRank, final @NotNull ProgressIndicator indicator, final int done, final int total) {
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
            indexer.putTestCaseVerbatim(dirPath, tc);

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
