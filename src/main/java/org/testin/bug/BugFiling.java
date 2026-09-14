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

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.BugRepository;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.BugIssueUrl;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestRunDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sends the bug report a tester wrote, and records the issue it became (#28).
 * <p>
 * The send runs in the background and cannot be canceled. The answer is
 * recorded on the EDT, and only after the run and the run item are found again
 * through the indexer: a run renamed or removed while {@code gh} was talking is
 * not written back to where it used to be.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugFiling {

    /**
     * UC-VIEW-PANEL-016, Rule-VIEW-PANEL-073, Rule-VIEW-PANEL-077.
     * <p>
     * Sends it. What to redraw once the answer is recorded is the caller's to
     * say, because the surfaces showing the run item are the caller's to know.
     */
    public static void send(final @NotNull Project p, final @NotNull BugReports.RunItem item, final @NotNull BugRepository repository, final @NotNull BugReports.Edits edits, final @NotNull List<byte[]> screenshots, final @NotNull Runnable redraw) {
        final @NotNull BugReports reports = Services.getInstance(p, BugReports.class);
        reports.keep(item, edits);
        reports.moveTo(item, BugReports.Stage.SENDING);
        redraw.run();

        final @NotNull AtomicReference<IssueCreation> answer = new AtomicReference<>(IssueCreation.failed(""));
        BackgroundWork.run(p, Bundle.message("bug.sending"), Bundle.message("bug.send.failed.title"), false,
                indicator -> answer.set(GitHubCli.onPath(indicator).create(repository, edits.title(), edits.body(), screenshots)),
                () -> record(p, item, answer.get()),
                () -> {
                    reports.end(item, BugReports.Stage.SENDING);
                    redraw.run();
                });
    }

    /**
     * On the EDT, with what {@code gh} answered. An issue that exists is always
     * announced with its Open, whether or not it could be stored.
     */
    static void record(final @NotNull Project p, final @NotNull BugReports.RunItem item, final @NotNull IssueCreation answer) {
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

        answer.url().ifPresentOrElse(url -> {
            Services.getInstance(p, BugReports.class).discard(item);

            final @NotNull List<String> said = new ArrayList<>(List.of(BugIssueUrl.reference(url)));
            store(p, item, url).ifPresent(said::add);
            if (answer.notUploaded() > 0) said.add(Bundle.message("bug.not.uploaded", answer.notUploaded()));
            else if (!answer.problem().isEmpty()) said.add(answer.problem());

            notifier.infoWithActions(p, Done.REPORTED.getOutcome(), String.join("<br>", said),
                    notifier.lastingAction(Bundle.message("bug.open.issue"), () -> BugIssueUrl.open(url)));
        }, () -> notifier.error(p, Bundle.message("bug.send.failed.title"), answer.problem()));
    }

    /**
     * UC-VIEW-PANEL-016, Rule-VIEW-PANEL-074.
     * <p>
     * Writes the issue's address on the run item, and says why not when it
     * cannot: the run was renamed or removed, or the run item is gone or no
     * longer failed.
     */
    static @NotNull Optional<String> store(final @NotNull Project p, final @NotNull BugReports.RunItem item, final @NotNull String url) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull Optional<TestRunDto> run = indexer.findTestRun(item.run());
        final @NotNull Optional<TestRunItems> found = run.flatMap(item::in);
        if (found.isEmpty()) return Optional.of(Bundle.message("bug.not.stored.moved"));

        final @NotNull TestRunItems result = found.orElseThrow();
        if (result.getStatus() != TestStatus.FAILED) return Optional.of(Bundle.message("bug.not.stored.no.longer.failed"));

        result.setBugIssueUrl(url);
        indexer.persistRun(item.run(), run.orElseThrow());
        return Optional.empty();
    }
}
