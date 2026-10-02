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
import com.intellij.ui.popup.AbstractPopup;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;

import java.awt.event.KeyEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PressEscape {
    public static void on(final @NotNull Project p, final @NotNull Class<? extends AbstractFrameworkDialog> kind) {
        final @NotNull AbstractPopup popup = (AbstractPopup) Services.getInstance(p, OpenDialogs.class).shown(kind)
                .orElseThrow(() -> new AssertionError("no " + kind.getSimpleName() + " is open"));

        popup.dispatchKeyEvent(new KeyEvent(popup.getContent(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
    }
}
