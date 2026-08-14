package org.testin.generateReport;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.FileTypes;
import org.testin.ui.dialogs.DestinationForm;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Shortcuts;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Where to write a test run report: the destination form and a Generate button.
 * The same form the export dialog uses, so the two ask for a destination the
 * same way.
 */
public final class GenerateReportDialog extends AbstractFrameworkDialog<DestinationForm> {

    private final @NotNull BiConsumer<@NotNull FileTypes, @NotNull File> onGenerate;

    public GenerateReportDialog(final @NotNull Project p, final @NotNull String suggestedFileName,
                                final @NotNull BiConsumer<@NotNull FileTypes, @NotNull File> onGenerate) {
        super(p);
        this.onGenerate = onGenerate;

        title = "Generate Report";

        final DestinationForm form = new DestinationForm(p,
                Arrays.stream(FileTypes.values()).filter(FileTypes::isReportable).toArray(FileTypes[]::new),
                FileTypes.PDF,
                suggestedFileName,
                "Select Destination Folder",
                "Choose the folder to save the report in");

        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.button("Generate"));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));
    }

    @Override
    protected void submit() {
        final DestinationForm.Destination destination = component().resolve();
        if (destination == null) return;

        onGenerate.accept(destination.format(), destination.file());
        closeOk();
    }
}
