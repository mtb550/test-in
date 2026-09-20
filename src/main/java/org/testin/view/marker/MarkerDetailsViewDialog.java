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

package org.testin.view.marker;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.NodeCounter;
import org.testin.model.DirectoryType;
import org.testin.model.NodeFigures;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.markers.Marker;
import org.testin.testcase.TestEditorAttributes;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogDetails;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
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
        final @NotNull NodeFigures figures = NodeCounter.figures(p, dto);

        title = Bundle.message("dialog.details.title");

        final @NotNull ComponentDialogBase.DetailsBuilder details = ComponentDialogBase.details()
                .row(Bundle.message("caption.name"), dto.getName())
                .row(Bundle.message("caption.path"), dto.getPath().toString())
                .row(TestEditorAttributes.CREATED_BY.getName(), marker.getCreatedBy())
                .row(TestEditorAttributes.CREATED_AT.getName(), Display.formatDate(marker.getCreatedAt()))
                .row(TestEditorAttributes.UPDATED_BY.getName(), marker.getModifiedBy())
                .row(TestEditorAttributes.UPDATED_AT.getName(), Display.formatDate(marker.getModifiedAt()))
                .row(TestEditorAttributes.STATUS.getName(), marker.getStatusLabel());

        // Whatever the marker has to say about itself - for a test run, when it
        // was executed and what the tester answered when it was created, which
        // the marker holds and answers for since those facts moved into it
        // (#305, S15). Added without asking what kind of marker this is, the same
        // way the status row is: a marker with nothing to add returns nothing,
        // and a blank value is dropped.
        marker.getDetailRows().forEach(extra -> details.row(extra.caption(), extra.value()));

        type.getCounts().forEach(count -> details.row(count.getCaption(), count.of(figures)));

        components = List.of(
                details.build(),
                ComponentDialogBase.of(new VerdictDonut(type.getStatistics().getSlices(), figures)));

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, Bundle.message("shortcut.close"), this::closeCancel));

        // Sized to its rows rather than fixed, and still movable and resizable.
        // A fixed 600 by 500 fit one-line rows; with each caption above its value
        // (#328) a test run's rows and chart needed more, and the chart was
        // squeezed out below them.
        resizable = true;
    }

    @Override
    protected void submit() {
        closeOk();
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-075. Shows one node. Asked about another, the newer node is the one wanted.
     */
    @Override
    protected boolean replacesItsKind() {
        return true;
    }
}
