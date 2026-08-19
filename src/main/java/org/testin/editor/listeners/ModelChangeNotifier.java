package org.testin.editor.listeners;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.UpdateCallback;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.Optional;

/**
 * Tells the editor that its list model changed, so it can re-sort and redraw.
 * <p>
 * It reports the change and nothing more; it does not mirror it into the
 * editor's master list, and must not. The model holds one page of the
 * <em>filtered</em> list, so the page indices it reports do not address the
 * unfiltered master: with a search active the two do not line up, and a
 * difference between them is not a change at all - it is whatever the filter
 * hid. Acting on that removed live test cases from the master list, and the
 * sequence write that followed persisted the loss. Clearing the model to show
 * "Refreshing..." looked exactly like every case on the page having been
 * deleted.
 * <p>
 * Nothing needs it to sync: every path that mutates the model - the deletion
 * action, cut and paste, drag and drop, the page reload - maintains the master
 * list itself.
 * <p>
 * {@link #pause()} around a wholesale repopulation, so rebuilding the page does
 * not read as an edit by the tester.
 */
public class ModelChangeNotifier implements ListDataListener {

    private boolean active = true;

    @Setter
    private @Nullable UpdateCallback onUpdateCallback;

    public void pause() {
        this.active = false;
    }

    public void resume() {
        this.active = true;
    }

    @Override
    public void intervalAdded(final ListDataEvent e) {
        notifyChanged();
    }

    @Override
    public void intervalRemoved(final ListDataEvent e) {
        notifyChanged();
    }

    @Override
    public void contentsChanged(final ListDataEvent e) {
    }

    private void notifyChanged() {
        if (!active) return;

        Optional.ofNullable(onUpdateCallback)
                .ifPresent(cb -> ApplicationManager.getApplication().invokeLater(cb::onUpdate));
    }
}
