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

package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

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

        title = Bundle.message("dialog.remote.title");

        final @NotNull ComponentDialogBase<TextInput> url = ComponentDialogBase.textField()
                .placeholder("https://github.com/user/repo.git")
                .build();

        components = List.of(
                ComponentDialogBase.message(Bundle.message("dialog.remote.message", remoteName)),
                url);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        urlField = url.getComponent();
    }

    // UC-SHARE-013, Rule-SHARE-060, Rule-INTERNAL-067
    @Override
    protected void submit() {
        final @NotNull String typed = accepted(urlField, GitRefs::isRepositoryUrl, Refused.NOT_A_REPOSITORY_URL);
        if (typed.isEmpty()) return;

        onUrl.accept(typed);
        closeOk();
    }
}
