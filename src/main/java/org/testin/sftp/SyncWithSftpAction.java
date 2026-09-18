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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.FailureText;
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

            // A balloon for the same reason as above: nothing failed, the tester
            // pressed the button with no test project selected (#66, finding 137).
            selectedProject.ifPresentOrElse(
                    projectRoot -> askThenSync(address, projectRoot),
                    () -> Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("sftp.nothing.title"),
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

                    // Not final, and it cannot be: the catch below needs to know
                    // which stored secret the attempt used, and working that out
                    // reads the credential store and probes the SSH agent - both
                    // of which belong inside the try that answers for them.
                    @NotNull Proof proof = Proof.NOTHING_STORED;

                    try {
                        proof = authFor(address, account, keyFile);
                        final @NotNull SftpAuth auth = proof.auth();
                        if (auth == SftpAuth.NONE) {
                            // Nothing on this machine can prove who this is, so the
                            // tester is asked - rather than being shown the server's
                            // refusal, which says nothing about what to do next.
                            ApplicationManager.getApplication().invokeLater(() -> ask(address, projectRoot, keyFile));
                            return;
                        }

                        // A run change made from before the sync reads this project
                        // until after its last scan waits for it, and lands on the
                        // runs that arrived (#66, findings 129 and 144).
                        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                        final @NotNull SftpSync.Outcome outcome = indexer.whileSyncing(projectRoot, () -> {
                            final @NotNull SftpSync.Outcome synced = SftpSync.run(
                                    p, projectRoot, address, account.user(), auth, knownHosts(), indicator);

                            // Nothing moved and nothing to reread: somebody else is
                            // syncing this project, and the tester is told who.
                            if (!synced.isBlocked()) {
                                indicator.setText(Bundle.message("sftp.progress.rereading"));
                                indexer.refreshDirectory(projectRoot);
                                indexer.scanSingleProject(projectRoot);
                            }
                            return synced;
                        });

                        // Kept only now that the server has accepted it. Stored
                        // before connecting, a mistyped password was kept, the
                        // account window never opened again, and every later sync
                        // failed on it (#312, A34).
                        //
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

                        report(outcome, projectRoot, address, account, auth);

                    } catch (final SftpTransport.AuthRefused refused) {
                        // The one failure with something to do about it. A secret
                        // this machine kept and the server will not take is worth
                        // nothing, and keeping it meant every later sync failed on
                        // it while the account window never opened again (#312,
                        // N7). Rule-SHARE-113. Forgotten first, so the dialog that follows is
                        // answering a question rather than fighting what is stored.
                        Logger.warn("The server refused the stored credentials for " + account.user() + "@" + address.display());
                        proof.stored().ifPresent(secret -> secret.forget(address, account.user()));

                        ApplicationManager.getApplication().invokeLater(() -> {
                            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("sftp.auth.refused"));
                            ask(address, projectRoot, keyFile);
                        });

                    } catch (final ProcessCanceledException stopped) {
                        // The tester pressed Cancel, which is an answer rather than
                        // a failure, so it goes back to the platform to close the
                        // bar - not into the catch below as "Sync Failed" (#312,
                        // A35; the same as BackgroundWork, #66 finding 83).
                        throw stopped;
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
        private @NotNull Proof authFor(final @NotNull SftpAddress address, final @NotNull SftpAccountDialog.Account account, final @NotNull String keyFile) {
            // Rule-SETTING-036. The passphrase read here is always empty, and
            // that is the rule rather than a gap: nothing in Testin asks for one
            // or stores one, so a key protected by a passphrase works only
            // through an agent already holding it. The read stays because it is
            // where a passphrase would arrive if asking for one is ever built,
            // and because JSch wants a supplier either way. The rule used to
            // promise the asking, which was never true (#312, A36).
            if (!keyFile.isEmpty()) {
                return new Proof(SftpAuth.forKey(keyFile, () -> SftpSecret.KEY_PASSPHRASE.read(address, account.user())),
                        Optional.of(SftpSecret.KEY_PASSPHRASE));
            }

            // What the tester just typed, before what was kept from last time - so a
            // corrected password works on the attempt they corrected it on, rather
            // than on the one after.
            //
            // Nothing stored behind it: a typed password is only kept once the
            // server has taken it (A34), so a refusal has nothing to forget.
            if (!account.password().isEmpty()) return Proof.of(SftpAuth.withPassword(account.password()));

            final @NotNull Optional<SftpAuth> agent = SshAgent.loadedIdentities().map(SftpAuth::withAgent);
            if (agent.isPresent()) return Proof.of(agent.orElseThrow());

            final @NotNull String stored = SftpSecret.ACCOUNT_PASSWORD.read(address, account.user());

            // NONE means nobody can be asked to accept this connection, which the
            // caller turns into a question rather than a failure.
            return stored.isEmpty()
                    ? Proof.NOTHING_STORED
                    : new Proof(SftpAuth.withPassword(stored), Optional.of(SftpSecret.ACCOUNT_PASSWORD));
        }

        /**
         * UC-SHARE-020, Rule-SHARE-095.
         * <p>
         * How this machine proved who it is, and which secret that took out of the
         * credential store.
         * <p>
         * The second half is only ever read by a refusal, and it has to come from
         * the attempt itself rather than be worked out again afterwards: whether
         * the agent answered decides whether the kept password was tried at all,
         * and an agent that was running a second ago may not be now. Forgetting
         * the wrong secret is worse than forgetting none (#312, N7).
         */
        private record Proof(@NotNull SftpAuth auth, @NotNull Optional<SftpSecret> stored) {

            /**
             * Nothing this machine can prove, and nothing kept that could have
             * been wrong. Also what a refusal reads when the attempt never got as
             * far as choosing.
             */
            private static final @NotNull Proof NOTHING_STORED = new Proof(SftpAuth.NONE, Optional.empty());

            private static @NotNull Proof of(final @NotNull SftpAuth auth) {
                return new Proof(auth, Optional.empty());
            }
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
         * UC-SHARE-022, Rule-SHARE-101.
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
        private void askAboutConflicts(final @NotNull List<Unsettled> unsettled, final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull SftpAccountDialog.Account account, final @NotNull SftpAuth auth, final @NotNull Map<String, Answered> answered) {
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

                // With the version it is an answer to. A colleague can sync a
                // newer copy of this very test case while these windows are
                // open, and an answer about the old one must not be written over
                // it (#312, A33).
                answered.put(next.path(), new Answered(mapper.writeValueAsString(next.merged()), next.theirs()));

                // Once this window has closed. The callback runs before it does,
                // and a second window of the same kind asked to show while the
                // first is still on screen only brings the first forward - so the
                // next question never opened and nothing was ever sent. The Git
                // chain continues the same way (ConflictResolution.ask).
                ApplicationManager.getApplication().invokeLater(() ->
                        askAboutConflicts(rest, projectRoot, address, account, auth, answered));
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
        private void send(final @NotNull Map<String, Answered> answered, final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull SftpAccountDialog.Account account, final @NotNull SftpAuth auth) {
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
                    //
                    // Under the same hold as the sync, since settling writes the
                    // answers into this project too (#66, finding 144).
                    final int settled = Services.getInstance(p, ProjectIndexer.class).whileSyncing(projectRoot,
                            () -> SftpSync.finish(p, projectRoot, address, account.user(), auth, knownHosts(), answered));

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
            // The exception's own name when it carries no message, rather than
            // "null" in the log and a null handed to the notification (#312, A96).
            final @NotNull String reason = FailureText.of(ex);
            Logger.error(whatFailed + " failed: " + reason);

            ApplicationManager.getApplication().invokeLater(() ->
                    Services.getInstance(p, Notifier.class).error(p, Bundle.message("sftp.sync.failed.title"), reason));
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
