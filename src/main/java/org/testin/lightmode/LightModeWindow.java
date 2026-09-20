/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.lightmode;

import org.testin.editor.run.ExecutionControl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.WriteIntentReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.WindowStateService;
import com.intellij.ui.WindowMoveListener;
import com.intellij.ui.WindowResizeListener;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.Animator;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import java.awt.Toolkit;
import org.testin.editor.run.RunEditor;
import org.testin.editor.toolbar.components.StartExecutionBtn;
import org.testin.model.DirectoryType;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testrun.RunStatusService;
import org.testin.ui.framework.StatusBarBase;
import org.testin.model.StatusBarItem;
import org.testin.ui.Motion;
import org.testin.ui.framework.Prose;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class LightModeWindow {
    private static final @NotNull String PLACEMENT = "testin.lightMode.v1";

    private static final @NotNull String ZOOM = "testin.lightMode.zoom.v1";

    private static final int WIDTH = 420;

    private static final int MIN_WIDTH = 280;

    private static final int GRAB = 4;

    private static final float ZOOM_STEP = 0.1f;
    private static final float ZOOM_MIN = 0.8f;
    private static final float ZOOM_MAX = 2.0f;

    private final @NotNull JFrame frame = new JFrame();
    private final @NotNull RunEditor editor;
    private final @NotNull Runnable onClosed;

    private final @NotNull TitleBarBtn start = new TitleBarBtn(ExecutionControl.START.getLabel(), ExecutionControl.START.getIcon());
    private final @NotNull TitleBarBtn stop = new TitleBarBtn(ExecutionControl.STOP.getLabel(), ExecutionControl.STOP.getIcon());
    private final @NotNull JBLabel counter = new JBLabel();

    private final @NotNull Font setFont = CaseFont.label();
    private final @NotNull Font descriptionFont = CaseFont.description();
    private final @NotNull Font expectedFont = CaseFont.body();

    private final @NotNull JBLabel set = new JBLabel();
    private final @NotNull JTextArea description = Prose.of(descriptionFont, JBUI.CurrentTheme.Label.foreground());
    private final @NotNull JTextArea expected = Prose.of(expectedFont, JBUI.CurrentTheme.ContextHelp.FOREGROUND);

    private final @NotNull JComponent expectedRow = iconBefore(CreateTestCaseFields.EXPECTED_RESULT.getIcon(), expected);
    private final @NotNull JBLabel idle = new JBLabel(Bundle.message("light.idle"), SwingConstants.CENTER);

    private final @NotNull JBLabel chosen = new JBLabel();
    private final @NotNull SlidingPanel caseView = new SlidingPanel(new BorderLayout());

    private @NotNull Optional<UUID> shownCase = Optional.empty();

    private final @NotNull Disposable motionScope = Disposer.newDisposable("Testin light mode motion");

    private @NotNull Optional<Animator> heightMotion = Optional.empty();
    private @NotNull Optional<Animator> slideMotion = Optional.empty();
    private final @NotNull CaseDetails details;

    private final @NotNull JBPanel<?> underCase = new JBPanel<>(new BorderLayout());

    private final @NotNull JBLabel caseClock = clock(Bundle.message("light.case.clock"));
    private final @NotNull JBLabel runClock = clock(Bundle.message("light.run.clock"));
    private final @NotNull JBPanel<?> strip = new JBPanel<>(new BorderLayout());
    private final @NotNull JBPanel<?> setLine = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));

    private final @NotNull JBPanel<?> footer = new JBPanel<>(new BorderLayout());

    private final @NotNull JComponent verdictRow = verdictButtons();

    private final @NotNull StatusBarBase statusBar = new StatusBarBase(new StatusBarItem[0]);

    private final @NotNull ViewMenuBtn viewMenu = new ViewMenuBtn(this::applyView);

    private @NotNull Optional<FailureForm> capture = Optional.empty();

    private float zoom = Math.clamp(PropertiesComponent.getInstance().getFloat(ZOOM, 1.0f), ZOOM_MIN, ZOOM_MAX);

    private boolean detailsKeyHeld = false;

    private int lastWidth;

    LightModeWindow(final @NotNull RunEditor editor, final @NotNull Runnable onClosed) {
        this.editor = editor;
        this.details = new CaseDetails(editor.getProject());
        this.onClosed = onClosed;

        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setContentPane(content());

        applyZoom();

        frame.pack();

        placeIt();
        refresh();

        bindKeys();
        bindWheel();
        bindResize();

        frame.setVisible(true);
    }

    boolean shows(final @NotNull TestRunDirectoryDto other) {
        return editor.getParent().getPath().equals(other.getPath());
    }

    void refresh() {
        final @NotNull List<TestCaseDto> cases = editor.getCurrentTestCases();
        final int index = editor.getCurrentlyExecutingIndex();
        final boolean executing = index >= 0 && index < cases.size();

        counter.setText(executing
                ? Bundle.message("light.counter.position", String.valueOf(index + 1), String.valueOf(cases.size()))
                : Bundle.message("light.counter.cases", String.valueOf(cases.size())));

        idle.setVisible(!executing);
        caseView.setVisible(executing);
        footer.setVisible(executing);

        start.setVisible(!editor.isExecuting());
        stop.setVisible(editor.isExecuting());

        // Rule-EDITOR-PANEL-135
        start.setEnabled(editor.canStartManualExecution());
        start.setToolTipText(StartExecutionBtn.tooltipFor(editor));

        final @NotNull Optional<UUID> wasShowing = shownCase;
        if (executing) showCase(cases.get(index));

        if (!executing || !shownCase.equals(wasShowing)) capture = Optional.empty();

        if (capture.isPresent()) applyParts();
        else showCapture();

        tick();

        fitHeight();
        frame.repaint();
    }

    void tick() {
        caseClock.setText(Display.formatCaseClock(editor.getCurrentCaseElapsed()));
        runClock.setText(Display.formatRunClock(editor.getElapsed()));
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-202, Rule-EDITOR-PANEL-204
    private void fitHeight() {
        frame.validate();

        final int wanted = frame.getPreferredSize().height;
        final int usable = usableHeight();

        details.setCutOff(wanted > usable);
        frame.validate();

        final int target = Math.min(frame.getPreferredSize().height, usable);
        final int from = frame.getHeight();

        if (from == target || !frame.isShowing() || from <= 0) {
            frame.setSize(frame.getWidth(), target);
            return;
        }

        heightMotion.ifPresent(Animator::dispose);

        heightMotion = Motion.run(motionScope, "Testin light mode height",
                travelled -> frame.setSize(frame.getWidth(), from + (int) ((target - from) * travelled)),
                () -> frame.setSize(frame.getWidth(), target));
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-217
    private int usableHeight() {
        return Optional.ofNullable(frame.getGraphicsConfiguration())
                .map(gc -> gc.getBounds().height
                        - Toolkit.getDefaultToolkit().getScreenInsets(gc).top
                        - Toolkit.getDefaultToolkit().getScreenInsets(gc).bottom)
                .orElseGet(() -> Toolkit.getDefaultToolkit().getScreenSize().height);
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-201
    private void showCase(final @NotNull TestCaseDto tc) {
        final boolean arrived = shownCase.map(previous -> !previous.equals(tc.getId())).orElse(false);
        shownCase = Optional.of(tc.getId());

        if (arrived) caseView.captureLeaving();

        set.setText(tc.getParent().getName());
        description.setText(TestEditorAttributes.DESCRIPTION.displayValue(tc));
        expected.setText(TestEditorAttributes.EXPECTED_RESULT.displayValue(tc));

        details.show(tc);

        if (arrived) slideCaseIn();
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-201, Rule-EDITOR-PANEL-204
    private void slideCaseIn() {
        if (!caseView.hasSomethingToSlide()) return;

        slideMotion.ifPresent(Animator::dispose);

        slideMotion = Motion.run(motionScope, "Testin light mode case",
                caseView::setTravelled,
                () -> caseView.setTravelled(1.0));
    }

    void close() {
        closeQuietly();

        onClosed.run();
    }

    void closeQuietly() {
        WindowStateService.getInstance().putLocation(PLACEMENT, frame.getLocation());

        WindowStateService.getInstance().putSize(PLACEMENT, frame.getSize());

        Disposer.dispose(motionScope);
        frame.dispose();
    }

    private void placeIt() {
        Optional.ofNullable(WindowStateService.getInstance().getSize(PLACEMENT))
                .map(size -> Math.max(size.width, JBUI.scale(MIN_WIDTH)))
                .ifPresent(width -> frame.setSize(width, frame.getHeight()));

        lastWidth = frame.getWidth();

        Optional.ofNullable(WindowStateService.getInstance().getLocation(PLACEMENT))
                .ifPresentOrElse(frame::setLocation, () -> frame.setLocationRelativeTo(null));
    }

    private void bindKeys() {
        bind(Shortcuts.Escape.getKey(), "testin.lightMode.escape", this::escape);
        bind(Shortcuts.Enter.getKey(), "testin.lightMode.commit", this::saveCapture);
        bind(Shortcuts.ToggleDetails.getKey(), "testin.lightMode.toggleDetails", this::toggleDetailsOnce);
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK, true), "testin.lightMode.releaseDetails", () -> detailsKeyHeld = false);
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "testin.lightMode.releaseDetailsAlone", () -> detailsKeyHeld = false);

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

    private void bindWheel() {
        frame.getContentPane().addMouseWheelListener(e -> {
            zoomBy(-e.getWheelRotation() * ZOOM_STEP);
            e.consume();
        });
    }

    // UC-EDITOR-PANEL-046
    private void zoomBy(final float delta) {
        final float next = Math.clamp(zoom + delta, ZOOM_MIN, ZOOM_MAX);
        if (next == zoom) return;

        zoom = next;
        PropertiesComponent.getInstance().setValue(ZOOM, zoom, 1.0f);

        applyZoom();
        fitHeight();
    }

    private void applyZoom() {
        set.setFont(scaled(setFont));

        chosen.setFont(scaled(setFont));
        description.setFont(scaled(descriptionFont));
        expected.setFont(scaled(expectedFont));

        details.setZoom(zoom);
        capture.ifPresent(form -> form.setZoom(zoom));
    }

    private @NotNull Font scaled(final @NotNull Font base) {
        return CaseFont.zoomed(base, zoom);
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

    // UC-EDITOR-PANEL-046
    private void toggleDetailsOnce() {
        if (detailsKeyHeld) return;

        detailsKeyHeld = true;
        toggleDetails();
    }

    private void toggleDetails() {
        if (capture.isPresent()) return;

        details.setVisible(!details.isVisible());

        fitHeight();
    }

    // UC-EDITOR-PANEL-046
    private void judge(final @NotNull TestStatus status) {
        if (capture.isPresent()) return;

        if (status.isCollectsFailureDetails()) {
            openCapture();
            return;
        }

        record(status);
    }

    private void record(final @NotNull TestStatus status) {
        final @NotNull Project p = editor.getProject();

        Services.getInstance(p, RunStatusService.class).executeNext(p, editor, status);
    }

    private void openCapture() {
        executingItem().ifPresent(item -> {
            capture = Optional.of(new FailureForm(editor.getProject(), editor.getParent().getPath(), item, zoom, this::fitHeight));

            showCapture();
            fitHeight();

            capture.ifPresent(FailureForm::focusFirstField);
        });
    }

    private void cancelCapture() {
        capture = Optional.empty();

        showCapture();
        fitHeight();
    }

    private void saveCapture() {
        capture.ifPresent(form -> {
            // Rule-EDITOR-PANEL-225
            if (!form.save()) return;

            capture = Optional.empty();

            showCapture();
            fitHeight();

            record(TestStatus.FAILED);
        });
    }

    // UC-EDITOR-PANEL-046
    private void escape() {
        if (capture.isPresent()) {
            cancelCapture();
            return;
        }

        close();
    }

    // UC-EDITOR-PANEL-046
    @SuppressWarnings("UnstableApiUsage")
    private void showCapture() {
        underCase.removeAll();

        WriteIntentReadAction.run(() -> underCase.add(capture.map(form -> (JComponent) form).orElse(details), BorderLayout.CENTER));

        statusBar.updateItems(capture.isPresent() ? commitKeys() : caseKeys());

        applyParts();
    }

    private void applyView() {
        applyParts();
        fitHeight();
    }

    private void applyParts() {
        final boolean writing = capture.isPresent();

        set.setVisible(shows(LightModePart.SET_NAME));
        chosen.setVisible(writing);
        chosen.setText(set.isVisible() ? " · " + TestStatus.FAILED.getLabel() : TestStatus.FAILED.getLabel());
        setLine.setVisible(set.isVisible() || chosen.isVisible());

        expectedRow.setVisible(!expected.getText().isBlank());

        strip.setVisible(shows(LightModePart.DURATION));
        verdictRow.setVisible(shows(LightModePart.VERDICT_BUTTONS) && !writing);
        statusBar.setShown(shows(LightModePart.STATUS_BAR) || writing);
    }

    private boolean shows(final @NotNull LightModePart part) {
        return viewMenu.getSelectedDetails().contains(part);
    }

    private @NotNull Optional<TestRunItems> executingItem() {
        final @NotNull List<TestCaseDto> cases = editor.getCurrentTestCases();
        final int index = editor.getCurrentlyExecutingIndex();

        if (index < 0 || index >= cases.size()) return Optional.empty();

        return editor.runItem(cases.get(index).getId()).filter(item -> !item.isRemoved());
    }

    private @NotNull JComponent content() {
        final @NotNull JBPanel<?> panel = new JBPanel<>(new BorderLayout());

        panel.setBackground(JBUI.CurrentTheme.CustomFrameDecorations.paneBackground());
        panel.setBorder(BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                JBUI.Borders.empty(0, GRAB)));

        panel.add(titleBar(), BorderLayout.NORTH);
        panel.add(body(), BorderLayout.CENTER);
        panel.add(footer(), BorderLayout.SOUTH);

        return panel;
    }

    private @NotNull JComponent titleBar() {
        final @NotNull JBPanel<?> bar = new JBPanel<>(new BorderLayout(JBUI.scale(6), 0));
        bar.setBorder(JBUI.Borders.empty(4, 6));

        bar.setBackground(JBUI.CurrentTheme.Advertiser.background());

        start.addActionListener(e -> editor.onStartExecutionClicked());
        stop.addActionListener(e -> editor.onStopExecutionClicked());

        final @NotNull JBPanel<?> left = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(2), 0));
        left.setOpaque(false);
        left.add(start);
        left.add(stop);
        left.add(pin());
        left.add(viewMenu);

        counter.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        bar.add(left, BorderLayout.WEST);
        final @NotNull JBLabel runName = new JBLabel(editor.getParent().getName());
        runName.setFont(JBUI.Fonts.label().biggerOn(1f));

        bar.add(runName, BorderLayout.CENTER);
        bar.add(counter, BorderLayout.EAST);

        dragBy(bar);

        return bar;
    }

    private @NotNull TitleBarBtn pin() {
        final @NotNull TitleBarBtn button = new TitleBarBtn(Bundle.message("light.pin"), AllIcons.General.Pin_tab);

        button.setOn(frame.isAlwaysOnTop());
        button.addActionListener(e -> {
            frame.setAlwaysOnTop(!frame.isAlwaysOnTop());
            button.setOn(frame.isAlwaysOnTop());
        });

        return button;
    }

    private void dragBy(final @NotNull JComponent bar) {
        new WindowMoveListener(bar).installTo(bar);
    }

    // UC-EDITOR-PANEL-046
    private void bindResize() {
        frame.setMinimumSize(new Dimension(JBUI.scale(MIN_WIDTH), 0));

        final @NotNull JComponent content = (JComponent) frame.getContentPane();
        final @NotNull WindowResizeListener resize = new WindowResizeListener(content, JBUI.insets(0, GRAB), null);

        content.addMouseListener(resize);
        content.addMouseMotionListener(resize);

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final @NotNull ComponentEvent e) {
                if (frame.getWidth() == lastWidth) return;

                lastWidth = frame.getWidth();
                fitHeight();
            }
        });
    }

    private @NotNull JComponent body() {
        set.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        set.setIcon(Icons.gray(DirectoryType.TS.getIcon()));
        set.setIconTextGap(JBUI.scale(CaseDetails.GAP));
        idle.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        chosen.setForeground(TestStatus.FAILED.getRowColor());

        final @NotNull JBPanel<?> text = new JBPanel<>(new BorderLayout(0, JBUI.scale(10)));
        text.setOpaque(false);
        text.add(iconBefore(CreateTestCaseFields.DESCRIPTION.getIcon(), description), BorderLayout.NORTH);
        text.add(expectedRow, BorderLayout.CENTER);

        details.setBorder(JBUI.Borders.emptyTop(14));
        details.setVisible(false);

        setLine.setOpaque(false);
        setLine.setBorder(JBUI.Borders.emptyBottom(CaseDetails.GAP));
        setLine.add(set);
        setLine.add(chosen);

        underCase.setOpaque(false);

        caseView.setOpaque(false);
        caseView.add(setLine, BorderLayout.NORTH);
        caseView.add(text, BorderLayout.CENTER);
        caseView.add(underCase, BorderLayout.SOUTH);

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

    private @NotNull JComponent verdictButtons() {
        final @NotNull JBPanel<?> verdicts = new JBPanel<>(new GridLayout(1, 0, JBUI.scale(6), 0));
        verdicts.setBorder(JBUI.Borders.empty(0, 10, 10, 10));
        verdicts.setOpaque(false);

        for (final TestStatus status : TestStatus.values()) {
            if (!status.isVerdict()) continue;

            verdicts.add(new KeyBtn(keyOf(status), status.getLabel(), () -> judge(status)));
        }

        return verdicts;
    }

    private static @NotNull String keyOf(final @NotNull TestStatus status) {
        return Shortcuts.shortcutText(status.getMenuEntry().shortcut());
    }

    private @NotNull JComponent footer() {
        footer.setOpaque(false);
        footer.add(verdictRow, BorderLayout.NORTH);
        footer.add(durationStrip(), BorderLayout.CENTER);
        footer.add(statusBar.getPanel(), BorderLayout.SOUTH);

        return footer;
    }

    private @NotNull JComponent durationStrip() {
        strip.setBorder(JBUI.Borders.empty(0, 10, 8, 10));
        strip.setOpaque(false);
        strip.add(caseClock, BorderLayout.WEST);
        strip.add(runClock, BorderLayout.EAST);

        return strip;
    }

    private StatusBarItem @NotNull [] caseKeys() {
        final @NotNull List<StatusBarItem> items = new ArrayList<>();
        items.add(StatusBarShortcut.hint(Shortcuts.ToggleDetails.getShortcutText(), Bundle.message("shortcut.details")));
        items.add(StatusBarShortcut.hint(Shortcuts.Escape.getShortcutText(), Bundle.message("shortcut.close")));

        for (final TestStatus status : TestStatus.values()) {
            if (status.isVerdict()) items.add(StatusBarShortcut.hint(keyOf(status), status.getLabel()));
        }

        return items.toArray(new StatusBarItem[0]);
    }

    private StatusBarItem @NotNull [] commitKeys() {
        return new StatusBarItem[]{
                StatusBarShortcut.hint(Shortcuts.Enter.getShortcutText(), Bundle.message("shortcut.save.and.next")),
                StatusBarShortcut.hint(Shortcuts.Escape.getShortcutText(), Bundle.message("shortcut.cancel")),
                StatusBarShortcut.corrections()};
    }

    private static @NotNull JComponent iconBefore(final @NotNull Icon icon, final @NotNull JComponent text) {
        return JBUI.Panels.simplePanel(CaseDetails.GAP, 0).addToLeft(new JBLabel(icon)).addToCenter(text).andTransparent();
    }

    private static @NotNull JBLabel clock(final @NotNull String meaning) {
        final @NotNull JBLabel label = new JBLabel();
        label.setToolTipText(meaning);
        label.setFont(JBUI.Fonts.smallFont());
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        return label;
    }
}
