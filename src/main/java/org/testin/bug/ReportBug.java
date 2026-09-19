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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.TestCaseFile;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.Optional;
import java.util.UUID;

/**
 * Report Bug: prepares a failed run item's bug report and opens it (#28).
 * <p>
 * Everything slow happens before the dialog opens, under the IDE's progress
 * bar, so the text a tester edits never changes under their hands and Send is
 * already enabled or disabled with its reason.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportBug {

    /**
     * UC-VIEW-PANEL-016, Rule-VIEW-PANEL-067.
     * <p>
     * On the EDT, at the click. Nothing starts when Report Bug is off for the
     * run item or it is no longer failed. What to redraw as the report moves on
     * is the caller's to say.
     */
    public static void start(final @NotNull Project p, final @NotNull TestRunDirectoryDto runDirectory, final @NotNull UUID runItemId, final @NotNull TestCaseDto tc, final @NotNull Runnable redraw) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull BugReports reports = Services.getInstance(p, BugReports.class);
        final @NotNull BugReports.RunItem item = new BugReports.RunItem(runDirectory.getPath(), runItemId);

        final @NotNull Optional<TestRunDto> run = indexer.findTestRun(item.run());
        final @NotNull Optional<TestRunItems> failed = run.flatMap(item::failedIn);
        if (failed.isEmpty() || reports.whyReportBugIsOff(item, failed.orElseThrow()).isPresent()) return;

        reports.begin(item);
        redraw.run();

        final @NotNull BugFacts facts = BugFacts.of(failed.orElseThrow(), tc, run.orElseThrow(), runDirectory.getName(), indexer.screenshots(item.run(), failed.orElseThrow()));
        final @NotNull Optional<TestCaseFile> file = indexer.testCaseFile(tc);

        BackgroundWork.run(p, Bundle.message("bug.preparing"), Bundle.message("bug.send.failed.title"), true,
                indicator -> prepare(p, facts, file, indicator),
                bug -> open(p, item, bug, redraw),
                () -> {
                    reports.end(item, BugReports.Stage.PREPARING);
                    redraw.run();
                });
    }

    /**
     * Off the EDT, under the progress bar. The configuration is read again, as
     * Refresh does, so a {@code bugRepoUrl} added by hand counts without one.
     */
    private static @NotNull PreparedBug prepare(final @NotNull Project p, final @NotNull BugFacts facts, final @NotNull Optional<TestCaseFile> file, final @NotNull ProgressIndicator indicator) {
        TestinYml.reload(p);

        final @NotNull Optional<String> link = file.flatMap(where -> TestCaseLink.read(p, where));
        indicator.checkCanceled();

        final @NotNull Optional<String> whyNotReady = GitHubCli.onPath(indicator).whyItCannotSend(TestinYml.bugRepoUrl(p));
        indicator.checkCanceled();

        return new PreparedBug(facts, BugTemplate.body(facts, link), TestinYml.bugRepository(p), whyNotReady);
    }

    /**
     * On the EDT, once prepared. The dialog shows one of a kind at a time, so a
     * report prepared while another run item's is open waits for that one.
     */
    private static void open(final @NotNull Project p, final @NotNull BugReports.RunItem item, final @NotNull PreparedBug bug, final @NotNull Runnable redraw) {
        final @NotNull BugReports reports = Services.getInstance(p, BugReports.class);
        if (reports.anotherIsOpen(item)) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("bug.finish.open.report"));
            return;
        }

        reports.moveTo(item, BugReports.Stage.OPEN);
        new ReportBugDialog(p, item, bug, redraw).open();
    }
}
