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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

// UC-INTERNAL-008, Rule-INTERNAL-091
@Service(Service.Level.APP)
public final class Conversions {
    private final @NotNull Map<String, Object> locks = new ConcurrentHashMap<>();

    private final @NotNull Set<FormatConverter.Report> said = ConcurrentHashMap.newKeySet();

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

    // UC-INTERNAL-008, Rule-INTERNAL-091
    void ensure(final @NotNull Project p, final @NotNull Path project) {
        report(p, convert(p, project).map(List::of).orElse(List.of()));
    }

    // UC-INTERNAL-008, Rule-INTERNAL-091
    void sweep(final @NotNull Project p) {
        final @NotNull Path root = Services.getInstance(p, TestinRoot.class).absolutePath();
        if (root.toString().isEmpty() || !Files.isDirectory(root)) return;

        final @NotNull List<FormatConverter.Report> reports = new ArrayList<>();
        for (final Path project : projectsIn(root)) {
            convert(p, project).ifPresent(reports::add);
        }

        report(p, reports);
    }

    private @NotNull Optional<FormatConverter.Report> convert(final @NotNull Project p, final @NotNull Path project) {
        synchronized (locks.computeIfAbsent(project.toString(), _ -> new Object())) {
            return new FormatConverter(p).convert(project);
        }
    }

    // UC-INTERNAL-008, Rule-INTERNAL-092
    private void report(final @NotNull Project p, final @NotNull List<FormatConverter.Report> reports) {
        final @NotNull List<FormatConverter.Report> worth = reports.stream()
                .filter(report -> report.changedAnything() || report.failed() || !report.toRepair().isEmpty())
                .filter(said::add)
                .toList();
        if (worth.isEmpty()) return;

        final @NotNull StringBuilder message = new StringBuilder();
        for (final FormatConverter.Report report : worth) {
            message.append(Bundle.message("convert.project", report.project(), String.valueOf(report.testCases()), String.valueOf(report.runs())));

            if (!report.toRepair().isEmpty()) {
                message.append(' ').append(Bundle.message("convert.repair", String.join(", ", report.toRepair())));
                report.toRepair().forEach(file ->
                        Logger.warn("Converting " + report.project() + " left " + file + " for the tester to repair"));
            }
            if (report.failed()) {
                message.append(' ').append(Bundle.message("convert.unfinished"));
                Logger.warn("Converting " + report.project() + " did not finish");
            }

            message.append('\n');
        }

        Services.getInstance(p, Notifier.class).info(p, Bundle.message("convert.title"), message.toString().strip());
    }
}
