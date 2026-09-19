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

    /**
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-115.
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
     * Works out the lines and what they change off the EDT - the clone address
     * is a Git command, and the file is read - then shows them, and writes them
     * only when the tester presses Save.
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
            final @NotNull String preview = preview(p, open.getName(), lines);

            ApplicationManager.getApplication().invokeLater(() ->
                    new ConfirmDialog(p, Bundle.message("yml.save.name"), preview, "", "", Bundle.message("yml.save.confirm"), () -> save(p, lines)).show(), p.getDisposed());
        });
    }

    /**
     * Rule-TREE-PANEL-113.
     * <p>
     * The project, and where it is cloned from when Git can say. Without the
     * Git plugin nothing can be asked, so only the project's line is written.
     */
    private static @NotNull Map<String, String> linesFor(final @NotNull Project p, final @NotNull TestProjectDirectoryDto open) {
        if (!OptionalPlugin.GIT.isAvailable()) return TestinYml.lines(open.getName());

        final @NotNull Path folder = open.getPath();
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        return TestinYml.lines(open.getName(), git.isNotRepository(folder) ? "" : git.remoteUrl(folder));
    }

    /**
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-113, Rule-TREE-PANEL-114.
     * <p>
     * The file, each line as it will be and what that changes, and the reminder
     * that the file is the team's.
     */
    private static @NotNull String preview(final @NotNull Project p, final @NotNull String projectName, final @NotNull Map<String, String> lines) {
        final @NotNull Map<String, String> written = TestinYml.writtenValues(p, lines.keySet());
        final @NotNull StringBuilder message = new StringBuilder();

        TestinYml.savePath(p).ifPresent(path -> message.append(path).append("\n\n"));
        lines.forEach((key, value) -> message.append(key).append(": ").append(value).append("   ").append(change(Optional.ofNullable(written.get(key)), value)).append('\n'));
        message.append('\n').append(Bundle.message("yml.save.rest"))
                .append('\n').append(Bundle.message("yml.save.shared", projectName));

        return message.toString();
    }

    /**
     * What saving does to one line: adds it, leaves it as it is, or replaces the
     * value it had.
     */
    private static @NotNull String change(final @NotNull Optional<String> before, final @NotNull String value) {
        return before.map(was -> was.equals(value) ? Bundle.message("yml.save.same") : Bundle.message("yml.save.was", was))
                .orElse(Bundle.message("yml.save.new"));
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

        Services.getInstance(p, BoundTestProject.class).refreshGutter();
        Services.getInstance(p, TestinEditors.class).refreshOpen(p);
        notifier.softShow(p, Done.SAVED);
    }
}
