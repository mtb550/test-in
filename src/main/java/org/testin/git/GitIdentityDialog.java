package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * Collects the Git identity required for a first commit.
 * <p>
 * On the framework, like every other dialog: the constructor declares the
 * title, the components and the keys, and the status bar shows the keys it
 * declared. It used to be hand-built on the frameless wrapper, which bound
 * Enter and Escape into the root pane and told the tester about neither (#66).
 * <p>
 * The scope is two radios rather than a "set globally" checkbox. The question
 * has two named halves - this repository, or every repository on the machine -
 * and a radio pair says both out loud where a checkbox only names one and
 * leaves the other to be inferred from it being off.
 */
final class GitIdentityDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull TextInput nameField;
    private final @NotNull TextInput emailField;
    private final @NotNull RadioSelection<Boolean> scope;
    private final @NotNull Consumer<@NotNull Identity> onSet;

    GitIdentityDialog(final @NotNull Project p, final @NotNull Consumer<@NotNull Identity> onSet) {
        super(p);
        this.onSet = onSet;

        title = "Set Git Identity and Commit";

        final @NotNull ComponentDialogBase<TextInput> name = ComponentDialogBase.textField()
                .placeholder("your name...")
                .build();
        final @NotNull ComponentDialogBase<TextInput> email = ComponentDialogBase.textField()
                .placeholder("your email address...")
                .build();
        final @NotNull ComponentDialogBase<RadioSelection<Boolean>> where = ComponentDialogBase.<Boolean>radios("Apply to")
                .option("This repository", false)
                .option("Every repository on this machine", true)
                .select(false)
                .build();

        components = List.of(
                ComponentDialogBase.message("Git records who made a commit, and has no name or email to record yet."),
                name,
                email,
                where);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        nameField = name.getComponent();
        emailField = email.getComponent();
        scope = where.getComponent();
    }

    @Override
    protected void submit() {
        final @NotNull String name = nameField.getText().trim();
        if (name.isEmpty()) {
            nameField.showEmptyWarning();
            return;
        }

        final @NotNull String email = emailField.getText().trim();
        if (email.isEmpty()) {
            emailField.showEmptyWarning();
            return;
        }

        onSet.accept(new Identity(name, email, scope.getSelected()));
        closeOk();
    }

    /**
     * What the tester filled in: the two values Git needs, and whether they
     * belong to this repository or to the machine.
     */
    record Identity(@NotNull String name, @NotNull String email, boolean global) {
    }
}
