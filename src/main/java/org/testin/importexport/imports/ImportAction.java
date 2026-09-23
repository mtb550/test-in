/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.importexport.imports;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.codegen.CodeOn;
import org.testin.codegen.GenType;
import org.testin.codegen.JavaCode;
import org.testin.creator.CreateTestSet;
import org.testin.editor.TestinEditors;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testcase.Rank;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestEditorAttributes.Can;
import org.testin.testcase.TestEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.NameSanitizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ImportAction extends DumbAwareAction {
    public static final @NotNull String NAME = Bundle.message("import.action.name");

    private static final int METHODS_PER_COMMAND = 200;

    private static void report(final int testCases, final long startedAt, final long readyAt) {
        final long finishedAt = System.currentTimeMillis();
        Logger.info("Import: " + testCases + " cases in " + (finishedAt - startedAt) + "ms"
                + " (waiting for the index " + (readyAt - startedAt) + "ms,"
                + " writing and generating " + (finishedAt - readyAt) + "ms)");
    }

    private static void onEdt(final @NotNull Runnable work) {
        ApplicationManager.getApplication().invokeAndWait(work, ModalityState.nonModal());
    }

    private static <T> @NotNull T onEdtCompute(final @NotNull Supplier<T> work) {
        final @NotNull List<T> answer = new ArrayList<>(1);
        onEdt(() -> answer.add(work.get()));
        return answer.getFirst();
    }

    // UC-SHARE-005
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.firstSelected(e, DirectoryDto.class)
                .filter(DirectoryDto::isTestCaseContainer)
                .ifPresent(dir -> new Work(p).openImportDialog(dir));
    }

    // UC-SHARE-005
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.singleSelectedNode(e)
                .filter(DirectoryDto::isTestCaseContainer)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private record Work(@NotNull Project p) {
        private void openImportDialog(final @NotNull DirectoryDto dirDto) {
            new ImportDialog(p, TestEditorAttributes.all(Can.IMPORT),
                    (file, format) -> format.importToFile(p, file),
                    selectedTestCasesBySheet -> executeImportWriteAction(dirDto, selectedTestCasesBySheet))
                    .show();
        }

        // UC-SHARE-005, UC-SHARE-006
        private void executeImportWriteAction(final @NotNull DirectoryDto selectedDirDto, final @NotNull Map<String, List<TestCaseDto>> selectedTestCasesBySheet) {
            final @NotNull Path targetPath = selectedDirDto.getPath();

            final boolean generateCode = CodeOn.isOnOrWarnOnce(p);

            final int total = selectedTestCasesBySheet.values().stream().mapToInt(List::size).sum();

            BackgroundWork.run(p, Bundle.message("import.task.importing", String.valueOf(total), selectedDirDto.getName()),
                    Bundle.message("import.failed.title"), indicator -> {
                        indicator.setIndeterminate(false);
                        final long startedAt = System.currentTimeMillis();

                        if (generateCode) {
                            indicator.setText2(Bundle.message("import.progress.indexing"));
                            DumbService.getInstance(p).waitForSmartMode();
                        }
                        final long readyAt = System.currentTimeMillis();

                        int imported = 0;

                        final @NotNull Set<String> stillEmpty = new LinkedHashSet<>();

                        try {
                            final @NotNull Map<TestSetDirectoryDto, List<TestCaseDto>> targets =
                                    targetSets(selectedDirDto, targetPath, selectedTestCasesBySheet);
                            targets.keySet().forEach(made -> stillEmpty.add(made.getName()));

                            for (final Map.Entry<TestSetDirectoryDto, List<TestCaseDto>> set : targets.entrySet()) {
                                final @NotNull TestSetDirectoryDto into = set.getKey();
                                final @NotNull List<TestCaseDto> testCases = set.getValue();
                                final @NotNull Path setPath = into.getPath();

                                final @NotNull List<TestCaseDto> written = linkAndSaveTestCases(setPath, testCases, rankOfTail(setPath), indicator, imported, total);
                                if (!written.isEmpty()) stillEmpty.remove(into.getName());

                                for (final TestCaseDto tc : testCases) tc.setParent(into);

                                if (generateCode) generateTestMethods(written, into.getName(), indicator);

                                imported += written.size();

                                // UC-SHARE-007, Rule-SHARE-037
                                if (indicator.isCanceled()) break;
                            }
                        } catch (final Exception ex) {
                            // UC-SHARE-007, Rule-SHARE-037
                            Logger.error("Import failed after at least " + imported + " of " + total + ": " + FailureText.of(ex));

                            Services.getInstance(p, Notifier.class).error(p, Bundle.message("import.failed.title"),
                                    Bundle.message("import.failed.partial", String.valueOf(imported), String.valueOf(total), FailureText.of(ex)));

                            refreshTarget(targetPath);
                            return;
                        }

                        if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                            onEdt(() -> Services.getInstance(p, TestinEditors.class).closeThenOpen(p, ts));
                        }

                        Services.getInstance(p, Notifier.class).softShowCounted(p, Done.IMPORTED, imported);

                        reportEmptySets(List.copyOf(stillEmpty));

                        report(total, startedAt, readyAt);

                        refreshTarget(targetPath);
                    });
        }

        private void refreshTarget(final @NotNull Path targetPath) {
            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(targetPath);
            ApplicationManager.getApplication().invokeLater(() ->
                    Services.getInstance(p, TreePanel.class).getProjectTree().refresh());
        }

        // UC-SHARE-007, Rule-SHARE-037
        private void reportEmptySets(final @NotNull List<String> empty) {
            if (empty.isEmpty()) return;

            final @NotNull String named = empty.stream().limit(5).collect(Collectors.joining(", "));
            final @NotNull String rest = empty.size() > 5
                    ? Bundle.message("indexer.more", String.valueOf(empty.size() - 5))
                    : "";
            final @NotNull String count = empty.size() == 1
                    ? Bundle.message("import.empty.one")
                    : Bundle.message("import.empty.many", String.valueOf(empty.size()));

            Services.getInstance(p, Notifier.class).warn(p, Bundle.message("import.empty.title"),
                    Bundle.message("import.empty.message", count, named, rest));
        }

        // UC-SHARE-006, Rule-SHARE-031
        private @NotNull Map<TestSetDirectoryDto, List<TestCaseDto>> targetSets(final @NotNull DirectoryDto selectedDirDto, final @NotNull Path targetPath, final @NotNull Map<String, List<TestCaseDto>> testCasesBySheet) {
            if (selectedDirDto instanceof TestSetDirectoryDto ts) {
                final @NotNull List<TestCaseDto> everything = new ArrayList<>();
                testCasesBySheet.values().forEach(everything::addAll);

                return Map.of(ts, everything);
            }

            final @NotNull Map<TestSetDirectoryDto, List<TestCaseDto>> sets = new LinkedHashMap<>();
            testCasesBySheet.forEach((sheetName, testCases) -> {
                final @NotNull String name = NameSanitizer.removeSpecialChars(sheetName);
                final @NotNull Path path = targetPath.resolve(name);

                sets.put(onEdtCompute(() -> {
                    final @NotNull TestSetDirectoryDto made = (TestSetDirectoryDto) new CreateTestSet(p)
                            .execute(name, selectedDirDto, path)
                            .orElseThrow(() -> new IllegalStateException(Bundle.message("import.set.not.made", name)));

                    try {
                        JavaCode.of(DirectoryType.TS).getCreated().execute(p, made);
                    } catch (final Exception ex) {
                        Logger.error("Failed to create Java class: " + ex.getMessage());
                    }

                    return made;
                }), testCases);
            });

            return sets;
        }

        private void generateTestMethods(final @NotNull List<TestCaseDto> testCases, final @NotNull String targetName, final @NotNull ProgressIndicator indicator) {
            Logger.info("Import: generating test methods for '" + targetName + "' with " + testCases.size() + " cases");
            final long startedAt = System.currentTimeMillis();

            for (int from = 0; from < testCases.size(); from += METHODS_PER_COMMAND) {
                final @NotNull List<TestCaseDto> batch =
                        testCases.subList(from, Math.min(from + METHODS_PER_COMMAND, testCases.size()));
                final int written = from + batch.size();

                indicator.setText2(Bundle.message("import.progress.generating", String.valueOf(written), String.valueOf(testCases.size())));
                onEdt(() -> GenType.CREATE_TEST_CASE.executeAll(p, batch));
            }

            Logger.info("Import: generated " + testCases.size() + " test methods in "
                    + (System.currentTimeMillis() - startedAt) + "ms");
        }

        // UC-SHARE-005, Rule-SHARE-025, Rule-SHARE-037
        private @NotNull List<TestCaseDto> linkAndSaveTestCases(final @NotNull Path dirPath, final @NotNull List<TestCaseDto> testCases, final @NotNull String tailRank, final @NotNull ProgressIndicator indicator, final int done, final int total) {
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            String rank = tailRank;

            for (final TestCaseDto currentTestCase : testCases) {
                rank = Rank.after(rank);
                currentTestCase.setOrder(rank);
            }

            final @NotNull List<TestCaseDto> written = new ArrayList<>(testCases.size());
            int tried = 0;
            for (final TestCaseDto tc : testCases) {
                if (indicator.isCanceled()) break;

                if (indexer.putTestCaseVerbatim(dirPath, tc)) written.add(tc);

                tried++;
                indicator.setFraction((done + tried) / (double) total);
                indicator.setText2(tc.getDescription());
            }

            return written;
        }

        private @NotNull String rankOfTail(final @NotNull Path directory) {
            return findExistingTail(directory).map(TestCaseDto::getOrder).orElse("");
        }

        private @NotNull Optional<TestCaseDto> findExistingTail(final @NotNull Path directory) {
            final @NotNull List<TestCaseDto> existing =
                    TestCaseOrder.ordered(Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(directory));

            return existing.isEmpty() ? Optional.empty() : Optional.of(existing.getLast());
        }
    }
}
