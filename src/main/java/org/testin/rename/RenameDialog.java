package org.testin.rename;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.AbstractInputPopupDialog;

import java.util.function.Consumer;

/**
 * Rename popup using the shared dynamic input-dialog design.
 */
final class RenameDialog extends AbstractInputPopupDialog {

    private final @NotNull Consumer<@NotNull String> onSubmit;

    RenameDialog(final @NotNull Project project,
                 final @NotNull String currentName,
                 final @NotNull Consumer<@NotNull String> onSubmit) {
        super(project, "Rename", AllIcons.Actions.Edit, "set new name..", currentName);
        this.onSubmit = onSubmit;
        setLeadingIcon(AllIcons.Actions.Edit);
        initializeInputPopup();
    }

    @Override
    protected void onSubmit(final @NotNull String value) {
        onSubmit.accept(value);
    }
}
