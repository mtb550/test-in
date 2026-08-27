package org.testin.sftp;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.config.TestinConfigService;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.git.ResolveConflictDialog;
import org.testin.git.TestCaseMerge;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.EditorUtil;
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
 */
public final class SyncWithSftpAction extends AbstractProjectTreeAction {

    private final @NotNull ExplorerPanel pp;

    public SyncWithSftpAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull ExplorerPanel pp) {
        super(p, tree, "Sync With SFTP", "Send this test project to its server and take what is there",
                AllIcons.Actions.Upload);
        this.pp = pp;
    }

    /**
     * Off unless {@code testin.yml} says this project is reached over a server.
     * <p>
     * The connection carries the answer, the way it carries whether a branch box
     * belongs on screen - so this asks the connection rather than testing which
     * one it is.
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(
                Services.getInstance(p, TestinConfigService.class).get().connection().isSyncsToServer());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads a config already in memory, and no Swing state.
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull SftpAddress address =
                Services.getInstance(p, TestinConfigService.class).get().sftpAddress();

        // A balloon, not a notification that stays in the log. Nothing is wrong
        // here: a repository reached over Git has no server by design, and the
        // tester found that out by pressing the button, under their own hand. A
        // logged warning would keep saying so afterwards, about a setup that is
        // correct.
        if (!address.isConfigured()) {
            Services.getInstance(p, Notifier.class).softShow(p, "No SFTP Server Configured",
                    "Set connection: sftp and sftpHost in testin.yml");
            return;
        }

        TreeValueUtil.projectPath(tree).ifPresentOrElse(
                projectRoot -> askThenSync(address, projectRoot),
                () -> Services.getInstance(p, Notifier.class).error(p, "Nothing to Sync",
                        "Select a test project in the tree first."));
    }

    /**
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

    private void syncInBackground(final @NotNull SftpAddress address, final @NotNull Path projectRoot, final @NotNull SftpAccountDialog.Account account, final @NotNull String keyFile) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Syncing with " + address.display(), true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                try {
                    // Off the EDT, where writing to the keychain is allowed.
                    if (!account.password().isEmpty()) {
                        SftpSecret.ACCOUNT_PASSWORD.store(address, account.user(), account.password());
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

                    indicator.setText("Rereading the project...");
                    Services.getInstance(p, ProjectIndexer.class).refreshDirectory(projectRoot);
                    Services.getInstance(p, ProjectIndexer.class).scanSingleProject(projectRoot);

                    report(outcome, projectRoot, address, account, auth);
                } catch (final Exception ex) {
                    Logger.error("Sync with " + address.display() + " failed: " + ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).error(p, "Sync Failed", ex.getMessage()));
                }
            }
        });
    }

    /**
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

    private void report(final @NotNull SftpSync.Outcome outcome, final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull SftpAccountDialog.Account account, final @NotNull SftpAuth auth) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

            if (outcome.isBlocked()) {
                notifier.warn(p, "Somebody else is syncing this project", outcome.blockedBy()
                        + " Nothing was sent or fetched. Try again when they have finished.");
                return;
            }

            // The pair the Refresh button makes: the tree for the structure and
            // the open editors for their contents, because a sync can rewrite a
            // case an editor is showing (#118).
            pp.getProjectTree().refresh();
            Services.getInstance(p, EditorUtil.class).refreshOpen(p);

            if (outcome.conflicts() == 0 && outcome.removedOnServer().isEmpty()) {
                notifier.softShow(p, "Synced", outcome.describe());
            } else if (!outcome.conflicting().isEmpty()) {
                notifier.warn(p, "Synced, with " + outcome.conflicts() + " left to you",
                        "Both sides changed " + naming(outcome.conflicting())
                                + ". This machine kept its copies and sent nothing for them; "
                                + "you'll be asked about anything that can be merged field by field.");
            }

            askAboutDeletions(outcome, projectRoot);
            askAboutConflicts(outcome.unsettled(), projectRoot, address, account, auth, new TreeMap<>());
        });
    }

    /**
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
        final @NotNull String what = count == 1 ? "1 file" : count + " files";

        new ConfirmDialog(p, "Removed On The Server",
                what + " here were deleted on the server by somebody else, and this machine has not "
                        + "touched them since: " + naming(outcome.removedOnServer())
                        + ". Removing them here agrees with that. Keeping them offers the same choice "
                        + "again on the next sync.",
                "", "", "Remove " + what,
                () -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    Services.getInstance(p, ProjectIndexer.class)
                            .removeIncoming(projectRoot, outcome.removedOnServer());

                    ApplicationManager.getApplication().invokeLater(() -> {
                        pp.getProjectTree().refresh();
                        Services.getInstance(p, EditorUtil.class).refreshOpen(p);
                        Services.getInstance(p, Notifier.class).softShow(p, "Removed " + count);
                    });
                })).show();
    }

    /**
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

        new ResolveConflictDialog(p, next.name(), next.questions(), takeTheirs -> {
            for (final TestCaseMerge.Question question : next.questions()) {
                TestCaseMerge.answer(mapper, next.merged(), question, takeTheirs.contains(question.field()),
                        next.theirs());
            }

            answered.put(next.path(), mapper.writeValueAsString(next.merged()));
            askAboutConflicts(rest, projectRoot, address, account, auth, answered);
        }).show();
    }

    /**
     * Sends what the tester settled, off the EDT - it opens a connection.
     */
    private void send(final @NotNull Map<String, String> answered, final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull SftpAccountDialog.Account account, final @NotNull SftpAuth auth) {
        if (answered.isEmpty()) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            SftpSync.finish(p, projectRoot, address, account.user(), auth, knownHosts(), answered);

            ApplicationManager.getApplication().invokeLater(() -> {
                pp.getProjectTree().refresh();
                Services.getInstance(p, EditorUtil.class).refreshOpen(p);
                Services.getInstance(p, Notifier.class).softShow(p, "Settled " + answered.size());
            });
        });
    }

    /**
     * The first few by name, and a count for the rest.
     * <p>
     * Named because "2,249 files" sends a tester looking through a tree for
     * something the plugin already knows the name of.
     */
    private static @NotNull String naming(final @NotNull List<String> paths) {
        final @NotNull String first = String.join(", ", paths.stream().limit(3).toList());

        return paths.size() > 3 ? first + " and " + (paths.size() - 3) + " more" : first;
    }
}
