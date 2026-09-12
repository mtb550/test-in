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

package org.testin.model;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Something that can be a row on a shortcut menu: it has a name, an icon, a
 * shortcut to print, and knows how to bind that shortcut itself.
 * <p>
 * Every implementor is an enum, and every one of them already answered all four
 * questions - {@code ShortcutMenuPopup} was simply handed the four answers as
 * separate method references, five functional arguments on an eight-argument
 * constructor that nobody could read at a glance (#175, C12).
 * <p>
 * Naming the thing rather than passing its parts also puts the binding where the
 * constant is. A menu row's shortcut belongs to the row, not to the popup that
 * happens to be showing it, which is why {@code bindShortcut} is declared here
 * and why the {@code ItemShortcutBinder} that used to carry it across is gone.
 */
public interface MenuItem extends StatusBarItem {

    @NotNull Icon getIcon();

    /**
     * Makes this item's own key select it, on whatever component is showing the
     * menu. A constant with no shortcut binds nothing.
     */
    void bindShortcut(@NotNull JComponent component, @NotNull Runnable onTrigger);
}
