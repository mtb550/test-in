package org.testin.statusbar;

/**
 * Status bar used by the framework's dialogs.
 * <p>
 * It opens empty. It used to be built with Confirm and Cancel hard-coded, from
 * the days when every dialog had exactly those two keys - and the framework
 * overwrote them with the dialog's own declaration before it was ever painted.
 * A default nothing can see is not a default; the keys come from the dialog
 * that declared them (#66).
 */
public final class DialogStatusBar extends StatusBarBase {

    public DialogStatusBar() {
        super(new StatusBarItem[0]);
    }
}
