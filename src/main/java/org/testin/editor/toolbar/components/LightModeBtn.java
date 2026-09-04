package org.testin.editor.toolbar.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractIconButton;
import org.testin.editor.run.RunEditor;
import org.testin.lightmode.LightMode;
import org.testin.services.Services;

/**
 * Opens light mode, and says whether it is open (#13).
 * <p>
 * A toggle rather than an opener: pressed once it shows the window, pressed
 * again it closes it and the tester carries on in the editor. It stays pressed
 * for exactly as long as the window exists, so a glance at the toolbar answers
 * which mode you are in.
 * <p>
 * <b>Pressed is asked, not remembered.</b> {@link #updateState} asks whether
 * the window is showing <i>this</i> run, so the window closing by any route -
 * Escape, its own close button, another run taking it over, the project
 * shutting - un-presses this without anybody writing a third handler.
 * <p>
 * Enabled on the same question that enables Start: a run that has been signed
 * off has nothing left to record, so there is nothing to open light mode for.
 */
public class LightModeBtn extends AbstractIconButton implements ToolbarItem {

    private final @NotNull Project p;
    private final @NotNull RunEditor editor;

    public LightModeBtn(final @NotNull Project p, final @NotNull RunEditor editor) {
        super("Light Mode - one test case, above other windows", AllIcons.Actions.MoveToWindow);
        this.p = p;
        this.editor = editor;

        addActionListener(e -> Services.getInstance(p, LightMode.class).toggle(editor.getParent(), this::updateState));
    }

    public void updateState() {
        setEnabled(editor.getParent().isStillOpen());
        setOn(Services.getInstance(p, LightMode.class).isOpenOn(editor.getParent()));
    }
}
