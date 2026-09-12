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

package org.testin.editor;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Shared colors for list and grid selection states.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditorColors {
    public static final @NotNull Color SELECTION_BACKGROUND = new JBColor(
            new Color(214, 230, 250),
            new Color(37, 55, 76)
    );
    public static final @NotNull Color SELECTION_BORDER = JBColor.blue;

    /**
     * One color for one fact: a filter is on and the tester is not seeing
     * everything.
     * <p>
     * It marks the filter button's count and the status bar's own "(filtered
     * from 120)" in the same breath, because a tester who has stopped noticing
     * one has stopped noticing the other - and a second color for the same fact
     * would say they were two.
     */
    public static final @NotNull Color FILTER_ACTIVE = JBUI.CurrentTheme.Link.Foreground.ENABLED;
}
