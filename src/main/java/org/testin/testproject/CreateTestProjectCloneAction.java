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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.TreePanel;
import org.testin.git.GitSafeText;
import org.testin.indexer.ProjectIndexer;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Objects;

public class CreateTestProjectCloneAction extends AbstractProjectAction {
    private final @NotNull String gitUrl;
    private final @NotNull String projectName;
    private final @NotNull TreePanel tp;

    public CreateTestProjectCloneAction(final @NotNull Project p, final @NotNull String gitUrl, final @NotNull String name, final @NotNull TreePanel tp) {
        super(p, Bundle.message("clone.action.text"), Bundle.message("clone.action.description"), AllIcons.Vcs.Clone);
        this.gitUrl = gitUrl;
        this.projectName = name;
        this.tp = tp;
    }

    // UC-TREE-PANEL-003
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    /**
     * UC-TREE-PANEL-003, Rule-TREE-PANEL-019.
     * <p>
     * Direct entry point for dialog callbacks — no AnActionEvent required.
     */
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

                    ApplicationManager.getApplication().invokeLater(() -> {
                        // The indexer owns disk reads/refresh: scanSingleProject re-scans the cloned
                        // project from disk. No direct VFS refresh here.
                        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                        final @NotNull Path projectPath = Services.getInstance(p, TestinRoot.class).getPath().resolve(projectName);

                        indexer.scanSingleProject(projectPath);

                        // Bound to what was just cloned, for the same reason a new
                        // project is: this repository asked for it (#8).
                        Services.getInstance(p, BoundTestProject.class).bind(projectName);

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
                    final @NotNull String said = GitSafeText.withoutCredentials(Objects.requireNonNullElse(ex.getMessage(), ex.toString()));

                    Services.getInstance(p, Notifier.class).error(p, Bundle.message("clone.failed.title"), Bundle.message("clone.failed.message", said));
                }
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
