package org.testin.editor.toolbar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckBoxList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.ToolBarAttribute;
import org.testin.ui.dialogs.DialogStyle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Toolbar button that shows a persistent check-box popup of editor detail attributes.
 * The concrete subclasses only supply the enum options and the persistence key.
 */
public abstract class AbstractDetailsPopupBtn<E extends Enum<E> & ToolBarAttribute> extends AbstractButton implements ToolbarItem {

    @Getter
    private final @NotNull Set<E> selectedDetails = new HashSet<>();

    private final @NotNull String propertyKey;
    private final @NotNull List<E> options;

    protected AbstractDetailsPopupBtn(final @NotNull String propertyKey,
                                      final @NotNull Class<E> attributes,
                                      final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        // A checked box, matching the check-box list this button opens. The
        // previous icon was a framed panel with rules in it, which the New UI
        // draws almost identically to the List View button beside it.
        super("Details", AllIcons.Actions.Selectall);

        this.propertyKey = propertyKey;
        this.options = List.of(attributes.getEnumConstants());

        // Only when nothing is stored yet: the attributes the enum flags on, not
        // every option. A saved selection is honored exactly as it was saved.
        final String defaults = options.stream()
                .filter(o -> o.getToolBarDefault().isSelectedByDefault())
                .map(Enum::name)
                .collect(Collectors.joining(","));
        final String saved = PropertiesComponent.getInstance().getValue(propertyKey, defaults);

        for (final String s : saved.split(",")) {
            if (s.isEmpty()) continue;
            try {
                selectedDetails.add(Enum.valueOf(attributes, s));
            } catch (final IllegalArgumentException ex) {
                Logger.error("Invalid editor attribute '" + s + "' for " + propertyKey + ": " + ex.getMessage());
            }
        }

        // The locked attributes hold the state they declare, whatever was stored.
        options.forEach(o -> o.getToolBarDefault().enforceLock(o, selectedDetails));

        addActionListener(e -> showDetailsPopup(onToolBarDetailsSelectedChanged));
    }

    private void saveProps() {
        final String joinedNames = selectedDetails.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));

        PropertiesComponent.getInstance().setValue(propertyKey, joinedNames);
    }

    private void showDetailsPopup(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        final CheckBoxList<E> detailsList = new CheckBoxList<>() {
            /**
             * Grays the locked attributes out, and stops the click and the space
             * key from toggling them - the platform asks this before both.
             */
            @Override
            protected boolean isEnabled(final int index) {
                final E item = getItemAt(index);
                return item == null || item.getToolBarDefault().isSwitchable();
            }
        };
        DialogStyle.styleContent(detailsList);

        options.forEach(attr -> detailsList.addItem(attr, attr.getName(), selectedDetails.contains(attr)));

        detailsList.setCheckBoxListListener((index, state) -> {
            final E item = detailsList.getItemAt(index);
            if (item != null) {
                if (state) selectedDetails.add(item);
                else selectedDetails.remove(item);
            }

            saveProps();

            onToolBarDetailsSelectedChanged.run();
        });

        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(detailsList, detailsList)
                .setRequestFocus(true)
                .createPopup()
                .showUnderneathOf(this);
    }
}
