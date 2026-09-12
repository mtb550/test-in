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

import com.intellij.ui.components.JBPanel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.awt.*;
import java.util.Optional;
import javax.swing.*;

/**
 * The editor's swappable middle: the panel, and whichever component is in it.
 * <p>
 * Both editors switch between a list and a grid, and both tracked the component
 * currently in the center with a field of their own so they could take it out
 * again. Forgetting the removal leaves two views stacked in one BorderLayout
 * slot, which is why this holds the pair together rather than leaving the field
 * beside the panel and hoping.
 */
@RequiredArgsConstructor
public final class EditorCenter {

    private final @NotNull JBPanel<?> panel;

    /**
     * Whatever is in the center, and nothing before the first {@link #set}.
     */
    private @NotNull Optional<JComponent> current = Optional.empty();

    /**
     * Puts this component in the center, taking out whatever was there.
     */
    public void set(final @NotNull JComponent component) {
        Logger.debug("[center] setCenter -> " + component.getClass().getSimpleName()
                + " (had center=" + current.isPresent() + ")");

        current.ifPresent(panel::remove);

        panel.add(component, BorderLayout.CENTER);
        current = Optional.of(component);

        panel.revalidate();
        panel.repaint();
    }
}
