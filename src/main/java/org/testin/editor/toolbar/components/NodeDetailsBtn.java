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

package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.Toolbar;
import org.testin.util.Bundle;
import org.testin.view.marker.MarkerDetailsViewDialog;

/**
 * Opens the Details popup for the node the editor is showing - the same popup
 * the tree's own Details action opens, on the same node.
 * <p>
 * One button for both editors. It works for either because it never asks which
 * kind it is looking at: it asks the editor for its node, and the popup reads
 * the {@link org.testin.model.markers.Marker} contract that every node's marker
 * implements. So a test set shows its status and counts and a test run shows the
 * configuration it was created with, from the same click.
 * <p>
 * The dialog is opened here rather than handed back to the editor as a callback.
 * There is nothing editor-specific to do, and a callback would have been the
 * same two lines written twice.
 * <p>
 * Not to be confused with {@link RunDetailsPopupBtn} and
 * {@link TestDetailsPopupBtn}, which despite the name choose which columns the
 * grid shows.
 */
public class NodeDetailsBtn extends AbstractIconButton implements ToolbarItem {

    // UC-EDITOR-PANEL-028, Rule-EDITOR-PANEL-121
    public NodeDetailsBtn(final @NotNull Toolbar editor) {
        // The icon the tree's Details action already uses, so one command does
        // not look like two things depending on where it is reached from.
        super(Bundle.message("toolbar.node.details"), AllIcons.General.IndentDetected);

        addActionListener(e -> new MarkerDetailsViewDialog(editor.getProject(), editor.getEditedNode()).show());
    }
}
