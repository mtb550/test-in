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
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpenDialogKeys {

    public static boolean isOpen(final @NotNull Project p, final @NotNull Class<? extends AbstractFrameworkDialog> kind) {
        return Services.getInstance(p, OpenDialogs.class).shown(kind).isPresent();
    }

    public static @NotNull JBPopup popupOf(final @NotNull Project p, final @NotNull Class<? extends AbstractFrameworkDialog> kind) {
        return Services.getInstance(p, OpenDialogs.class).shown(kind).orElseThrow(() -> new AssertionError("no " + kind.getSimpleName() + " is open"));
    }

    public static void press(final @NotNull Project p, final @NotNull Class<? extends AbstractFrameworkDialog> kind, final @NotNull Shortcuts key) {
        final @NotNull JComponent content = popupOf(p, kind).getContent();
        UIUtil.uiTraverser(content).filter(JComponent.class).toList().stream().map(component -> bindingOf(component, key)).flatMap(Optional::stream).findFirst()
                .orElseThrow(() -> new AssertionError("nothing in " + kind.getSimpleName() + " answers " + key.getShortcutText()))
                .actionPerformed(new ActionEvent(content, ActionEvent.ACTION_PERFORMED, key.name()));
    }

    private static @NotNull Optional<Action> bindingOf(final @NotNull JComponent component, final @NotNull Shortcuts key) {
        return Optional.ofNullable(component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(key.getKey())).map(name -> component.getActionMap().get(name));
    }
}
