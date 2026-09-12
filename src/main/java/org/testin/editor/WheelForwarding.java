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

import com.intellij.ui.components.JBScrollPane;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.MouseWheelEvent;
import java.util.Optional;

/**
 * Lets a component that does not scroll pass the wheel on to the one that does.
 * <p>
 * Split out of editor/Shared (#113). Its own class because it is neither a
 * badge nor a card measurement - it is one Swing gesture, and the grid and the
 * card listener both need it to behave the same way.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WheelForwarding {
    /**
     * UC-SETTING-011, Rule-SETTING-039.
     * <p>
     * Hands a wheel event to the enclosing scroll pane, so a component that does
     * not scroll itself does not swallow the gesture. Ctrl/Meta is left alone -
     * that is the font zoom, not a scroll.
     */
    public static void forwardWheelToScrollPane(final @NotNull MouseWheelEvent e) {
        if (e.isControlDown() || e.isMetaDown())
            return;

        findScrollPane(e.getComponent())
                .filter(scrollPane -> e.getComponent() != scrollPane)
                .ifPresent(scrollPane -> {
                    final @NotNull MouseWheelEvent clonedEvent = (MouseWheelEvent) SwingUtilities.convertMouseEvent(e.getComponent(), e, scrollPane);
                    scrollPane.dispatchEvent(clonedEvent);
                    e.consume();
                });
    }

    /**
     * The scroll pane this component sits in, walking up until Swing runs out
     * of parents - which is where the null comes from and where it stops.
     */
    private static @NotNull Optional<JBScrollPane> findScrollPane(final @NotNull Component component) {
        return Optional.ofNullable((JBScrollPane) SwingUtilities.getAncestorOfClass(JBScrollPane.class, component));
    }
}
