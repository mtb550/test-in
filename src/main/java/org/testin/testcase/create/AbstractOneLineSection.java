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

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;

/**
 * A section that is one line of text: the field, the row it sits in, the key
 * that reveals it, and what focusing and disabling it mean.
 * <p>
 * The one-line counterpart of {@link AbstractMultiLineSection}, and written for
 * the same reason (#176). Three sections repeated these four answers word for
 * word - module, pre-conditions and the description - so a change to how a row
 * is focused or greyed had three places to be made, and the field each of them
 * held was styled by a line each of them remembered to write.
 * <p>
 * What a subclass says is what actually differs: which field (a plain one, or
 * one that completes what other cases have used), which caption and icon
 * describe it, which key reveals it, and which value of a test case it is.
 */
public abstract class AbstractOneLineSection implements CreateTestCaseSection {

    protected final @NotNull EditorTextField field;

    private final @NotNull JBPanel<?> wrapper;
    private final @NotNull Shortcuts shortcut;

    protected AbstractOneLineSection(final @NotNull EditorTextField field, final @NotNull CreateTestCaseFields describes, final @NotNull Shortcuts shortcut) {
        this.field = field;
        this.field.setOneLineMode(true);
        this.shortcut = shortcut;

        styleField(this.field, describes);

        this.wrapper = createWrapper(describes.getIcon(), this.field);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    /**
     * UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-028.
     * <p>
     * Advertised as well as bound - the field is in the dialog's jump map. The
     * key was taken off one of these once because it was not, which left a field
     * drawn in a dialog with no way at all to reach it.
     */
    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, shortcut.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return field;
    }

    @Override
    public void setEditable(final boolean editable) {
        field.setEnabled(editable);
    }
}
