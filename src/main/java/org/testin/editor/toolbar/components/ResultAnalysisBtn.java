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
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunStatus;
import org.testin.util.Bundle;

/**
 * Writes what the run means: a paragraph per verdict, printed in the reports
 * under the counts.
 * <p>
 * Shown always and enabled only once the run is completed. Disabled rather than
 * hidden, because a button that appears when a run finishes is a button the
 * tester has to notice; one that is there from the start, greyed, says the work
 * exists and when it can be done - and the tooltip says why it cannot yet.
 * <p>
 * Completed and not merely terminal: a closed run is finished with, and writing
 * an analysis into it is describing a run nobody will act on.
 */
public class ResultAnalysisBtn extends AbstractIconButton implements ToolbarItem {

    private final @NotNull RunEditor editor;

    // UC-EDITOR-PANEL-045
    public ResultAnalysisBtn(final @NotNull RunEditor editor, final @NotNull Runnable onResultAnalysisClicked) {
        // The platform's own analysis icon. The one this was asked for -
        // ExceptionAnalyzerIcons expui/exceptionAnalyzer - ships with the
        // ExceptionAnalyzer plugin rather than the platform, so naming it would
        // make Testin refuse to load without that plugin installed.
        //
        // Off rather than On: the two differ only in color, and the On variant is
        // green. Green on a toolbar reads as something being switched on, and
        // this is a button that opens a dialog.
        super(Bundle.message("toolbar.analysis"), AllIcons.Actions.ProjectWideAnalysisOff);
        this.editor = editor;

        addActionListener(e -> onResultAnalysisClicked.run());
        updateEnabledState();
    }

    // UC-EDITOR-PANEL-045, Rule-EDITOR-PANEL-189
    public void updateEnabledState() {
        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();
        final boolean completed = status == TestRunStatus.COMPLETED;

        setEnabled(completed);
        setToolTipText(completed
                ? Bundle.message("toolbar.analysis")
                : Bundle.message("toolbar.analysis.disabled", status.getLabel()));
    }
}
