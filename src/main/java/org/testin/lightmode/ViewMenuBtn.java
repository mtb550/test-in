package org.testin.lightmode;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.components.AbstractDetailsPopupBtn;

/**
 * The light mode window's view menu: five checkboxes for what it shows (#13).
 * <p>
 * The same button the two editors put on their toolbars, which is what its own
 * javadoc anticipated - "a third editor needs no new plumbing". The popup, the
 * check-box list, the per-machine persistence and the debounced redraw all come
 * from there; what is here is the enum and the key.
 */
class ViewMenuBtn extends AbstractDetailsPopupBtn<LightModePart> {

    ViewMenuBtn(final @NotNull Runnable onChanged) {
        // v1, and the same versioning rule the editors follow: bump the key only
        // when a stored selection would be answering an older question, because
        // bumping discards what every tester ticked.
        super("Choose what the window shows",
                "testin.lightMode.parts.v1",
                LightModePart.class,
                onChanged);
    }
}
