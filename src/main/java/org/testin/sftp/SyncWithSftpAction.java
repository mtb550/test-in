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

package org.testin.sftp;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.config.TestinConfigService;
import org.testin.explorer.TreePanel;
import org.testin.explorer.tree.TreeValues;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;
import org.testin.notifications.Done;
import org.testin.git.ResolveConflictDialog;
import org.testin.git.TestCaseMerge;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.editor.TestinEditors;
import org.testin.util.Mapper;
import org.testin.setting.AppSettingsState;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Optional;

/**
 * Sends the test project to its server and brings back what is there (#94).
 * <p>
 * The second way a team can share a suite, beside Git and independent of it -
 * for testers who have a server and an account on it and nothing else.
 * <p>
 * The address comes from {@code testin.yml}, which travels with the repository,
 * so a colleague who opens it is already pointed at the same place. The account
 * and anything secret stay on this machine.
 * <p>
 * Declared in {@code plugin.xml} (#119), so Find Action offers it and a tester
 * can bind a key to it - it has never had one. No constructor and no fields: the
 * platform builds one instance for the whole IDE, so the project comes from the
 * keystroke and the sync itself is {@link Work}.
 */
public final class SyncWithSftpAction extends DumbAwareAction {

    // UC-SHARE-019, Rule-SHARE-085
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        new Work(p).start(TestinData.tree(e).flatMap(TreeValues::projectPath));
    }

    /**
     * UC-SHARE-019.
     * <p>
     * Off unless {@code testin.yml} says this project is reached over a server.
     * <p>
     * The connection carries the answer, the way it carries whether a branch box
     * belongs on screen - so this asks the connection rather than testing which
     * one it is.
     * <p>
     * And off outside the Testin tree, which is where the test project to send
     * is read from - so a key bound to this is inert in a Java file (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();

        e.getPresentation().setEnabled(p != null
                && TestinData.tree(e).isPresent()
                && Services.getInstance(p, TestinConfigService.class).get().connection().isSyncsToServer());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads a config already in memory, and no Swing state.
        return ActionUpdateThread.BGT;
    }

    /**
     * Syncing one test project with its server, for a project that is there.
     */
    private record Work(@NotNull Project p) {

        /**
         * UC-SHARE-019, Rule-SHARE-085.
         * <p>
         * The two answers a sync needs before it can start: a server in
         * {@code testin.yml}, and a test project in the tree to send.
         */
        private void start(final @NotNull Optional<Path> selectedProject) {
            final @NotNull SftpAddress address =
                    Services.getInstance(p, TestinConfigService.class).get().sftpAddress();

            // A balloon, not a notification that stays in the log. Nothing is wrong
            // here: a repository reached over Git has no server by design, and the
            // tester found that out by pressing the button, under their own hand. A
            // logged warning would keep saying so afterwards, about a setup that is
            // correct.
            if (!address.isConfigured()) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("sftp.not.configured.title"),
                        Bundle.message("sftp.not.configured.message"));
                return;
            }

            selectedProject.ifPresentOrElse(
                    projectRoot -> askThenSync(address, projectRoot),
                    () -> Services.getInstance(p, Notifier.class).error(p, Bundle.message("sftp.nothing.title"),
                            Bundle.message("sftp.nothing.message")));
        }

        /**
         * UC-SHARE-019.
         * <p>
         * Collects the account on the EDT, then leaves it.
         * <p>
         * The dialog is Swing and the sync is network and disk, so the two cannot
         * share a thread. Nothing here holds a lock while it waits.
         */
        private void askThenSync(final @NotNull SftpAddress address, final @NotNull Path projectRoot) {
            final @NotNull AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);
            final @NotNull String knownUser = settings.sftpUser.isEmpty()
                    ? System.getProperty("user.name", "")
                    : settings.sftpUser;

            // Asked only when there is something to ask. An account already in the
            // settings, with a key, an agent or a password behind it, is everything
            // this needs - and a dialog on every sync for answers it already has is
            // the kind of thing a tester stops reading.
            if (!settings.sftpUser.isEmpty()) {
                syncInBackground(address, projectRoot,
                        new SftpAccountDialog.Account(settings.sftpUser, ""), settings.sftpKeyFile);
                return;
            }

            // The dialog hands back on the EDT, so nothing slow happens here - the
            // account is carried into the background task and kept there. Storing a
            // secret writes to the OS keychain, which the platform rightly refuses
            // to do on the EDT.
            new SftpAccountDialog(p, address, knownUser, account -> {
                settings.sftpUser = account.user();
                syncInBackground(address, projectRoot, account, settings.sftpKeyFile);
            }).show();
        }

        // UC-SHARE-019, Rule-SHARE-091
        private void syncInBackground(final @NotNull SftpAddress address, final @NotNull Path projectRoot, final @NotNull SftpAccountDialog.Account account, final @NotNull String keyFile) {
            ProgressManager.getInstance().run(new Task.Backgroundable(p, Bundle.message("sftp.task.syncing", address.display()), true) {
                @Override
                public void run(final @NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);

                    try {
                        // Off the EDT, where writing to the keychain is allowed. A
                        // refusal is said once, here: nothing read the answer
                        // before, so a keychain that would not take the password
                        // asked the tester for it again every single sync with no
                        // explanation - which is the outcome store's own contract
                        // says must not happen.
                        if (!account.password().isEmpty() && !SftpSecret.ACCOUNT_PASSWORD.store(address, account.user(), account.password())) {
                            ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, Notifier.class)
                                    .softRefuse(p, Bundle.message("sftp.password.not.kept.title"), Bundle.message("sftp.password.not.kept.message")));
                        }

                        final @NotNull SftpAuth auth = authFor(address, account, keyFile);
                        if (auth == SftpAuth.NONE) {
                            // Nothing on this machine can prove who this is, so the
                            // tester is asked - rather than being shown the server's
                            // refusal, which says nothing about what to do next.
                            ApplicationManager.getApplication().invokeLater(() -> ask(address, projectRoot, keyFile));
                            return;
                        }

                        final @NotNull SftpSync.Outcome outcome = SftpSync.run(
                                p, projectRoot, address, account.user(), auth, knownHosts(), indicator);

                        // Nothing moved and nothing to reread: somebody else is
                        // syncing this project, and the tester is told who.
                        if (outcome.isBlocked()) {
                            report(outcome, projectRoot, address, account, auth);
                            return;
                        }

                        indicator.setText(Bundle.message("sftp.progress.rereading"));
                        Services.getInstance(p, ProjectIndexer.class).refreshDirectory(projectRoot);
                        Services.getInstance(p, ProjectIndexer.class).scanSingleProject(projectRoot);

                        report(outcome, projectRoot, address, account, auth);
                    } catch (final Exception ex) {
                        reportFailure(p, "Sync with " + address.display(), ex);
                    }
                }
            });
        }

        /**
         * UC-SHARE-020, Rule-SHARE-095.
         * <p>
         * How this machine proves who it is: the agent when one holds keys, then a
         * key file, then the password kept for this server.
         */
        private @NotNull SftpAuth authFor(final @NotNull SftpAddress address, final @NotNull SftpAccountDialog.Account account, final @NotNull String keyFile) {
            if (!keyFile.isEmpty()) {
                return SftpAuth.forKey(keyFile, () -> SftpSecret.KEY_PASSPHRASE.read(address, account.user()));
            }

            // What the tester just typed, before what was kept from last time - so a
            // corrected password works on the attempt they corrected it on, rather
            // than on the one after.
            if (!account.password().isEmpty()) return SftpAuth.withPassword(account.password());

            final @NotNull Optional<SftpAuth> agent = SshAgent.loadedIdentities().map(SftpAuth::withAgent);
            if (agent.isPresent()) return agent.orElseThrow();

            final @NotNull String stored = SftpSecret.ACCOUNT_PASSWORD.read(address, account.user());

            // NONE means nobody can be asked to accept this connection, which the
            // caller turns into a question rather than a failure.
            return stored.isEmpty() ? SftpAuth.NONE : SftpAuth.withPassword(stored);
        }

        /**
         * Opens the account dialog, for the two moments it is needed: no account
         * saved yet, and nothing on this machine able to prove the saved one.
         */
        private void ask(final @NotNull SftpAddress address, final @NotNull Path projectRoot, final @NotNull String keyFile) {
            final @NotNull AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);

            new SftpAccountDialog(p, address, settings.sftpUser, account -> {
                settings.sftpUser = account.user();
                syncInBackground(address, projectRoot, account, keyFile);
            }).show();
        }

        /**
         * The hosts this machine already trusts. A server that is not in it is
         * refused rather than trusted - the tester adds it once, with {@code ssh},
         * the same way every other tool on the machine learns a host.
         */
        private static @NotNull Path knownHosts() {
            return Path.of(System.getProperty("user.home", ""), ".ssh", "known_hosts");
        }

        // UC-SHARE-019
        private void report(final @NotNull SftpSync.Outcome outcome, final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull SftpAccountDialog.Account account, final @NotNull SftpAuth auth) {
            ApplicationManager.getApplication().invokeLater(() -> {
                final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

                if (outcome.isBlocked()) {
                    notifier.warn(p, Bundle.message("sftp.blocked.title"),
                            Bundle.message("sftp.blocked.message", outcome.blockedBy()));
                    return;
                }

                // The pair the Refresh button makes: the tree for the structure and
                // the open editors for their contents, because a sync can rewrite a
                // case an editor is showing (#118).
                Services.getInstance(p, TreePanel.class).getProjectTree().refresh();
                Services.getInstance(p, TestinEditors.class).refreshOpen(p);

                // One notification, always, and it always carries what the sync
                // actually did. The pair of conditions this replaces had a gap
                // exactly where a tester most wants to hear something: deletions on
                // the server with no conflicts matched neither branch, so twelve
                // files uploaded and a question about removals arrived with nothing
                // ever saying what had been sent.
                if (outcome.conflicting().isEmpty()) {
                    // Stays, like the Git sync and the push. A sync that worked
                    // while the tester was elsewhere is exactly the message they
                    // come back to (#268).
                    notifier.info(p, Bundle.message("sftp.synced.title"), outcome.describe());
                } else {
                    notifier.warn(p, Bundle.message("sftp.synced.conflicts.title", String.valueOf(outcome.conflicts())),
                            Bundle.message("sftp.synced.conflicts.message", outcome.describe(), naming(outcome.conflicting())));
                }

                askAboutDeletions(outcome, projectRoot);
                askAboutConflicts(outcome.unsettled(), projectRoot, address, account, auth, new TreeMap<>());
            });
        }

        /**
         * UC-SHARE-022, Rule-SHARE-099.
         * <p>
         * Offers to remove what the server no longer holds.
         * <p>
         * Asked rather than done, because a deletion is the one thing a sync cannot
         * take back: the file is gone from both sides and there is no third copy.
         * The count is in the question, because 3 files and 1,204 files deserve very
         * different amounts of thought.
         */
        private void askAboutDeletions(final @NotNull SftpSync.Outcome outcome, final @NotNull Path projectRoot) {
            if (outcome.removedOnServer().isEmpty()) return;

            final int count = outcome.removedOnServer().size();
            final @NotNull String what = count == 1
                    ? Bundle.message("sftp.removed.file.one")
                    : Bundle.message("sftp.removed.file.many", String.valueOf(count));

            new ConfirmDialog(p, Bundle.message("sftp.removed.title"),
                    Bundle.message("sftp.removed.message", what, naming(outcome.removedOnServer())),
                    "", "", Bundle.message("sftp.removed.confirm", what),
                    () -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        Services.getInstance(p, ProjectIndexer.class)
                                .removeIncoming(projectRoot, outcome.removedOnServer());

                        ApplicationManager.getApplication().invokeLater(() -> {
                            Services.getInstance(p, TreePanel.class).getProjectTree().refresh();
                            Services.getInstance(p, TestinEditors.class).refreshOpen(p);
                            Services.getInstance(p, Notifier.class).softShowCounted(p, Done.REMOVED, count);
                        });
                    }),
                    List.of(new ConfirmDialog.Alternative(Shortcuts.ConfirmAlternative, Bundle.message("sftp.removed.keep", what),
                            () -> keepThem(outcome, projectRoot, count)))).show();
        }

        /**
         * UC-SHARE-022, Rule-SHARE-100.
         * <p>
         * Keeping them, and meaning it. Escape still leaves the question open for
         * next time, which is what a tester who has not decided wants; this is the
         * answer that settles it.
         */
        private void keepThem(final @NotNull SftpSync.Outcome outcome, final @NotNull Path projectRoot, final int count) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final boolean kept = SftpSync.keep(p, projectRoot, outcome.removedOnServer());

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!kept) {
                        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("sftp.not.kept.title"),
                                Bundle.message("sftp.not.kept.message"));
                        return;
                    }

                    Services.getInstance(p, Notifier.class).softShowCounted(p, Done.KEPT, count);
                });
            });
        }

        /**
         * UC-SHARE-021, Rule-SHARE-097.
         * <p>
         * Puts one case both testers rewrote in front of the tester, then the next,
         * and sends the answers when there are no more.
         * <p>
         * One at a time, because each answer is a choice between two versions of a
         * sentence somebody wrote, and a dialog holding six of those is a dialog
         * nobody reads. The same dialog the Git channel opens, on the same merge, so
         * a conflict looks the same however the team shares their work.
         */
        private void askAboutConflicts(final @NotNull List<Unsettled> unsettled, final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull SftpAccountDialog.Account account, final @NotNull SftpAuth auth, final @NotNull Map<String, String> answered) {
            if (unsettled.isEmpty()) {
                send(answered, projectRoot, address, account, auth);
                return;
            }

            final @NotNull Unsettled next = unsettled.getFirst();
            final @NotNull List<Unsettled> rest = List.copyOf(unsettled.subList(1, unsettled.size()));
            final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);

            new ResolveConflictDialog(p, next.name(), next.questions(), next.settled(), takeTheirs -> {
                for (final TestCaseMerge.Question question : next.questions()) {
                    TestCaseMerge.answer(mapper, next.merged(), question, takeTheirs.contains(question.field()),
                            next.theirs());
                }

                answered.put(next.path(), mapper.writeValueAsString(next.merged()));
                askAboutConflicts(rest, projectRoot, address, account, auth, answered);
                // Escape is a skip, not a cancel: this test case is left as the
                // server has it and the sync goes on. It used to end the whole sync,
                // so the rest were never asked about and nothing already answered
                // was sent (#258).
            }, () -> askAboutConflicts(rest, projectRoot, address, account, auth, answered)).show();
        }

        /**
         * UC-SHARE-021.
         * <p>
         * Sends what the tester settled, off the EDT - it opens a connection.
         */
        private void send(final @NotNull Map<String, String> answered, final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull SftpAccountDialog.Account account, final @NotNull SftpAuth auth) {
            if (answered.isEmpty()) return;

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    // How many reached the server, not how many were answered.
                    // finish returns early when another machine holds the sync
                    // lock - it writes a log line and nothing else - so a tester
                    // who had just answered six merge questions was told
                    // "Settled 6" when not a byte had left the machine, and met
                    // the same six questions on the next sync with no
                    // explanation. A connection that drops half way down the
                    // list is the same lie with a smaller number behind it.
                    final int settled = SftpSync.finish(p, projectRoot, address, account.user(), auth, knownHosts(), answered);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        Services.getInstance(p, TreePanel.class).getProjectTree().refresh();
                        Services.getInstance(p, TestinEditors.class).refreshOpen(p);

                        if (settled > 0) {
                            Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("sftp.settled", String.valueOf(settled)));
                        } else {
                            Services.getInstance(p, Notifier.class).warn(p, Bundle.message("sftp.nothing.settled.title"),
                                    Bundle.message("sftp.nothing.settled.message"));
                        }
                    });

                } catch (final Exception ex) {
                    // The same handling the sync itself has. Without it a connection
                    // that dropped while sending showed the tester nothing at all.
                    reportFailure(p, "Settling with " + address.display(), ex);
                }
            });
        }

        /**
         * Says a step of the sync failed, in the log and to the tester.
         * <p>
         * One reporter for both, because they are the same report: what was being
         * done, and what went wrong. Written out at each site, the two had already
         * started to differ in the log line while sharing the balloon title, and the
         * title is the part a second copy quietly stops agreeing on.
         */
        private static void reportFailure(final @NotNull Project p, final @NotNull String whatFailed, final @NotNull Exception ex) {
            Logger.error(whatFailed + " failed: " + ex.getMessage());

            ApplicationManager.getApplication().invokeLater(() ->
                    Services.getInstance(p, Notifier.class).error(p, Bundle.message("sftp.sync.failed.title"), ex.getMessage()));
        }

        /**
         * The first few by name, and a count for the rest.
         * <p>
         * Named because "2,249 files" sends a tester looking through a tree for
         * something the plugin already knows the name of.
         */
        private static @NotNull String naming(final @NotNull List<String> paths) {
            final @NotNull String first = String.join(", ", paths.stream().limit(3).toList());

            return paths.size() > 3
                    ? Bundle.message("sftp.naming.more", first, String.valueOf(paths.size() - 3))
                    : first;
        }
    }
}
