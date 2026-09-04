package org.testin.lightmode;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.DimensionService;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

/**
 * The light mode window: one test case, above everything else (#13).
 * <p>
 * A real top-level frame rather than a tool window, which is the whole
 * requirement - a tool window belongs to the IDE frame and is hidden with it, and
 * staying visible while IntelliJ is minimized is the reason this exists at all.
 * <p>
 * Undecorated, so the title bar is ours: the pin, the run name and what the
 * design puts beside them are part of the window rather than sitting under an
 * operating system bar that says the same things differently on two platforms.
 * The price is that dragging is ours too, which is {@link #dragBy}.
 * <p>
 * Where it sits is the platform's problem, not this class's.
 * {@link DimensionService} stores a location against a key, so the window comes
 * back where the tester left it. Deliberately the overload without a project:
 * that argument only picks a project frame to normalize the coordinates against,
 * and this window belongs to the screen rather than to a project - which also
 * means closing it while the project shuts down asks nothing of the project.
 * <p>
 * Height is never stored: it is whatever the content needs, so the window is
 * packed rather than sized.
 */
final class LightModeWindow {

    /**
     * One key for the placement. Versioned, so a later layout change that makes
     * an old position wrong can start again rather than restoring a window
     * somewhere that no longer makes sense.
     */
    private static final @NotNull String PLACEMENT = "testin.lightMode.v1";

    private final @NotNull JFrame frame = new JFrame();
    private final @NotNull TestRunDirectoryDto run;
    private final @NotNull Runnable onClosed;

    /**
     * Where in the title bar the drag started, and empty whenever no drag is
     * under way.
     */
    private @NotNull Optional<Point> dragOrigin = Optional.empty();

    LightModeWindow(final @NotNull TestRunDirectoryDto run, final @NotNull Runnable onClosed) {
        this.run = run;
        this.onClosed = onClosed;

        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setContentPane(content());
        frame.pack();

        placeIt();
        closeOnEveryRoute();

        frame.setVisible(true);
    }

    /**
     * By path, which is how the indexer identifies a run everywhere else - the
     * editor and this window are handed the same cached object today, and this
     * does not quietly depend on that staying true.
     */
    boolean shows(final @NotNull TestRunDirectoryDto other) {
        return run.getPath().equals(other.getPath());
    }

    /**
     * Closes, and says so - the toolbar button is drawn from whether this window
     * exists, so every route out of it comes through here.
     */
    void close() {
        closeQuietly();

        onClosed.run();
    }

    /**
     * Closes without saying so, for the project shutting down: the position is
     * still worth keeping, but there is nobody left to tell.
     * <p>
     * The position is read off the frame here rather than tracked while it moves:
     * a drag is a stream of events and only the last one matters.
     */
    void closeQuietly() {
        DimensionService.getInstance().setLocation(PLACEMENT, frame.getLocation());

        frame.dispose();
    }

    private void placeIt() {
        final @NotNull Optional<Point> remembered = Optional.ofNullable(DimensionService.getInstance().getLocation(PLACEMENT));

        remembered.ifPresentOrElse(frame::setLocation, () -> frame.setLocationRelativeTo(null));
    }

    /**
     * Escape and Alt+F4 both arrive at {@link #close}, so no route disposes the
     * frame behind the toolbar button's back - the button asks whether this
     * window exists, and a frame that vanished without saying so would leave it
     * pressed forever.
     */
    private void closeOnEveryRoute() {
        final @NotNull JComponent root = frame.getRootPane();
        final @NotNull Object key = "testin.lightMode.close";

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Shortcuts.Escape.getKey(), key);
        root.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(final @NotNull ActionEvent e) {
                close();
            }
        });

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final @NotNull WindowEvent e) {
                close();
            }
        });
    }

    private @NotNull JComponent content() {
        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()));
        panel.add(titleBar(), BorderLayout.NORTH);
        panel.add(body(), BorderLayout.CENTER);

        return panel;
    }

    /**
     * The window's own title bar: the pin, which run this is, and the close.
     * Dragging is bound here because this is the strip a tester expects to drag.
     */
    private @NotNull JComponent titleBar() {
        final @NotNull JBPanel<?> bar = new JBPanel<>(new BorderLayout(JBUI.scale(6), 0));
        bar.setBorder(JBUI.Borders.empty(4, 6));
        bar.setBackground(JBUI.CurrentTheme.CustomFrameDecorations.paneBackground());

        bar.add(pin(), BorderLayout.WEST);
        bar.add(new JBLabel(run.getName()), BorderLayout.CENTER);
        bar.add(closeButton(), BorderLayout.EAST);

        dragBy(bar);

        return bar;
    }

    /**
     * Pressed on arrival, because the window opens on top - the button reports
     * the state rather than setting it, so the two cannot disagree.
     */
    private @NotNull TitleBarBtn pin() {
        final @NotNull TitleBarBtn button = new TitleBarBtn("Keep above other windows", AllIcons.General.Pin_tab);

        button.setOn(frame.isAlwaysOnTop());
        button.addActionListener(e -> {
            frame.setAlwaysOnTop(!frame.isAlwaysOnTop());
            button.setOn(frame.isAlwaysOnTop());
        });

        return button;
    }

    private @NotNull TitleBarBtn closeButton() {
        final @NotNull TitleBarBtn button = new TitleBarBtn("Close light mode", AllIcons.Actions.Close);

        button.addActionListener(e -> close());

        return button;
    }

    /**
     * The cost of drawing our own title bar: the operating system is no longer
     * moving the window, so this does. Only the offset within the bar is kept -
     * everything else is arithmetic against where the pointer is now.
     */
    private void dragBy(final @NotNull JComponent bar) {
        final @NotNull MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(final @NotNull MouseEvent e) {
                dragOrigin = Optional.of(e.getPoint());
            }

            @Override
            public void mouseReleased(final @NotNull MouseEvent e) {
                dragOrigin = Optional.empty();
            }

            @Override
            public void mouseDragged(final @NotNull MouseEvent e) {
                dragOrigin.ifPresent(origin -> {
                    final @NotNull Point now = e.getLocationOnScreen();
                    frame.setLocation(now.x - origin.x, now.y - origin.y);
                });
            }
        };

        bar.addMouseListener(drag);
        bar.addMouseMotionListener(drag);
    }

    /**
     * What the window holds before it has anything to execute. The case, the
     * verdicts and the rest arrive with the next patch; this is the shell, and
     * its size is what sets the window's.
     */
    private @NotNull JComponent body() {
        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setPreferredSize(new Dimension(JBUI.scale(420), JBUI.scale(96)));

        final @NotNull JBLabel waiting = new JBLabel("Press the play button to start test execution", SwingConstants.CENTER);
        waiting.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        panel.add(waiting, BorderLayout.CENTER);

        return panel;
    }
}
