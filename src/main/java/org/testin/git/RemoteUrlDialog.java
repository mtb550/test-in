package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;

import java.util.List;
import java.util.function.Consumer;

/**
 * Asks where to push, the first time there is nowhere to push to.
 * <p>
 * On the framework, like every other dialog Testin asks a tester to fill in.
 * This one was the platform's own input box: only the title and one line of
 * prompt text were ours, so it had no status bar, no styling of ours and no way
 * to say anything the line did not - in the middle of a flow where every other
 * dialog does (#83).
 * <p>
 * It could not refuse either. A mistyped address, or a repository's web page
 * pasted in place of its clone URL, was accepted and came back as a failed push
 * some seconds later. {@link GitRefs#isRepositoryUrl} is the rule the create
 * project dialog already decides this by, and it decides it here too rather
 * than a second rule being written.
 * <p>
 * <b>Only the address is asked for.</b> Credentials stay the IDE's: commands run
 * through {@code GitLineHandler}, so git4idea prompts for them and stores what
 * the tester enters. Testin never sees a token, which is what makes the privacy
 * promise true.
 */
final class RemoteUrlDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull TextInput urlField;
    private final @NotNull Consumer<@NotNull String> onUrl;

    RemoteUrlDialog(final @NotNull Project p, final @NotNull String remoteName, final @NotNull Consumer<@NotNull String> onUrl) {
        super(p);
        this.onUrl = onUrl;

        title = "Configure Remote";

        final @NotNull ComponentDialogBase<TextInput> url = ComponentDialogBase.textField()
                .placeholder("https://github.com/user/repo.git")
                .build();

        components = List.of(
                ComponentDialogBase.message("This repository has nowhere to push to yet. What you type is added as the remote '"
                        + remoteName + "', and every push from here goes to it."),
                url);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        urlField = url.getComponent();
    }

    // UC-SHARE-013, Rule-SHARE-060
    @Override
    protected void submit() {
        final @NotNull String typed = urlField.getText().trim();

        if (typed.isEmpty()) {
            urlField.showEmptyWarning();
            return;
        }

        // Refused here rather than at the push, which is the whole point of
        // asking in a dialog of ours: the tester is still looking at what they
        // typed and can correct it.
        if (!GitRefs.isRepositoryUrl(typed)) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NOT_A_REPOSITORY_URL, typed);
            return;
        }

        onUrl.accept(typed);
        closeOk();
    }
}
