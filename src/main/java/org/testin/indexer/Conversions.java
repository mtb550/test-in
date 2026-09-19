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
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * UC-INTERNAL-008, Rule-INTERNAL-091.
 * <p>
 * The one thing that converts a test project, and the one thing that says what a
 * conversion did (#305, S8).
 * <p>
 * <b>Application level, and keyed by the project's folder.</b> Scans share a read
 * lock, there is one scan coordinator per open window, and the file watcher
 * re-reads in every window - so two windows opening the same Testin folder would
 * otherwise convert one project twice at once: the same case moved by both, the
 * format number written by one over the other's half-finished work. One lock per
 * project folder, held across the whole conversion, is what makes that
 * impossible whatever opens what.
 * <p>
 * Deleted with {@link FormatConverter} in 2.14.0-alpha, except for the check that
 * refuses a project this build cannot read, which lives on the marker.
 */
@Service(Service.Level.APP)
public final class Conversions {

    /**
     * One lock per project folder. Kept rather than dropped: a project converted
     * once is asked about on every scan, and a map that forgets would hand two
     * scans two locks for one folder.
     */
    private final @NotNull Map<String, Object> locks = new ConcurrentHashMap<>();

    /**
     * UC-INTERNAL-008, Rule-INTERNAL-091.
     * <p>
     * Brings this project to the format this build reads, once, whoever asks
     * first - the scan before it reads a project, or the sweep at start. A project
     * already in it returns without touching a file.
     */
    void ensure(final @NotNull Project p, final @NotNull Path project) {
        report(p, convert(p, project).map(List::of).orElse(List.of()));
    }

    /**
     * UC-INTERNAL-008, Rule-INTERNAL-091.
     * <p>
     * D9: <b>every</b> test project in the Testin folder, not only the one a
     * repository is about - a project nobody opened under 2.13.0-alpha would be
     * refused by 2.14.0-alpha, and the tester would have nothing left that could
     * convert it. Run at every start and every time the Testin folder changes,
     * skipping what is already converted, so there is no flag to keep (S10).
     */
    void sweep(final @NotNull Project p) {
        final @NotNull Path root = Services.getInstance(p, TestinRoot.class).getPath();
        if (root.toString().isEmpty() || !Files.isDirectory(root)) return;

        final @NotNull List<FormatConverter.Report> reports = new ArrayList<>();
        for (final Path project : projectsIn(root)) {
            convert(p, project).ifPresent(reports::add);
        }

        report(p, reports);
    }

    private @NotNull Optional<FormatConverter.Report> convert(final @NotNull Project p, final @NotNull Path project) {
        synchronized (locks.computeIfAbsent(project.toString(), path -> new Object())) {
            return new FormatConverter(p).convert(project);
        }
    }

    /**
     * The folders under the Testin root that are test projects: the ones carrying
     * a project marker. Sorted, so the notification lists them the same way every
     * time.
     */
    private static @NotNull List<Path> projectsIn(final @NotNull Path root) {
        try (Stream<Path> children = Files.list(root)) {
            return children.filter(Files::isDirectory)
                    .filter(folder -> Files.exists(folder.resolve(DirectoryType.TP.getMarker())))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (final IOException ex) {
            Logger.warn("Could not list the Testin folder " + root + ": " + ex.getMessage());
            return List.of();
        }
    }

    /**
     * UC-INTERNAL-008, Rule-INTERNAL-092.
     * <p>
     * One notification for the whole conversion, in the log where it stays: which
     * projects were converted, how many test cases each kept and how many runs it
     * lost, and any file left for the tester to repair. A conversion is something
     * that happened to a tester's data while they were opening a project, so it
     * says so once and does not fade (D9).
     */
    private static void report(final @NotNull Project p, final @NotNull List<FormatConverter.Report> reports) {
        final @NotNull List<FormatConverter.Report> said = reports.stream()
                .filter(report -> report.changedAnything() || report.failed() || !report.toRepair().isEmpty())
                .toList();
        if (said.isEmpty()) return;

        final @NotNull StringBuilder message = new StringBuilder();
        for (final FormatConverter.Report report : said) {
            message.append(Bundle.message("convert.project", report.project(), String.valueOf(report.cases()), String.valueOf(report.runs())));

            if (!report.toRepair().isEmpty()) {
                message.append(' ').append(Bundle.message("convert.repair", String.join(", ", report.toRepair())));
            }
            if (report.failed()) message.append(' ').append(Bundle.message("convert.unfinished"));

            message.append('\n');
        }

        Services.getInstance(p, Notifier.class).info(p, Bundle.message("convert.title"), message.toString().strip());
    }
}
