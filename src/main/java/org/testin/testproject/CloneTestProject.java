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

import lombok.AllArgsConstructor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import org.jetbrains.annotations.NotNull;
import org.testin.util.FailureText;
import org.testin.explorer.TreePanel;
import org.testin.git.GitSafeText;
import org.testin.indexer.ProjectIndexer;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.nio.file.Path;
import org.testin.config.TestinYml;
import org.testin.model.DirectoryType;
import org.testin.util.NameSanitizer;
import java.util.Locale;
import java.util.Optional;

/**
 * UC-TREE-PANEL-003.
 * <p>
 * Clones a test project's repository and chooses it for this repository.
 * <p>
 * Not an action, for the reason {@link NewTestProject} is not: nothing
 * registered it, so its AnAction half could never run (#312, A100).
 */
@AllArgsConstructor
public final class CloneTestProject {
    private final @NotNull Project p;
    private final @NotNull String gitUrl;
    private final @NotNull String projectName;
    private final @NotNull TreePanel tp;

    /**
     * UC-TREE-PANEL-003, Rule-TREE-PANEL-107.
     * <p>
     * What a clone of this address is called. Named after its repository, so no
     * {@code testin.yml} is needed to clone one - unless the address is the one
     * the file gives, which then names it with the name it gives beside it.
     * A name already in the Testin folder takes the next number; the file's
     * name never does, because the file means that folder.
     */
    public static @NotNull String nameFor(final @NotNull Project p, final @NotNull String url) {
        final @NotNull String named = TestinYml.projectName(p);
        if (!named.isEmpty() && TestinYml.isRepoUrl(p, url)) return named;

        final @NotNull String base = repositoryName(url);
        final @NotNull Path root = Services.getInstance(p, TestinRoot.class).getPath();
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        @NotNull String name = base;
        for (int n = 2; indexer.isTaken(root.resolve(name), Optional.empty()); n++) {
            name = base + n;
        }
        return name;
    }

    /**
     * Rule-TREE-PANEL-107.
     * <p>
     * The repository's own name, from any form of address a clone takes:
     * {@code https://github.com/acme/nafath-test-cases.git},
     * {@code git@github.com:acme/nafath-test-cases.git} and
     * {@code https://host/acme/nafath-test-cases/} all give
     * {@code nafath-test-cases}. Kept as it is when it can name a test project,
     * and made into one when it cannot - the package it would become has to be
     * one Java accepts (Rule-TREE-PANEL-095).
     */
    static @NotNull String repositoryName(final @NotNull String url) {
        @NotNull String path = url.strip();

        final int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);

        final int fragment = path.indexOf('#');
        if (fragment >= 0) path = path.substring(0, fragment);

        while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        if (path.toLowerCase(Locale.ROOT).endsWith(".git")) path = path.substring(0, path.length() - ".git".length());

        final @NotNull String last = path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf(':')) + 1);
        return DirectoryType.TP.canTakeName(last) ? last : NameSanitizer.packageName(last);
    }

    // UC-TREE-PANEL-003, Rule-TREE-PANEL-107
    public void execute() {

        if (gitUrl.trim().isEmpty() || projectName.trim().isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("clone.error.title"), Bundle.message("clone.error.missing"));
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(p, Bundle.message("clone.task"), false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(Bundle.message("clone.progress", projectName));

                try {
                    final @NotNull Path parentPath = Services.getInstance(p, TestinRoot.class).getPath();

                    // UC-TREE-PANEL-003.
                    //
                    // git4idea's own clone, and the one Git call in the plugin
                    // that does not go through GitCommandRunner. That is not an
                    // oversight: the runner builds its handler on a repository,
                    // and the IDE knows only the repositories registered as VCS
                    // roots in the open project - a Testin root is deliberately
                    // not one. A clone has no repository to look up, it makes
                    // one, so git4idea builds its handler on the parent
                    // directory instead and the objection does not apply.
                    //
                    // It also does four things the runner would have to be
                    // taught: it sets the URL, which is what turns on the
                    // credential helper for a private HTTPS remote; it adds
                    // core.longpaths=true on Windows, without which a deep test
                    // project fails to check out; --progress; and the IDE's own
                    // recurse-submodules setting (#66, finding 4).
                    final @NotNull GitCommandResult result = Git.getInstance().clone(p, parentPath, gitUrl, projectName);
                    result.throwOnError();

                    // The indexer owns disk reads/refresh: scanSingleProject re-scans the cloned
                    // project from disk. No direct VFS refresh here.
                    //
                    // Here on the task's thread, under its bar, not in the invokeLater
                    // below: reading every set, case and run of the clone there froze
                    // the IDE until it finished (#312, A92).
                    final @NotNull Path projectPath = Services.getInstance(p, TestinRoot.class).getPath().resolve(projectName);
                    Services.getInstance(p, ProjectIndexer.class).scanSingleProject(projectPath, indicator);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        // Chosen, for the same reason a new project is: this
                        // repository asked for it (#8). On this machine only; a
                        // clone never writes testin.yml (Rule-TREE-PANEL-106).
                        Services.getInstance(p, BoundTestProject.class).choose(projectName);

                        tp.refresh();
                        Services.getInstance(p, Notifier.class).softShow(p, Done.CLONED);
                    });

                } catch (final Exception ex) {
                    // UC-TREE-PANEL-003, Rule-SHARE-062.
                    //
                    // Git names the remote it failed against, and this is the
                    // last place a remote still reaches Testin with a token in
                    // it: testin.yml has one stripped on the way in and on the
                    // way out, but the URL for a clone is typed into New Test
                    // Project and goes straight to Git as it was pasted. So a
                    // wrong token, a wrong name or no network put the token
                    // itself in the balloon and in the IDE's notification list.
                    final @NotNull String said = GitSafeText.withoutCredentials(FailureText.of(ex));

                    Services.getInstance(p, Notifier.class).error(p, Bundle.message("clone.failed.title"), Bundle.message("clone.failed.message", said));
                }
            }
        });
    }
}
