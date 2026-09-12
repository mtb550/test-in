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

package org.testin.ui.dialogs;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.MenuItem;

import javax.swing.*;

/**
 * Row renderer for {@link ShortcutMenuPopup}: icon, name, grayed shortcut text.
 */
final class ShortcutMenuRenderer<T extends MenuItem> extends ColoredListCellRenderer<T> {

    @Override
    protected void customizeCellRenderer(final @NotNull JList<? extends T> list, final T value, final int index, final boolean selected, final boolean hasFocus) {
        setIcon(value.getIcon());
        append(value.getName());
        append("   " + value.getShortcutText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        setBorder(JBUI.Borders.empty(6, 12));
    }
}
