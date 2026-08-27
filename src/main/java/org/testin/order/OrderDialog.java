package org.testin.order;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.Marker;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;

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

    OrderDialog(final @NotNull Project p, final int current, final @NotNull IntConsumer onSubmit) {
        super(p);
        this.onSubmit = onSubmit;

        title = "Order";

        components = List.of(
                ComponentDialogBase.textField()
                        .icon(AllIcons.Actions.Edit)
                        .placeholder("1, 2, 3... or empty for date order")
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

    @Override
    protected void submit() {
        onSubmit.accept(typed(component().getText().trim()));
        closeOk();
    }

    /**
     * What the tester typed, as a number. Empty takes the number off again; so
     * does a value too large to be an {@code int}, which the field's digits let
     * through and which means nothing as a position anyway.
     */
    private static int typed(final @NotNull String text) {
        try {
            return Integer.parseInt(text);
        } catch (final NumberFormatException ex) {
            return Marker.NOT_ORDERED;
        }
    }
}
