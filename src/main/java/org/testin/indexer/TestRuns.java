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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.logger.Logger;
import org.testin.model.result.TestRunItems;
import org.testin.model.TestRunDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class TestRuns {
    private final @NotNull Project p;
    private final @NotNull ProjectIndexer indexer;

    public TestRuns(final @NotNull Project p) {
        this.p = p;
        this.indexer = Services.getInstance(p, ProjectIndexer.class);
    }

    private @NotNull IndexerDataStore store() {
        return indexer.getStore();
    }

    private @NotNull TestRunWriter testRunWriter() {
        return indexer.getTestRunWriter();
    }

    public @NotNull TestRunDto getTestRunByPath(final @NotNull Path testRunPath) {
        return withTestCasesShown(store().getTestRunByPath(testRunPath));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-REPORT-021, Rule-VIEW-PANEL-083
    private @NotNull TestRunDto withTestCasesShown(final @NotNull TestRunDto testRun) {
        final @NotNull IndexerDataStore store = store();
        testRun.getResults().forEach(item -> item.showing(store.findTestCase(item.getId())));
        return testRun;
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-106
    public @NotNull Map<Path, TestRunDto> getAllTestRuns() {
        return store().getTestRunsByPath().entrySet().stream()
                .collect(Collectors.toMap(entry -> Path.of(entry.getKey()), entry -> withTestCasesShown(entry.getValue())));
    }

    // UC-INTERNAL-006, Rule-INTERNAL-051
    public @NotNull Optional<TestRunDirectoryDto> findTestRunDir(final @NotNull Path testRunPath) {
        return store().findTestRunDir(testRunPath);
    }

    public @NotNull Optional<TestRunDto> findTestRun(final @NotNull Path testRunPath) {
        return store().findTestRun(testRunPath).map(this::withTestCasesShown);
    }

    public void changeTestRun(final @NotNull Path testRunPath, final @NotNull Consumer<TestRunDto> change) {
        findTestRun(testRunPath).ifPresentOrElse(testRun -> {
            // Rule-INTERNAL-011
            final @NotNull Set<UUID> gone = testRun.coveredIds();
            change.accept(testRun);
            gone.removeAll(testRun.coveredIds());

            testRunWriter().persist(testRunPath, testRun, gone);
        }, () -> Logger.warn("Test run no longer indexed, so a change to it was dropped: " + testRunPath.getFileName()));
    }

    // Rule-INTERNAL-011
    public void changeResult(final @NotNull Path testRunPath, final @NotNull UUID testCaseId, final @NotNull Consumer<TestRunItems> change) {
        findTestRun(testRunPath).ifPresentOrElse(testRun -> testRun.resultOf(testCaseId).ifPresentOrElse(result -> {
            change.accept(result);
            testRunWriter().persistResult(testRunPath, testRun, result);
        }, () -> Logger.warn("'" + testRunPath.getFileName() + "' no longer covers " + testCaseId + ", so a change to its result was dropped")),
                () -> Logger.warn("Test run no longer indexed, so a change to it was dropped: " + testRunPath.getFileName()));
    }

    public void changeTestRunMarker(final @NotNull Path testRunPath, final @NotNull Consumer<TestRunMarker> change) {
        store().findTestRunDir(testRunPath).ifPresentOrElse(dir -> {
            final @NotNull TestRunMarker marker = dir.getMarker();
            change.accept(marker);
            testRunWriter().persistMarker(testRunPath, () -> notSaved(testRunPath));
            indexer.announce(testRunPath);
        }, () -> Logger.warn("Test run no longer indexed, so a change to its marker was dropped: " + testRunPath.getFileName()));
    }

    // Rule-INTERNAL-123
    private void notSaved(final @NotNull Path testRunPath) {
        store().rereadTestRunMarker(testRunPath);
        ApplicationManager.getApplication().invokeLater(() -> {
            indexer.announce(testRunPath);
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("test.run.marker.not.saved", testRunPath.getFileName()));
        });
    }

    public void saveTestRun(final @NotNull Path testRunPath) {
        changeTestRun(testRunPath, _ -> {
        });
    }

    public void putTestRun(final @NotNull Path testRunPath, final @NotNull TestRunDto tr) {
        testRunWriter().create(testRunPath, tr);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public @NotNull List<String> storeScreenshots(final @NotNull Path testRunPath, final @NotNull List<byte[]> pngs) {
        return testRunWriter().storeScreenshots(testRunPath, pngs);
    }

    public byte @NotNull [] screenshot(final @NotNull Path testRunPath, final @NotNull String name) {
        return testRunWriter().readScreenshot(testRunPath, name);
    }

    @TestOnly
    public void awaitWrites() {
        testRunWriter().awaitWrites();
    }

    public @NotNull List<byte[]> screenshots(final @NotNull Path testRunPath, final @NotNull TestRunItems item) {
        return item.getScreenshots().stream().map(name -> screenshot(testRunPath, name)).toList();
    }
}
