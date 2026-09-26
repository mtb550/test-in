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

package org.testin.ui.framework;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.ScreenUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Optional;

public record DialogSize(double heightPart) {
    public static final @NotNull DialogSize CONTENT = new DialogSize(0);
    public static final @NotNull DialogSize SHORT = new DialogSize(0.50);
    public static final @NotNull DialogSize HALF = new DialogSize(0.60);
    public static final @NotNull DialogSize TALL = new DialogSize(0.70);

    private static final double WIDTH = 0.60;
    private static final int MARGIN = 32;

    private static int clamped(final int least, final int wanted, final int most) {
        return Math.min(most, Math.max(least, wanted));
    }

    // Rule-INTERNAL-100
    static @NotNull Rectangle frameOn(final @NotNull Project p) {
        return Optional.ofNullable(WindowManager.getInstance().getIdeFrame(p))
                .map(IdeFrame::getComponent)
                .filter(Component::isShowing)
                .map(frame -> new Rectangle(frame.getLocationOnScreen(), frame.getSize()))
                .filter(frame -> frame.width > 0 && frame.height > 0)
                .orElseGet(ScreenUtil::getMainScreenBounds);
    }

    // Rule-INTERNAL-100
    static int widthOn(final @NotNull Project p, final int natural) {
        final @NotNull Rectangle frame = frameOn(p);

        return clamped(natural, (int) (frame.width * WIDTH), frame.width - JBUI.scale(MARGIN));
    }

    // Rule-INTERNAL-102
    static int withinFrame(final @NotNull Project p, final int wanted) {
        return Math.min(wanted, frameOn(p).height - JBUI.scale(MARGIN));
    }

    // Rule-INTERNAL-100
    boolean namesAHeight() {
        return heightPart > 0;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-100
    void applyTo(final @NotNull Project p, final @NotNull JComponent content) {
        if (!namesAHeight()) return;

        final @NotNull Rectangle frame = frameOn(p);
        final @NotNull Dimension natural = content.getPreferredSize();

        content.setPreferredSize(new Dimension(
                widthOn(p, natural.width),
                clamped(natural.height, (int) (frame.height * heightPart), frame.height - JBUI.scale(MARGIN))));
    }
}
