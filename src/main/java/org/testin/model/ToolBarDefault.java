package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Whether an editor attribute starts checked in the Details popup, and whether
 * the tester may switch it.
 * <p>
 * One field per attribute rather than two flags at the call site: the constant
 * says what it wants and every surface - the popup, the card title, the grid
 * columns - follows it without knowing which attribute it is holding.
 */
@Getter
@AllArgsConstructor
public enum ToolBarDefault {
    /**
     * Unchecked until the tester ticks it.
     */
    OFF(false, true),

    /**
     * Checked on a profile that has never stored a selection, and free to be
     * unticked afterwards.
     */
    ON(true, true),

    /**
     * Always checked, and grayed out so it cannot be unticked. For what the row
     * is made of rather than what it may also show - the order number and the
     * description are the card title, and a row without them cannot be told
     * apart from the next one.
     */
    LOCKED_CHECKED(true, false),

    /**
     * Never checked, and grayed out so it cannot be ticked. For an attribute
     * that belongs to the model but not to this view - it stays in the popup,
     * visibly unavailable, rather than disappearing and leaving the tester
     * looking for it.
     */
    LOCKED_UNCHECKED(false, false);

    /**
     * What a profile with nothing stored starts with.
     */
    private final boolean selectedByDefault;

    /**
     * Whether the tester may change it. A locked attribute is drawn grayed in
     * the popup, and the platform refuses both the click and the space key.
     */
    private final boolean switchable;

    /**
     * Puts a locked attribute into the state it declares, whatever the stored
     * selection says, and leaves a switchable one exactly as the tester left it.
     * <p>
     * Called for every attribute on load, so a selection stored before the
     * attribute existed - or one hand-edited in the properties file - can never
     * leave the popup disagreeing with what the card and the grid draw.
     */
    public <E extends ToolBarAttribute> void enforceLock(final @NotNull E option, final @NotNull Set<E> selected) {
        if (switchable) return;

        if (selectedByDefault) selected.add(option);
        else selected.remove(option);
    }
}
