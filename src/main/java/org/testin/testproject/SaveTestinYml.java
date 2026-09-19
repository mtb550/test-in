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

package org.testin.testproject;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.editor.TestinEditors;
import org.testin.git.GitRepositoryService;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * UC-TREE-PANEL-029.
 * <p>
 * Names the open test project in {@code testin.yml}, after showing what it
 * writes - the one thing in Testin that writes the file (#335). Pressed from the
 * panel's title bar, or from the notification that says code was left alone.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SaveTestinYml {

    private static final @NotNull String PROJECT = "testinProject";
    private static final @NotNull String LOCATION = "location";
    private static final @NotNull String REPO_URL = "RepoUrl";

    /**
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-104.
     * <p>
     * Why the button cannot save now, and nothing when it can. A file that
     * cannot be read is not edited: lines written into a broken file could
     * leave it broken and make code look on.
     */
    public static @NotNull Optional<String> whyNot(final @NotNull Project p) {
        if (TestinYml.isUnreadable(p)) return Optional.of(Bundle.message("yml.save.disabled.unreadable"));
        if (Services.getInstance(p, BoundTestProject.class).get().isEmpty()) return Optional.of(Bundle.message("yml.save.disabled.no.project"));

        return Optional.empty();
    }

    /**
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-113.
     * <p>
     * Works out the lines off the EDT - the clone address is a Git command -
     * then shows them, and writes them only when the tester presses Save.
     */
    public static void start(final @NotNull Project p) {
        final @NotNull Optional<String> why = whyNot(p);
        if (why.isPresent()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, why.orElseThrow());
            return;
        }

        final @NotNull TestProjectDirectoryDto open = Services.getInstance(p, BoundTestProject.class).get().orElseThrow();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Map<String, String> lines = linesFor(p, open);
            ApplicationManager.getApplication().invokeLater(() -> preview(p, lines), p.getDisposed());
        });
    }

    /**
     * Rule-TREE-PANEL-113.
     * <p>
     * The project, and where it is cloned from when Git can say: a remote gives
     * {@code location: remote} and its address without an account or token, a
     * folder with none gives {@code location: local}. Without the Git plugin
     * nothing can be asked, so those two lines are left as they are rather than
     * calling a Git project local.
     */
    private static @NotNull Map<String, String> linesFor(final @NotNull Project p, final @NotNull TestProjectDirectoryDto open) {
        final @NotNull Map<String, String> lines = new LinkedHashMap<>();
        lines.put(PROJECT, open.getName());

        if (!OptionalPlugin.GIT.isAvailable()) return lines;

        final @NotNull Path folder = open.getPath();
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        final @NotNull String remote = git.isNotRepository(folder) ? "" : git.remoteUrl(folder);

        if (remote.isEmpty()) {
            lines.put(LOCATION, "local");
        } else {
            lines.put(LOCATION, "remote");
            lines.put(REPO_URL, TestinYml.withoutCredentials(remote));
        }
        return lines;
    }

    /**
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-113, Rule-TREE-PANEL-114.
     * <p>
     * The file, each line as it will be and what that changes, and the reminder
     * that the file is the team's.
     */
    private static void preview(final @NotNull Project p, final @NotNull Map<String, String> lines) {
        final @NotNull Map<String, String> written = TestinYml.writtenValues(p, lines.keySet());
        final @NotNull StringBuilder message = new StringBuilder();

        TestinYml.savePath(p).ifPresent(path -> message.append(path).append("\n\n"));
        lines.forEach((key, value) -> message.append(key).append(": ").append(value).append("   ").append(change(written, key, value)).append('\n'));
        message.append('\n').append(Bundle.message("yml.save.rest"))
                .append('\n').append(Bundle.message("yml.save.shared", lines.get(PROJECT)));

        new ConfirmDialog(p, Bundle.message("yml.save.name"), message.toString(), "", "", Bundle.message("yml.save.confirm"), () -> save(p, lines)).show();
    }

    private static @NotNull String change(final @NotNull Map<String, String> written, final @NotNull String key, final @NotNull String value) {
        final @NotNull Optional<String> before = Optional.ofNullable(written.get(key)).map(SaveTestinYml::unquoted);
        if (before.isEmpty()) return Bundle.message("yml.save.new");

        return before.orElseThrow().equals(value) ? Bundle.message("yml.save.same") : Bundle.message("yml.save.was", before.orElseThrow());
    }

    private static @NotNull String unquoted(final @NotNull String written) {
        final boolean quoted = written.length() >= 2
                && (written.startsWith("'") && written.endsWith("'") || written.startsWith("\"") && written.endsWith("\""));
        return quoted ? written.substring(1, written.length() - 1) : written;
    }

    /**
     * UC-TREE-PANEL-029, Rule-CODEGEN-082.
     * <p>
     * Writes, and lets what depends on the file answer again at once: the
     * gutter is recomputed and the open editors reread which cases have code.
     */
    private static void save(final @NotNull Project p, final @NotNull Map<String, String> lines) {
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
        if (!TestinYml.save(p, lines)) {
            notifier.softRefuse(p, Bundle.message("yml.save.failed"));
            return;
        }

        // With a reason: the bare restart() is deprecated, and Build fails on a
        // deprecated call (#324). The reason only reaches the IDE's diagnostics.
        DaemonCodeAnalyzer.getInstance(p).restart("Save to testin.yml turned the automation code on");
        Services.getInstance(p, TestinEditors.class).refreshOpen(p);
        notifier.softShow(p, Done.SAVED);
    }
}
