package org.testin.view.marker;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.NodeFigures;
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
 * The Details popup on a tree node: what the node is, where it lives, the audit
 * and status its marker carries, and how much is inside it.
 * <p>
 * The rows read the {@link Marker} contract, and that contract is the per-node
 * declaration - a marker with a status of its own answers
 * {@link Marker#getStatusLabel()}, one without answers blank, and the
 * framework's details builder drops a blank row. So a test set shows its
 * Deprecated or Active, a fixed container shows no Status row at all, and no
 * node type is named here (#68).
 * <p>
 * The counts arrive the same way: the node's {@link DirectoryType} declares
 * which of them apply and how they are gathered. So a test project counting
 * four things and a test run charting five are one line of code here (#82).
 * <p>
 * They are computed as this dialog is built and kept nowhere. The indexer
 * already holds the tree in memory, so a count is a walk of what is cached -
 * and one that is never stored cannot go stale behind a sync.
 */
public final class MarkerDetailsViewDialog extends AbstractFrameworkDialog<DialogDetails> {

    public MarkerDetailsViewDialog(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        super(p);

        final @NotNull Marker marker = dto.getMarker();
        final @NotNull DirectoryType type = dto.getType();
        final @NotNull NodeFigures figures = type.getStatistics().getGather().of(p, dto);

        title = "Details";

        final @NotNull ComponentDialogBase.DetailsBuilder details = ComponentDialogBase.details()
                .row("Name", dto.getName())
                .row("Path", dto.getPath().toString())
                .row("Created By", marker.getCreatedBy())
                .row("Created At", Display.formatDate(marker.getCreatedAt()))
                .row("Modified By", marker.getModifiedBy())
                .row("Modified At", Display.formatDate(marker.getModifiedAt()))
                .row("Status", marker.getStatusLabel());

        // Whatever else the marker has to say about itself - a run lists the
        // configuration it was created with. Added without asking what kind of
        // marker this is, the same way the status row is: a marker with nothing
        // to add returns nothing, and a blank value is dropped.
        marker.getDetailRows().forEach(extra -> details.row(extra.caption(), extra.value()));

        type.getCounts().forEach(count -> details.row(count.getCaption(), count.of(figures)));

        components = List.of(
                details.build(),
                ComponentDialogBase.of(new VerdictDonut(type.getStatistics().getSlices(), figures)));

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, "Close", this::closeCancel));

        // Sized rather than packed so it stays movable and resizable, as it was,
        // and tall enough for the node that shows the most: the counts add a row
        // each, and a test run adds its chart beneath them.
        preferredSize = JBUI.size(600, 500);
    }

    @Override
    protected void submit() {
        closeOk();
    }
}
