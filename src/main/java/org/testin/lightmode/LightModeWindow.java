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
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.statusbar.StatusBarBase;
import org.testin.statusbar.StatusBarItem;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Display;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
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

    /**
     * Two keys for one state, because that is what the tester asked for: one
     * that opens the details and one that closes them, rather than a single key
     * whose effect depends on what is already on screen.
     * <p>
     * Constants here rather than in {@link Shortcuts}, which holds the keys more
     * than one class binds. These are this window's alone.
     */
    private static final @NotNull KeyStroke SHOW_DETAILS = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK);
    private static final @NotNull KeyStroke HIDE_DETAILS = KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK);

    private final @NotNull JFrame frame = new JFrame();
    private final @NotNull Project p;
    private final @NotNull RunEditor editor;
    private final @NotNull Runnable onClosed;

    private final @NotNull TitleBarBtn start = new TitleBarBtn("Start test execution", AllIcons.Actions.Execute);
    private final @NotNull TitleBarBtn stop = new TitleBarBtn("Stop test execution", AllIcons.Actions.Suspend);
    private final @NotNull JBLabel counter = new JBLabel();

    private final @NotNull JBLabel set = new JBLabel();
    private final @NotNull JTextArea description = Prose.of(JBFont.label().biggerOn(3).asBold(), JBUI.CurrentTheme.Label.foreground());
    private final @NotNull JTextArea expected = Prose.of(JBFont.label(), JBUI.CurrentTheme.ContextHelp.FOREGROUND);
    private final @NotNull JBLabel idle = new JBLabel("Press the play button to start test execution", SwingConstants.CENTER);

    private final @NotNull JBLabel chosen = new JBLabel();
    private final @NotNull JBPanel<?> caseView = new JBPanel<>(new BorderLayout());
    private final @NotNull CaseDetails details = new CaseDetails();

    /**
     * The one box under the case. It holds the details that {@code Ctrl+D} fills
     * with steps and tags, or the four fields a failure is written into - never
     * both, and never one added below the other. There is one window and one box
     * in it (#13).
     */
    private final @NotNull JBPanel<?> underCase = new JBPanel<>(new BorderLayout());

    private final @NotNull JBLabel caseClock = clock("Test case duration");
    private final @NotNull JBLabel runClock = clock("Test Run duration");

    /**
     * The verdicts, the clocks and the keys, shown together or not at all.
     * <p>
     * All three answer questions about a case being executed, so before the run
     * is started there is nothing for any of them to say - and one thing that
     * appears and disappears is one thing to reason about, where three were
     * three chances for the window to be caught half dressed.
     */
    private final @NotNull JBPanel<?> footer = new JBPanel<>(new BorderLayout());

    /**
     * The three verdicts, and the Cancel and Save that stand in their place
     * while a failure is being written. Built once and swapped, so the row keeps
     * its place and the window does not jump under the tester's hand.
     */
    private final @NotNull JBPanel<?> verdictRow = new JBPanel<>(new BorderLayout());
    private final @NotNull JComponent verdictButtons = verdictButtons();
    private final @NotNull JComponent commitButtons = commitButtons();

    private final @NotNull StatusBarBase statusBar = new StatusBarBase(new StatusBarItem[0]);

    /**
     * Where in the title bar the drag started, and empty whenever no drag is
     * under way.
     */
    private @NotNull Optional<Point> dragOrigin = Optional.empty();

    /**
     * Closed until asked for. The window exists to put one sentence in front of
     * a tester, so everything else starts out of the way.
     */
    private boolean detailsShown;

    /**
     * The failure being written up, and empty the rest of the time.
     * <p>
     * Every key in this window asks it first. While a form is waiting to be
     * filled in, Escape belongs to the form rather than to the window, Enter
     * means save, and the verdict keys mean nothing at all - a case whose
     * failure is halfway written down is not one to give a second verdict to.
     */
    private @NotNull Optional<FailureForm> capture = Optional.empty();

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
        footer.setVisible(executing);

        start.setVisible(!editor.isExecuting());
        stop.setVisible(editor.isExecuting());

        if (executing) showCase(cases.get(index));

        // Any execution-state change outdates a half-written failure: the case it
        // describes is no longer the case in front of the tester. Dropped rather
        // than carried over, because carrying it over would attach one case's
        // failure to the next one's row.
        capture = Optional.empty();
        showCapture();

        tick();

        fitHeight();
        frame.repaint();
    }

    /**
     * The two clocks, and only them.
     * <p>
     * Quiet and small, and never in the body: a number counting up in front of a
     * tester is a stopwatch, and a stopwatch makes them hurry. That is why they
     * are down here rather than beside the case, and why they are drawn in the
     * hint color rather than the text color.
     */
    void tick() {
        caseClock.setText(seconds(editor.getCurrentCaseElapsed()));
        runClock.setText(seconds(editor.getElapsed()));
    }

    /**
     * The same duration the editor's own status bar shows, to the second.
     * <p>
     * Truncated because these two tick in front of a tester who is reading:
     * the timer writes milliseconds, so untruncated the clocks would end in
     * three digits that change every second and take the number's width with
     * them. Display only - what is stored keeps every millisecond it measured.
     */
    private static @NotNull String seconds(final @NotNull Duration duration) {
        return Display.formatDuration(duration.truncatedTo(ChronoUnit.SECONDS));
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

        // Visibility is not touched here: rebuilding the rows does not change
        // whether they are shown, and showDetails is the one thing that decides.
        details.show(p, tc);

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
     * Escape leaves whatever the tester is in; Enter commits it; P, F and B
     * judge the case on screen.
     * <p>
     * Bound on the window rather than on a component inside it, because the
     * tester's hands are on the keyboard and nothing here is worth focusing.
     * Each verdict is bound from the keystroke the status itself declares - the
     * same one {@link KeyBtn} prints on its cap - so the key that works and the
     * key that is advertised cannot come apart.
     * <p>
     * <b>One handler per key, which then asks what is on screen.</b> Escape is
     * the clear case: it means "leave this", and while a failure is being
     * written the thing to leave is the form rather than the window. Two
     * handlers racing for Escape would have been resolved by whichever the
     * framework consulted first, silently.
     * <p>
     * A window binding is only reached by a key the focused component did not
     * take, which is what makes typing safe: the error capture keeps Enter for
     * its own newlines, and every letter typed into a field stays in the field
     * rather than recording a verdict.
     */
    private void bindKeys() {
        bind(Shortcuts.Escape.getKey(), "testin.lightMode.escape", this::escape);
        bind(Shortcuts.Enter.getKey(), "testin.lightMode.commit", this::saveCapture);
        bind(SHOW_DETAILS, "testin.lightMode.showDetails", () -> showDetails(true));
        bind(HIDE_DETAILS, "testin.lightMode.hideDetails", () -> showDetails(false));

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
     * Opens or closes the case's other fields, and resizes the window to what is
     * left.
     * <p>
     * Only the height moves: the tester chose the width and the details are not
     * a reason to take it away from them. Doing nothing when it is already in
     * the asked-for state keeps Ctrl+D held down from re-laying the window out
     * on every repeat.
     */
    private void showDetails(final boolean show) {
        // A form cannot be collapsed while it is waiting to be filled in.
        if (capture.isPresent()) return;

        if (detailsShown == show) return;

        detailsShown = show;
        details.setVisible(show);

        fitHeight();
    }

    /**
     * Takes a verdict on the case being executed - the one on screen.
     * <p>
     * A verdict that collects failure details opens the form instead of
     * recording anything; the record happens when the tester saves it. Which
     * verdict that is comes from the status itself, so this does not name
     * FAILED and would not have to be found again if a second such verdict were
     * ever added.
     */
    private void judge(final @NotNull TestStatus status) {
        if (capture.isPresent()) return;

        if (status.isCollectsFailureDetails()) {
            openCapture();
            return;
        }

        record(status);
    }

    /**
     * Writes the verdict down, through the same service call the grid makes -
     * which advances to the next case, persists, completes the run if that was
     * the last one, and tells the tester. It refuses on its own when nothing is
     * being executed, which is what a key pressed on the idle window is.
     */
    private void record(final @NotNull TestStatus status) {
        Services.getInstance(p, RunStatusService.class).executeNext(p, editor, status);
    }

    /**
     * Opens the failure form on the case being executed, and puts the caret in
     * its first field.
     * <p>
     * Nothing happens if there is no row to write on - a case the run does not
     * cover, or one removed from the test set. The window says nothing about it
     * because the tester pressed a key rather than asking a question, and the
     * form simply not opening is the answer.
     */
    private void openCapture() {
        executingItem().ifPresent(item -> {
            capture = Optional.of(new FailureForm(item));

            showCapture();
            fitHeight();

            capture.ifPresent(FailureForm::focusFirstField);
        });
    }

    /**
     * Leaves the form with the case still unjudged and nothing written - which
     * is the promise Escape makes everywhere else in this plugin.
     */
    private void cancelCapture() {
        if (capture.isEmpty()) return;

        capture = Optional.empty();

        showCapture();
        fitHeight();
    }

    /**
     * Writes the four fields onto the run row and then records the verdict, in
     * that order: a verdict is what decides whether what was typed survives, so
     * it goes last. It is also the order {@code FailedResultDialog} uses.
     */
    private void saveCapture() {
        capture.ifPresent(form -> {
            form.save();
            capture = Optional.empty();

            showCapture();
            fitHeight();

            record(TestStatus.FAILED);
        });
    }

    /**
     * Escape means "leave what I am in": the form while one is open, and the
     * window otherwise.
     */
    private void escape() {
        if (capture.isPresent()) {
            cancelCapture();
            return;
        }

        close();
    }

    /**
     * Draws whichever of the two states the window is in - the case with its
     * details and three verdicts, or the case with a failure form and two
     * commands under it.
     * <p>
     * One method, because the three things that change change together. Split
     * across the places that trigger them, a window could show the commit bar
     * over the details, or Escape's hint while Escape meant something else.
     */
    private void showCapture() {
        final boolean writing = capture.isPresent();

        underCase.removeAll();
        underCase.add(capture.map(form -> (JComponent) form).orElse(details), BorderLayout.CENTER);

        verdictRow.removeAll();
        verdictRow.add(writing ? commitButtons : verdictButtons, BorderLayout.CENTER);

        chosen.setVisible(writing);

        statusBar.updateItems(writing ? commitKeys() : caseKeys());
    }

    /**
     * The run row for the case being executed, and empty when there is none to
     * write on.
     */
    private @NotNull Optional<TestRunItems> executingItem() {
        final @NotNull List<TestCaseDto> cases = editor.getCurrentTestCases();
        final int index = editor.getCurrentlyExecutingIndex();

        if (index < 0 || index >= cases.size()) return Optional.empty();

        return editor.runItem(cases.get(index).getId()).filter(item -> !item.isRemoved());
    }

    private @NotNull JComponent content() {
        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()));
        panel.add(titleBar(), BorderLayout.NORTH);
        panel.add(body(), BorderLayout.CENTER);
        panel.add(footer(), BorderLayout.SOUTH);

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

        // The one place a verdict is colored while the tester is still working:
        // they have chosen Failed and are writing it up, so it is a state rather
        // than an offer. Everywhere else in this window the three verdicts are
        // drawn alike.
        chosen.setFont(JBUI.Fonts.smallFont());
        chosen.setForeground(TestStatus.FAILED.getRowColor());
        chosen.setText(" \u00b7 " + TestStatus.FAILED.getLabel());

        final @NotNull JBPanel<?> text = new JBPanel<>(new BorderLayout(0, JBUI.scale(10)));
        text.setOpaque(false);
        text.add(description, BorderLayout.NORTH);
        text.add(expected, BorderLayout.CENTER);

        details.setBorder(JBUI.Borders.emptyTop(14));
        details.setVisible(detailsShown);

        final @NotNull JBPanel<?> setLine = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setLine.setOpaque(false);
        setLine.add(set);
        setLine.add(chosen);

        underCase.setOpaque(false);

        caseView.setOpaque(false);
        caseView.add(setLine, BorderLayout.NORTH);
        caseView.add(text, BorderLayout.CENTER);
        caseView.add(underCase, BorderLayout.SOUTH);

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
    private @NotNull JComponent verdictButtons() {
        final @NotNull JBPanel<?> verdicts = new JBPanel<>(new GridLayout(1, 0, JBUI.scale(6), 0));
        verdicts.setBorder(JBUI.Borders.empty(0, 10, 10, 10));
        verdicts.setOpaque(false);

        for (final TestStatus status : TestStatus.values()) {
            if (!status.isVerdict()) continue;

            verdicts.add(new KeyBtn(keyOf(status), status.getLabel(),
                    "Record " + status.getLabel().toLowerCase(Locale.ROOT) + " for this test case",
                    () -> judge(status)));
        }

        return verdicts;
    }

    /**
     * What replaces the verdicts while a failure is being written: leave it, or
     * save it and move on.
     * <p>
     * Sized to their words and pushed right, where the verdicts are stretched
     * equally across the window - these two are not a choice between equals, and
     * laying them out as one would say they were.
     */
    private @NotNull JComponent commitButtons() {
        final @NotNull JBPanel<?> commit = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(6), 0));
        commit.setBorder(JBUI.Borders.empty(0, 10, 10, 10));
        commit.setOpaque(false);

        commit.add(new KeyBtn(Shortcuts.Escape.getShortcutText(), "Cancel",
                "Leave the case unjudged and write nothing", this::cancelCapture));
        commit.add(new KeyBtn(Shortcuts.Enter.getShortcutText(), "Save & next",
                "Record the failure and move to the next test case", this::saveCapture));

        return commit;
    }

    /**
     * The letter that applies a status, read from the status itself so the cap
     * and the binding cannot name different keys.
     */
    private static @NotNull String keyOf(final @NotNull TestStatus status) {
        return Shortcuts.shortcutText(status.getMenuEntry().shortcut());
    }

    /**
     * What sits under the case: the three verdicts, the two clocks, and the keys.
     * <p>
     * The verdicts and the clocks take the window's own background and the
     * status bar takes the platform's advertiser tint, so the strip of keys
     * reads as furniture and the two rows above it as part of the case.
     */
    private @NotNull JComponent footer() {
        footer.setOpaque(false);
        footer.add(verdictRow, BorderLayout.NORTH);
        footer.add(durationStrip(), BorderLayout.CENTER);
        footer.add(statusBar.getPanel(), BorderLayout.SOUTH);

        return footer;
    }

    /**
     * This case on the left, the whole run on the right. Neither is labeled: the
     * tester learns which is which once, and a word beside each would make the
     * clocks the loudest thing in a window built to hold one sentence.
     */
    private @NotNull JComponent durationStrip() {
        final @NotNull JBPanel<?> strip = new JBPanel<>(new BorderLayout());
        strip.setBorder(JBUI.Borders.empty(0, 10, 8, 10));
        strip.setOpaque(false);
        strip.add(caseClock, BorderLayout.WEST);
        strip.add(runClock, BorderLayout.EAST);

        return strip;
    }

    /**
     * The keys, drawn by the same strip every dialog in the plugin uses - so
     * light mode's keycaps are the plugin's keycaps, and the next change to them
     * arrives here without being asked for.
     * <p>
     * Hints, every one: this window binds its own keys in {@link #bindKeys}, and
     * an entry that claimed to bind them too would be a second claimant on P.
     * The letters still come from {@link TestStatus}, so the cap cannot name a
     * key that does nothing.
     */
    private StatusBarItem @NotNull [] caseKeys() {
        final @NotNull List<StatusBarItem> items = new ArrayList<>();
        items.add(StatusBarShortcut.hint(Shortcuts.shortcutText(SHOW_DETAILS), "Details"));
        items.add(StatusBarShortcut.hint(Shortcuts.shortcutText(HIDE_DETAILS), "Hide"));
        items.add(StatusBarShortcut.hint(Shortcuts.Escape.getShortcutText(), "Close"));

        for (final TestStatus status : TestStatus.values()) {
            if (status.isVerdict()) items.add(StatusBarShortcut.hint(keyOf(status), status.getLabel()));
        }

        return items.toArray(new StatusBarItem[0]);
    }

    /**
     * Two keys while a failure is being written, and neither of them is one of
     * the four above: Ctrl+D cannot collapse a form waiting to be filled in, and
     * Escape has stopped meaning close.
     */
    private StatusBarItem @NotNull [] commitKeys() {
        return new StatusBarItem[]{
                StatusBarShortcut.hint(Shortcuts.Enter.getShortcutText(), "Save & next"),
                StatusBarShortcut.hint(Shortcuts.Escape.getShortcutText(), "Cancel")};
    }

    private static @NotNull JBLabel clock(final @NotNull String meaning) {
        final @NotNull JBLabel label = new JBLabel();
        label.setToolTipText(meaning);
        label.setFont(JBUI.Fonts.smallFont());
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        return label;
    }

}
