package org.testin.lightmode;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.components.AbstractDetailsPopupBtn;

/**
 * The light mode window's view menu: four checkboxes for what it shows (#13).
 * <p>
 * The same button the two editors put on their toolbars, which is what its own
 * Javadoc anticipated - "a third editor needs no new plumbing". The popup, the
 * check-box list, the per-machine persistence and the debounced redraw all come
 * from there; what is here is the enum and the key.
 */
class ViewMenuBtn extends AbstractDetailsPopupBtn<LightModePart> {

    ViewMenuBtn(final @NotNull Runnable onChanged) {
        // v2: the expected result left the list, so a v1 selection holds an
        // option that no longer exists, and reading it back would log an
        // unknown-attribute error on every open. The versioning rule is
        // AbstractDetailsPopupBtn's; what is local is that it had to be used.
        super("Choose what the window shows",
                "testin.lightMode.parts.v2",
                LightModePart.class,
                onChanged);
    }
}
