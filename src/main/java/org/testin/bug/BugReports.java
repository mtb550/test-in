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

package org.testin.bug;

import com.intellij.openapi.components.Service;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestRunDto;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Which run items have a bug report on the way, and what a tester wrote that
 * has not been sent yet (#28).
 * <p>
 * Kept outside Swing, because the details panel rebuilds itself on every
 * refresh: a Report Bug link disabled on the component is drawn enabled again by
 * the next refresh, and a second click files a second issue. The panel asks here
 * whenever it draws instead.
 * <p>
 * Kept by run item - the run holding it and its id - rather than by test case,
 * because one test case is a run item in every run that covers it.
 * <p>
 * On the EDT: every change is made at a click or in a background task's EDT
 * hook.
 */
@Service(Service.Level.PROJECT)
public final class BugReports {

    /**
     * Where a run item's bug report is.
     */
    @Getter
    @AllArgsConstructor
    enum Stage {
        PREPARING(
                Bundle.message("bug.preparing")
        ),

        OPEN(
                Bundle.message("bug.open")
        ),

        SENDING(
                Bundle.message("bug.sending")
        );

        /**
         * Why Report Bug is off while the report is at this stage.
         */
        private final @NotNull String reason;
    }

    /**
     * A run item, by the run holding it.
     */
    public record RunItem(@NotNull Path run, @NotNull UUID id) {

        /**
         * This run item in that run, and empty when the run no longer holds it
         * or it was removed from the run.
         */
        public @NotNull Optional<TestRunItems> in(final @NotNull TestRunDto testRun) {
            return testRun.resultOf(id).filter(result -> !result.isRemoved());
        }

        /**
         * This run item in that run, and empty unless it is still failed: only a
         * failure is reported.
         */
        public @NotNull Optional<TestRunItems> failedIn(final @NotNull TestRunDto testRun) {
            return in(testRun).filter(result -> result.getStatus() == TestStatus.FAILED);
        }

        /**
         * The same, for the run as the indexer holds it now.
         */
        public @NotNull Optional<TestRunItems> stillFailed(final @NotNull ProjectIndexer indexer) {
            return indexer.findTestRun(run).flatMap(this::failedIn);
        }
    }

    /**
     * What the tester left in the dialog.
     */
    public record Edits(@NotNull String title, @NotNull String body) {
    }

    private final @NotNull Map<RunItem, Stage> onTheWay = new HashMap<>();
    private final @NotNull Map<RunItem, Edits> unsent = new HashMap<>();

    /**
     * Claims the run item for a new report, once {@link #whyReportBugIsOff} has
     * found nothing against it in the same click.
     */
    void begin(final @NotNull RunItem item) {
        onTheWay.put(item, Stage.PREPARING);
    }

    void moveTo(final @NotNull RunItem item, final @NotNull Stage stage) {
        onTheWay.replace(item, stage);
    }

    /**
     * Lets the run item go, but only from the stage the caller was holding it
     * at: a preparation that ends after its dialog opened must not close the
     * dialog's claim, and a dialog that closes on Send must not end the send.
     * True when it let go.
     */
    boolean end(final @NotNull RunItem item, final @NotNull Stage stage) {
        return onTheWay.remove(item, stage);
    }

    /**
     * Whether a report for some other run item is open in the dialog, which
     * shows one of a kind at a time.
     */
    boolean anotherIsOpen(final @NotNull RunItem item) {
        return onTheWay.entrySet().stream().anyMatch(entry -> entry.getValue() == Stage.OPEN && !entry.getKey().equals(item));
    }

    /**
     * Kept until the bug is sent or the tester throws the edits away, so a
     * refused or failed send reopens with them.
     */
    void keep(final @NotNull RunItem item, final @NotNull Edits edits) {
        unsent.put(item, edits);
    }

    @NotNull Optional<Edits> unsent(final @NotNull RunItem item) {
        return Optional.ofNullable(unsent.get(item));
    }

    void discard(final @NotNull RunItem item) {
        unsent.remove(item);
    }

    /**
     * UC-VIEW-PANEL-016, Rule-VIEW-PANEL-072.
     * <p>
     * Why Report Bug is off for this run item, and empty when it is on: its own
     * report on the way, the bug already reported, or another run item's report
     * open. A signed-off run is not a reason: the issue link is the one change
     * it takes, because it moves no verdict (P23).
     */
    public @NotNull Optional<String> whyReportBugIsOff(final @NotNull RunItem item, final @NotNull TestRunItems result) {
        final @NotNull Optional<Stage> stage = Optional.ofNullable(onTheWay.get(item));
        if (stage.isPresent()) return stage.map(Stage::getReason);

        if (result.bugIssue().isPresent()) return Optional.of(Bundle.message("bug.already.reported"));
        if (anotherIsOpen(item)) return Optional.of(Bundle.message("bug.finish.open.report"));

        return Optional.empty();
    }
}
