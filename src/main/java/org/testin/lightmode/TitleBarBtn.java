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

package org.testin.lightmode;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractIconButton;

import javax.swing.Icon;

/**
 * A button in light mode's own title bar, drawn the way every other button in
 * this plugin is drawn.
 * <p>
 * One class for all three of them - start, stop and the pin - for the reason
 * {@code PageBtn} gives: the toolbar's one-class-per-button exists so
 * {@code getToolbarItem} can find a button by type, and nothing looks these up.
 */
class TitleBarBtn extends AbstractIconButton {

    TitleBarBtn(final @NotNull String tooltip, final @NotNull Icon icon) {
        super(tooltip, icon);
    }
}
