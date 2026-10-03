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
import com.intellij.openapi.ui.popup.JBPopup;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;

import javax.swing.JComponent;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShownDialogContent {
    public static @NotNull Optional<JComponent> of(final @NotNull Project p, final @NotNull Class<? extends AbstractFrameworkDialog> kind) {
        return Services.getInstance(p, OpenDialogs.class).shown(kind).map(JBPopup::getContent);
    }

    public static void close(final @NotNull Project p, final @NotNull Class<? extends AbstractFrameworkDialog> kind) {
        Services.getInstance(p, OpenDialogs.class).shown(kind).ifPresent(JBPopup::cancel);
    }
}
