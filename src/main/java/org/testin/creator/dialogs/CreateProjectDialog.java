package org.testin.creator.dialogs;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitRefs;
import org.testin.model.DirectoryType;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;

import java.util.List;
import java.util.function.Consumer;

/**
 * Creates a test project — new, or imported by cloning a Git URL.
 * <p>
 * One field, no choice to make: what was typed says which it is. A repository
 * URL is clonable and a project name is not, so asking the tester to also pick
 * from a list was asking them to repeat themselves — and to be wrong. The
 * placeholder already promised this ("set name or paste url...") long before the
 * dialog behaved that way.
 */
public final class CreateProjectDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull Consumer<@NotNull String> onCreate;

    // UC-TREE-PANEL-002, UC-TREE-PANEL-003
    public CreateProjectDialog(final @NotNull Project p, final @NotNull Consumer<@NotNull String> onCreate) {
        super(p);
        this.onCreate = onCreate;

        title = "Create Project";

        components = List.of(
                ComponentDialogBase.textField()
                        .icon(DirectoryType.TP.getIcon())
                        .placeholder("set name or paste url...")
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-005, Rule-TREE-PANEL-095
    @Override
    protected void submit() {
        final @NotNull String name = accepted(component(), CreateProjectDialog::isNameOrUrl, Refused.NOT_A_JAVA_NAME);
        if (name.isEmpty()) return;

        onCreate.accept(name);
        closeOk();
    }

    /**
     * UC-TREE-PANEL-002, UC-TREE-PANEL-003, Rule-TREE-PANEL-095.
     * <p>
     * Whether what was typed is something this dialog can act on - and the one
     * field means the rule has two halves, the way the dialog itself does.
     * <p>
     * A test project's name becomes the first Java package of everything under
     * it, so it has to be a name Java accepts. A repository address does not: the
     * folder is named by {@code testin.yml} rather than by the URL, so a URL is
     * never asked to be a Java name and refusing it for not being one would
     * refuse the clone this dialog exists to offer.
     */
    private static boolean isNameOrUrl(final @NotNull String typed) {
        return GitRefs.isRepositoryUrl(typed) || DirectoryType.TP.canTakeName(typed);
    }
}
