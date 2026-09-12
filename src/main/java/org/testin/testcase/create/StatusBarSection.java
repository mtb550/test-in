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

package org.testin.testcase.create;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestCaseDialogKey;
import org.testin.ui.framework.StatusBarBase;

import java.util.ArrayList;
import java.util.List;

/**
 * UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-199, Rule-EDITOR-PANEL-200.
 * <p>
 * The hint strip at the bottom of a test case dialog: the keys of the field the
 * tester is in, then the two that mean the same thing everywhere.
 * <p>
 * <b>One strip.</b> It was two - a row that changed with the focus above a row
 * that never changed - so that tabbing from Description to Steps did not redraw
 * Save and Cancel with it (#56). What that bought was not worth what it cost:
 * every test case dialog was two tinted rows tall, and two of them stacked read
 * as two bars rather than as one hint area however carefully the second one
 * dropped its icon. The redraw it was avoiding is six labels on a focus change,
 * which nobody can see.
 * <p>
 * A field with no keys of its own still shows the strip, because Save and
 * Cancel are always on it. There is no empty row to hide any more, which is the
 * other thing two strips made possible and nothing wanted.
 */
public final class StatusBarSection {

    /**
     * The two keys every section shares, in the order a tester reads them, and
     * always last - a tester looks for Save in the same place whichever field
     * they are in.
     */
    private static final StatusBarItem @NotNull [] SHARED =
            {TestCaseDialogKey.SAVE, TestCaseDialogKey.CANCEL};

    private final @NotNull StatusBarBase bar = new StatusBarBase(SHARED);

    public StatusBarSection() {
        // The dialog opens on the description, so its keys are what the strip
        // starts on - the same section the focus listener would report first.
        updateItems(CreateTestCaseFields.DESCRIPTION.getStatusBarItems());
    }

    /**
     * Rule-EDITOR-PANEL-199.
     * <p>
     * The focused section's own keys, then the shared two.
     */
    public void updateItems(final StatusBarItem @NotNull [] items) {
        final @NotNull List<StatusBarItem> all = new ArrayList<>(List.of(items));
        all.addAll(List.of(SHARED));

        bar.updateItems(all.toArray(StatusBarItem[]::new));
    }

    public @NotNull JBPanel<?> getPanel() {
        return bar.getPanel();
    }
}
