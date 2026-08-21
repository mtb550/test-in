package org.testin.view.marker;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.markers.Marker;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogDetails;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Display;
import org.testin.util.Shortcuts;

import java.util.List;

/**
 * The Details popup on a tree node: what the node is, where it lives, and the
 * audit and status its marker carries.
 * <p>
 * The rows read the {@link Marker} contract, and that contract is the per-node
 * declaration - a marker with a status of its own answers
 * {@link Marker#getStatusLabel()}, one without answers blank, and the
 * framework's details builder drops a blank row. So a test set shows its
 * Deprecated or Active, a fixed container shows no Status row at all, and no
 * node type is named here (#68).
 */
public final class MarkerDetailsViewDialog extends AbstractFrameworkDialog<DialogDetails> {

    public MarkerDetailsViewDialog(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        super(p);

        final @NotNull Marker marker = dto.getMarker();

        title = "Details";

        components = List.of(ComponentDialogBase.details()
                .row("Name", dto.getName())
                .row("Path", dto.getPath().toString())
                .row("Created By", marker.getCreatedBy())
                .row("Created At", Display.formatDate(marker.getCreatedAt()))
                .row("Modified By", marker.getModifiedBy())
                .row("Modified At", Display.formatDate(marker.getModifiedAt()))
                .row("Status", marker.getStatusLabel())
                .build());

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, "Close", this::closeCancel));

        // Sized rather than packed so it stays movable and resizable, as it was.
        preferredSize = JBUI.size(600, 400);
    }

    @Override
    protected void submit() {
        closeOk();
    }
}
