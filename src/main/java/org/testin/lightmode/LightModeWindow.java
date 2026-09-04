package org.testin.lightmode;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.WindowStateService;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.util.Display;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
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
 * <b>It shows the run editor, it does not run anything.</b> Start, stop, which
 * case is being executed and what a verdict does to the run are all the
 * editor's, exactly as they are when the tester works in the grid - so the two
 * cannot disagree, and light mode inherits every fix to the execution flow
 * without asking. What is here is the drawing of it, and
 * {@link #refresh} is how the editor says something changed.
 * <p>
 * Where it sits is the platform's problem, not this class's.
 * {@link WindowStateService} stores a location against a key, so the window
 * comes back where the tester left it. Deliberately the application-level
 * instance rather than the per-project one: this window belongs to the screen
 * the tester is sitting at, not to whichever project happened to open it - which
 * also means closing it while a project shuts down asks nothing of that
 * project.
 */
final class LightModeWindow {

    /**
     * One key for the placement. Versioned, so a later layout change that makes
     * an old position wrong can start again rather than restoring a window
     * somewhere that no longer makes sense.
     */
    private static final @NotNull String PLACEMENT = "testin.lightMode.v1";

    /**
     * How wide the window opens. Only the width is a number: the height is
     * whatever the case needs, which is what {@link #fitHeight} works out.
     */
    private static final int WIDTH = 420;

    private final @NotNull JFrame frame = new JFrame();
    private final @NotNull Project p;
    private final @NotNull RunEditor editor;
    private final @NotNull Runnable onClosed;

    private final @NotNull TitleBarBtn start = new TitleBarBtn("Start test execution", AllIcons.Actions.Execute);
    private final @NotNull TitleBarBtn stop = new TitleBarBtn("Stop test execution", AllIcons.Actions.Suspend);
    private final @NotNull JBLabel counter = new JBLabel();

    private final @NotNull JBLabel set = new JBLabel();
    private final @NotNull JTextArea description = prose(JBFont.label().biggerOn(3).asBold(), JBUI.CurrentTheme.Label.foreground());
    private final @NotNull JTextArea expected = prose(JBFont.label(), JBUI.CurrentTheme.ContextHelp.FOREGROUND);
    private final @NotNull JBLabel idle = new JBLabel("Press the play button to start test execution", SwingConstants.CENTER);

    private final @NotNull JBPanel<?> caseView = new JBPanel<>(new BorderLayout());
    private final @NotNull JBPanel<?> verdicts = new JBPanel<>(new GridLayout(1, 0, JBUI.scale(6), 0));

    /**
     * Where in the title bar the drag started, and empty whenever no drag is
     * under way.
     */
    private @NotNull Optional<Point> dragOrigin = Optional.empty();

    LightModeWindow(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull Runnable onClosed) {
        this.p = p;
        this.editor = editor;
        this.onClosed = onClosed;

        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setContentPane(content());

        // Before the first refresh, not instead of it: a frame is not laid out
        // at all until it has a peer, and measuring a wrapped paragraph before
        // then measures it at no width.
        frame.pack();
        refresh();

        placeIt();
        bindKeys();

        frame.setVisible(true);
    }

    /**
     * By path, which is how the indexer identifies a run everywhere else - the
     * editor and this window are handed the same cached object today, and this
     * does not quietly depend on that staying true.
     */
    boolean shows(final @NotNull TestRunDirectoryDto other) {
        return editor.getParent().getPath().equals(other.getPath());
    }

    /**
     * Draws whatever the editor is doing now: the case it is executing, how far
     * through the run that is, and whether there is anything to judge yet.
     * <p>
     * Everything is read at the moment it is drawn rather than pushed in as it
     * changes. The editor already holds all of it, and a copy kept here would be
     * one more thing that can be stale - which, in a window whose whole job is
     * to show the tester what they are testing, is the one thing it must not be.
     */
    void refresh() {
        final @NotNull List<TestCaseDto> cases = editor.getCurrentTestCases();
        final int index = editor.getCurrentlyExecutingIndex();
        final boolean executing = index >= 0 && index < cases.size();

        counter.setText(executing ? (index + 1) + " / " + cases.size() : cases.size() + " cases");

        idle.setVisible(!executing);
        caseView.setVisible(executing);
        verdicts.setVisible(executing);

        start.setVisible(!editor.isExecuting());
        stop.setVisible(editor.isExecuting());

        if (executing) showCase(cases.get(index));

        fitHeight();
        frame.repaint();
    }

    /**
     * Keeps the window as wide as it is, and as tall as what is in it.
     * <p>
     * A description that wraps to three lines needs a taller window than one
     * that fits on one, and the tester asked for neither: the design gives them
     * the width and keeps the height out of their hands, because the only thing
     * that should move it is what the case says.
     * <p>
     * The width is applied before the height is asked for, and that order is the
     * whole method. A wrapped paragraph reports the height it needs for the
     * width it currently has, so measuring first and sizing afterwards would
     * measure the width it is about to stop having.
     */
    private void fitHeight() {
        final int width = frame.getWidth() > 0 ? frame.getWidth() : JBUI.scale(WIDTH);

        frame.setSize(width, frame.getHeight());
        frame.validate();
        frame.setSize(width, frame.getPreferredSize().height);
    }

    private void showCase(final @NotNull TestCaseDto tc) {
        set.setText(tc.getParent().getName());
        description.setText(Display.format(tc.getDescription()));
        expected.setText(Display.format(tc.getExpectedResult()));

        // The expected result is optional on a test case, and an empty paragraph
        // leaves a gap the tester reads as something failing to load.
        expected.setVisible(!expected.getText().isBlank());
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
        WindowStateService.getInstance().putLocation(PLACEMENT, frame.getLocation());

        frame.dispose();
    }

    private void placeIt() {
        final @NotNull Optional<Point> remembered = Optional.ofNullable(WindowStateService.getInstance().getLocation(PLACEMENT));

        remembered.ifPresentOrElse(frame::setLocation, () -> frame.setLocationRelativeTo(null));
    }

    /**
     * Escape closes; P, F and B judge the case on screen.
     * <p>
     * Bound on the window rather than on a component inside it, because the
     * tester's hands are on the keyboard and nothing here is worth focusing.
     * Each verdict is bound from the keystroke the status itself declares - the
     * same one {@link VerdictBtn} prints on its cap - so the key that works and
     * the key that is advertised cannot come apart.
     */
    private void bindKeys() {
        bind(Shortcuts.Escape.getKey(), "testin.lightMode.close", this::close);

        for (final TestStatus status : TestStatus.values()) {
            if (!status.isVerdict()) continue;

            bind(status.getMenuEntry().shortcut(), "testin.lightMode." + status.name(), () -> judge(status));
        }

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final @NotNull WindowEvent e) {
                close();
            }
        });
    }

    private void bind(final @NotNull KeyStroke key, final @NotNull String name, final @NotNull Runnable action) {
        final @NotNull JComponent root = frame.getRootPane();

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(final @NotNull ActionEvent e) {
                action.run();
            }
        });
    }

    /**
     * Records a verdict on the case being executed - the one on screen.
     * <p>
     * Through the same service call the grid makes, which advances to the next
     * case, persists, completes the run if that was the last one, and tells the
     * tester. It refuses on its own when nothing is being executed, which is
     * what a key pressed on the idle window is.
     */
    private void judge(final @NotNull TestStatus status) {
        Services.getInstance(p, RunStatusService.class).executeNext(p, editor, status);
    }

    private @NotNull JComponent content() {
        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()));
        panel.add(titleBar(), BorderLayout.NORTH);
        panel.add(body(), BorderLayout.CENTER);
        panel.add(verdictBar(), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * The window's own title bar: start or stop, the pin, which run this is, how
     * far through it the tester is, and the close. Dragging is bound here
     * because this is the strip a tester expects to drag.
     */
    private @NotNull JComponent titleBar() {
        final @NotNull JBPanel<?> bar = new JBPanel<>(new BorderLayout(JBUI.scale(6), 0));
        bar.setBorder(JBUI.Borders.empty(4, 6));
        bar.setBackground(JBUI.CurrentTheme.CustomFrameDecorations.paneBackground());

        start.addActionListener(e -> editor.onStartExecutionClicked());
        stop.addActionListener(e -> editor.onStopExecutionClicked());

        final @NotNull JBPanel<?> left = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(2), 0));
        left.setOpaque(false);
        left.add(start);
        left.add(stop);
        left.add(pin());

        counter.setFont(JBUI.Fonts.smallFont());
        counter.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        final @NotNull JBPanel<?> right = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(6), 0));
        right.setOpaque(false);
        right.add(counter);
        right.add(closeButton());

        bar.add(left, BorderLayout.WEST);
        bar.add(new JBLabel(editor.getParent().getName()), BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);

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
     * The case, or the sentence that stands in for it before the run is started.
     * Both are built once and one of them is shown, so starting a run does not
     * re-lay out the window.
     */
    private @NotNull JComponent body() {
        set.setFont(JBUI.Fonts.smallFont());
        set.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        idle.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        final @NotNull JBPanel<?> text = new JBPanel<>(new BorderLayout(0, JBUI.scale(10)));
        text.setOpaque(false);
        text.add(description, BorderLayout.NORTH);
        text.add(expected, BorderLayout.CENTER);

        caseView.setOpaque(false);
        caseView.add(set, BorderLayout.NORTH);
        caseView.add(text, BorderLayout.CENTER);

        // The width is this window's to choose and the height is the case's, so
        // the panel answers with one of each rather than taking a fixed size -
        // which is also what lets a wrapped paragraph be measured at the width
        // it is actually going to be drawn at.
        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout()) {
            @Override
            public @NotNull Dimension getPreferredSize() {
                return new Dimension(JBUI.scale(WIDTH), super.getPreferredSize().height);
            }
        };

        panel.setBorder(JBUI.Borders.empty(14));
        panel.add(idle, BorderLayout.CENTER);
        panel.add(caseView, BorderLayout.NORTH);

        return panel;
    }

    /**
     * The three verdicts, equally wide because they are equally likely - a
     * tester reaching for one of them is not choosing between a default and two
     * exceptions.
     */
    private @NotNull JComponent verdictBar() {
        verdicts.setBorder(JBUI.Borders.empty(0, 10, 10, 10));
        verdicts.setOpaque(false);

        for (final TestStatus status : TestStatus.values()) {
            if (!status.isVerdict()) continue;

            verdicts.add(new VerdictBtn(status, () -> judge(status)));
        }

        return verdicts;
    }

    /**
     * A paragraph the tester reads: wrapped, unselectable furniture rather than
     * a field. A label would print it on one line and let the window grow as
     * wide as the sentence.
     */
    private static @NotNull JTextArea prose(final @NotNull Font font, final @NotNull Color color) {
        final @NotNull JTextArea area = new JTextArea();
        area.setFont(font);
        area.setForeground(color);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setBorder(null);

        return area;
    }
}
