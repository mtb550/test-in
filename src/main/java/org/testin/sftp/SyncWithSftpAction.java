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
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.nio.file.Path;
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

    public SyncWithSftpAction(final @NotNull Project p, final @NotNull SimpleTree tree,
                                final @NotNull ExplorerPanel pp) {
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

    private void syncInBackground(final @NotNull SftpAddress address, final @NotNull Path projectRoot,
                                  final @NotNull SftpAccountDialog.Account account,
                                  final @NotNull String keyFile) {
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

                    indicator.setText("Rereading the project...");
                    Services.getInstance(p, ProjectIndexer.class).refreshDirectory(projectRoot);
                    Services.getInstance(p, ProjectIndexer.class).scanSingleProject(projectRoot);

                    report(outcome);
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
    private @NotNull SftpAuth authFor(final @NotNull SftpAddress address,
                                        final @NotNull SftpAccountDialog.Account account,
                                        final @NotNull String keyFile) {
        if (!keyFile.isEmpty()) {
            return SftpAuth.forKey(keyFile, () -> SftpSecret.KEY_PASSPHRASE.read(address, account.user()));
        }

        // What the tester just typed, before what was kept from last time - so a
        // corrected password works on the attempt they corrected it on, rather
        // than on the one after.
        if (!account.password().isEmpty()) return SftpAuth.withPassword(account.password());

        final @NotNull java.util.Optional<SftpAuth> agent = SshAgent.loadedIdentities().map(SftpAuth::withAgent);
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
    private void ask(final @NotNull SftpAddress address, final @NotNull Path projectRoot,
                     final @NotNull String keyFile) {
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

    private void report(final @NotNull SftpSync.Outcome outcome) {
        ApplicationManager.getApplication().invokeLater(() -> {
            pp.getProjectTree().refresh();

            final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
            if (outcome.conflicts() == 0 && outcome.removedOnServer().isEmpty()) {
                notifier.softShow(p, "Synced", outcome.describe());
                return;
            }

            if (!outcome.conflicting().isEmpty()) {
                notifier.warn(p, "Synced, with " + outcome.conflicts() + " left to you",
                        "Both sides changed " + naming(outcome.conflicting())
                                + ". This machine's copies were kept, and nothing was sent for them.");
            }

            if (!outcome.removedOnServer().isEmpty()) {
                notifier.warn(p, outcome.removedOnServer().size() + " gone from the server",
                        naming(outcome.removedOnServer()) + " were removed there and are still here. "
                                + "Nothing was deleted on this machine - delete them here to agree, "
                                + "or sync again after putting them back.");
            }
        });
    }

    /**
     * The first few by name, and a count for the rest.
     * <p>
     * Named because "2,249 files" sends a tester looking through a tree for
     * something the plugin already knows the name of.
     */
    private static @NotNull String naming(final @NotNull java.util.List<String> paths) {
        final @NotNull String first = String.join(", ", paths.stream().limit(3).toList());

        return paths.size() > 3 ? first + " and " + (paths.size() - 3) + " more" : first;
    }

    /**
     * The action is offered only where there is a project to sync.
     */
    public static @NotNull Optional<Path> projectFor(final @NotNull SimpleTree tree) {
        return TreeValueUtil.projectPath(tree);
    }
}
