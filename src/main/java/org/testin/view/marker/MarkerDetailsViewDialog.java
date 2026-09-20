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

        marker.getDetailRows().forEach(extra -> details.row(extra.caption(), extra.value()));

        type.getCounts().forEach(count -> details.row(count.getCaption(), count.of(figures)));

        components = List.of(
                details.build(),
                ComponentDialogBase.of(new VerdictDonut(type.getStatistics().getSlices(), figures)));

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, Bundle.message("shortcut.close"), this::closeCancel));

        resizable = true;
    }

    @Override
    protected void submit() {
        closeOk();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-075
    @Override
    protected boolean replacesItsKind() {
        return true;
    }
}
