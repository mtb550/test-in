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
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OnScreenDialog {

    public static boolean isOpen(final @NotNull Project p, final @NotNull Class<?> kind) {
        return openDialogs(p).shown(kind).isPresent();
    }

    public static @NotNull JComponent content(final @NotNull Project p, final @NotNull Class<?> kind) {
        return popup(p, kind).getContent();
    }

    public static void typed(final @NotNull Project p, final @NotNull Class<?> kind, final @NotNull String text) {
        final @NotNull JTextComponent field = Optional.ofNullable(UIUtil.uiTraverser(content(p, kind)).filter(JTextComponent.class).filter(JTextComponent::isEditable).first())
                .orElseThrow(() -> new AssertionError("no " + kind.getSimpleName() + " has a field to type in"));
        field.setText(text);
    }

    public static void pressed(final @NotNull Project p, final @NotNull Class<?> kind, final @NotNull Shortcuts key) {
        final @NotNull JComponent content = content(p, kind);
        final @NotNull KeyStroke stroke = key.getKey();

        final @NotNull List<JComponent> everyComponent = UIUtil.uiTraverser(content).filter(JComponent.class).toList();
        everyComponent.stream()
                .map(component -> bindingOf(component, stroke))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new AssertionError("nothing in " + kind.getSimpleName() + " answers " + stroke))
                .actionPerformed(new ActionEvent(content, ActionEvent.ACTION_PERFORMED, String.valueOf(stroke)));
    }

    public static void sized(final @NotNull Project p, final @NotNull Class<?> kind) {
        popup(p, kind).setSize(new Dimension(800, 600));
    }

    public static void closed(final @NotNull Project p, final @NotNull Class<?> kind) {
        openDialogs(p).shown(kind).ifPresent(Disposer::dispose);
    }

    private static @NotNull JBPopup popup(final @NotNull Project p, final @NotNull Class<?> kind) {
        return openDialogs(p).shown(kind).orElseThrow(() -> new AssertionError("no " + kind.getSimpleName() + " is open"));
    }

    private static @NotNull Optional<Action> bindingOf(final @NotNull JComponent component, final @NotNull KeyStroke stroke) {
        return boundIn(component, stroke, JComponent.WHEN_FOCUSED).or(() -> boundIn(component, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
    }

    private static @NotNull Optional<Action> boundIn(final @NotNull JComponent component, final @NotNull KeyStroke stroke, @MagicConstant(intValues = {JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT}) final int condition) {
        return Optional.ofNullable(component.getInputMap(condition).get(stroke)).map(name -> component.getActionMap().get(name));
    }

    private static @NotNull OpenDialogs openDialogs(final @NotNull Project p) {
        return Services.getInstance(p, OpenDialogs.class);
    }
}
