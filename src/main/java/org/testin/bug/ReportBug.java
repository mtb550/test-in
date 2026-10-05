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
import org.testin.help.Hint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.indexer.TestCaseFile;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.result.TestRunItems;
import org.testin.model.TestCaseDto;
import org.testin.model.TestRunDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportBug {
    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-067
    public static void start(final @NotNull Project p, final @NotNull TestRunDirectoryDto testRunDirectory, final @NotNull UUID runItemId, final @NotNull TestCaseDto tc, final @NotNull Runnable redraw) {
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        final @NotNull TestCases testCases = Services.getInstance(p, TestCases.class);
        final @NotNull BugReports reports = Services.getInstance(p, BugReports.class);
        final @NotNull RunItem item = new RunItem(testRunDirectory.getPath(), runItemId);

        final @NotNull Optional<TestRunDto> testRun = testRuns.findTestRun(item.testRunPath());
        final @NotNull Optional<TestRunItems> failed = testRun.flatMap(item::failedIn);
        if (failed.isEmpty() || reports.whyReportBugIsOff(item, failed.orElseThrow()).isPresent()) return;

        reports.begin(item);
        redraw.run();

        final @NotNull TestRunItems failedItem = failed.orElseThrow();
        final @NotNull Optional<TestCaseFile> file = testCases.testCaseFile(tc);

        BackgroundWork.run(p, Bundle.message("bug.preparing"), Bundle.message("bug.send.failed.title"), true,
                indicator -> prepare(p, BugFacts.of(failedItem, tc, testRunDirectory.getMarker(), testRunDirectory.getName(), testRuns.screenshots(item.testRunPath(), failedItem)), file, indicator),
                bug -> open(p, item, bug, redraw),
                () -> {
                    reports.end(item, Stage.PREPARING);
                    redraw.run();
                });
    }

    private static @NotNull PreparedBug prepare(final @NotNull Project p, final @NotNull BugFacts facts, final @NotNull Optional<TestCaseFile> file, final @NotNull ProgressIndicator indicator) {
        final @NotNull Optional<String> link = file.flatMap(where -> TestCaseLink.read(p, where));
        indicator.checkCanceled();

        final @NotNull String bugRepoUrl = TestinYml.bugRepoUrlOnDisk(p);
        final @NotNull Optional<String> whyNotReady = GitHubCli.onPath(indicator).whyItCannotSend(bugRepoUrl);
        indicator.checkCanceled();
        final @NotNull Hints hints = Services.getInstance(p, Hints.class);
        whyNotReady.ifPresentOrElse(reason -> hints.fire(BugRepository.of(bugRepoUrl).isEmpty()
                ? Hint.of(SetupStep.BUG_FILING, reason, () -> BugRepoUrlForm.of(p))
                : Hint.of(SetupStep.BUG_FILING, reason)), () -> hints.clear(SetupStep.BUG_FILING));

        return new PreparedBug(facts, BugTemplate.body(facts, link), BugRepository.of(bugRepoUrl), whyNotReady);
    }

    private static void open(final @NotNull Project p, final @NotNull RunItem item, final @NotNull PreparedBug bug, final @NotNull Runnable redraw) {
        final @NotNull BugReports reports = Services.getInstance(p, BugReports.class);
        if (reports.anotherIsOpen(item)) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("bug.finish.open.report"));
            return;
        }

        reports.moveTo(item, Stage.OPEN);
        new ReportBugDialog(p, item, bug, redraw).open();
    }
}
