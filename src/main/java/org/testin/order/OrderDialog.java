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

package org.testin.order;

import org.testin.services.Services;
import org.testin.notifications.Notifier;
import java.util.OptionalInt;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.Marker;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Asks for the number a node sits at among its siblings.
 * <p>
 * One field, because that is the whole decision. Empty means no number, and a
 * node with no number follows the numbered ones by the date it was created -
 * which is how every folder reads before anyone types anything.
 * <p>
 * The field takes 1 and up, never 0. Zero is what the marker holds when nobody
 * has said, so a tester who typed it would be asking for a position and getting
 * "no position" - a rule the field enforces rather than a surprise it explains.
 */
final class OrderDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull IntConsumer onSubmit;

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-055
    OrderDialog(final @NotNull Project p, final int current, final @NotNull IntConsumer onSubmit) {
        super(p);
        this.onSubmit = onSubmit;

        title = Bundle.message("dialog.order.title");

        components = List.of(
                ComponentDialogBase.textField()
                        .icon(AllIcons.Actions.Edit)
                        .placeholder(Bundle.message("dialog.order.placeholder"))
                        .value(shown(current))
                        .accepting("[1-9][0-9]*")
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));
    }

    /**
     * The field's text for a node's number: empty for a node nobody numbered.
     * <p>
     * The one place the two meet, so nothing else in the plugin has to know that
     * a very large number means "none" - and the same place turns an empty field
     * back into it on the way out.
     */
    private static @NotNull String shown(final int order) {
        return order == Marker.NOT_ORDERED ? "" : String.valueOf(order);
    }

    /**
     * UC-TREE-PANEL-015, Rule-TREE-PANEL-055.
     * <p>
     * Refuses a number too large and stays open, rather than writing something
     * the tester did not type.
     * <p>
     * A value that will not fit used to come back as "no position at all", so
     * typing a long number took the node's position off, dropped it back into
     * date order, and confirmed with Ordered. Three wrong answers to one typo,
     * and the only one the tester saw was the one saying it worked (#193).
     */
    @Override
    protected void submit() {
        final @NotNull String text = component().getText().trim();
        final @NotNull OptionalInt number = typed(text);

        if (number.isEmpty()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("dialog.order.refused.title"),
                    // The int, so MessageFormat groups it: 2,147,483,647 rather
                    // than ten undivided digits, and grouped the way the reader's
                    // own locale groups numbers. It was passed as a string while
                    // the sentence moved into the bundle, because a translation
                    // commit must change nothing a tester sees (#66, finding 97).
                    Bundle.message("dialog.order.refused.message", Marker.NOT_ORDERED));
            return;
        }

        onSubmit.accept(number.getAsInt());
        closeOk();
    }

    /**
     * What the tester typed, as a number. Empty takes the number off again,
     * which is the one way a position is meant to be cleared. Anything the field
     * let through that is not a position - a number too large to hold, and the
     * sentinel that means "none" - is nothing, and the caller refuses it.
     */
    private static @NotNull OptionalInt typed(final @NotNull String text) {
        if (text.isEmpty()) return OptionalInt.of(Marker.NOT_ORDERED);

        try {
            final int number = Integer.parseInt(text);

            return number < Marker.NOT_ORDERED ? OptionalInt.of(number) : OptionalInt.empty();
        } catch (final NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }
}
