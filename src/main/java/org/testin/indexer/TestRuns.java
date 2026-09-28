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

package org.testin.indexer;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestRunDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class TestRuns {
    private final @NotNull Project p;

    private @NotNull ProjectIndexer indexer() {
        return Services.getInstance(p, ProjectIndexer.class);
    }

    private @NotNull IndexerDataStore store() {
        return indexer().getStore();
    }

    private @NotNull RunWriter runWriter() {
        return indexer().getRunWriter();
    }

    public @NotNull TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return withTestCasesShown(store().getTestRunByPath(testRunPath));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-REPORT-021, Rule-VIEW-PANEL-083
    private @NotNull TestRunDto withTestCasesShown(final @NotNull TestRunDto run) {
        final @NotNull IndexerDataStore store = store();
        run.getResults().forEach(item -> item.showing(store.findTestCase(item.getId())));
        return run;
    }

    // UC-VIEW-PANEL-008, Rule-VIEW-PANEL-065
    public @NotNull Map<Path, TestRunDto> getAllTestRuns() {
        return store().getTestRunsByPath().entrySet().stream()
                .collect(Collectors.toMap(entry -> Path.of(entry.getKey()), entry -> withTestCasesShown(entry.getValue())));
    }

    // UC-INTERNAL-006, Rule-INTERNAL-051
    public @NotNull Optional<TestRunDto> findTestRun(final @NotNull Path testRunPath) {
        return store().findTestRun(testRunPath).map(this::withTestCasesShown);
    }

    public void changeRun(final @NotNull Path runPath, final @NotNull Consumer<TestRunDto> change) {
        findTestRun(runPath).ifPresentOrElse(run -> {
            // Rule-INTERNAL-011
            final @NotNull Set<UUID> gone = run.coveredIds();
            change.accept(run);
            gone.removeAll(run.coveredIds());

            runWriter().persist(runPath, run, gone);
        }, () -> Logger.warn("Test run no longer indexed, so a change to it was dropped: " + runPath.getFileName()));
    }

    // Rule-INTERNAL-011
    public void changeResult(final @NotNull Path runPath, final @NotNull UUID testCaseId, final @NotNull Consumer<TestRunItems> change) {
        findTestRun(runPath).ifPresentOrElse(run -> run.resultOf(testCaseId).ifPresentOrElse(result -> {
            change.accept(result);
            runWriter().persistResult(runPath, run, result);
        }, () -> Logger.warn("'" + runPath.getFileName() + "' no longer covers " + testCaseId + ", so a change to its result was dropped")),
                () -> Logger.warn("Test run no longer indexed, so a change to it was dropped: " + runPath.getFileName()));
    }

    public void changeRunMarker(final @NotNull Path runPath, final @NotNull Consumer<TestRunMarker> change) {
        store().findTestRunDir(runPath).ifPresentOrElse(dir -> {
            final @NotNull TestRunMarker marker = dir.getMarker();
            change.accept(marker);
            runWriter().persistMarker(runPath);
            indexer().announce(runPath);
        }, () -> Logger.warn("Test run no longer indexed, so a change to its marker was dropped: " + runPath.getFileName()));
    }

    public void saveRun(final @NotNull Path runPath) {
        changeRun(runPath, _ -> {
        });
    }

    public void putTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        runWriter().create(testRunPath, tr);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public @NotNull List<String> storeScreenshots(final @NotNull Path runPath, final @NotNull List<byte[]> pngs) {
        return runWriter().storeScreenshots(runPath, pngs);
    }

    public byte @NotNull [] screenshot(final @NotNull Path runPath, final @NotNull String name) {
        return runWriter().readScreenshot(runPath, name);
    }

    public @NotNull List<byte[]> screenshots(final @NotNull Path runPath, final @NotNull TestRunItems item) {
        return item.getScreenshots().stream().map(name -> screenshot(runPath, name)).toList();
    }
}
