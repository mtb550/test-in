package org.testin.editor.run;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;

import javax.swing.*;
import java.time.Duration;
import java.util.Optional;

/**
 * Updates the duration of the currently executing run item on the EDT.
 * <p>
 * It times a tester working through a case by hand. What a framework measures
 * about a method it ran is a different measurement and a better one, and
 * {@code TestRunItems.recordDuration} lets it replace whatever this counted.
 */
final class RunExecutionTimer implements Disposable {

    /**
     * How often the card is redrawn while a case is being timed. It decides how
     * often the tester sees the number change, and nothing else: what is
     * recorded is the time that actually elapsed, measured when it is asked for.
     */
    private static final int REDRAW_MS = 1000;

    /**
     * The ticker, and one that ticks nothing before a case is started and after
     * one is stopped - so stopping is always something that can be done.
     */
    private @NotNull Timer timer = notTicking();

    private long startedAt;

    /**
     * The item being counted, empty when none is. Held so the last stretch can
     * be recorded when the clock stops, rather than only on a tick.
     */
    private @NotNull Optional<TestRunItems> counting = Optional.empty();

    /**
     * What the item already carried when counting began. A tester who stops
     * halfway and resumes is continuing the same case, so the time already
     * spent on it is part of its duration; resetting to zero discarded that
     * silently and the case reported only its last sitting.
     */
    private @NotNull Duration alreadyCounted = Duration.ZERO;

    private static @NotNull Timer notTicking() {
        return new Timer(REDRAW_MS, ignored -> {
        });
    }

    void start(final @NotNull TestRunItems item, final @NotNull Runnable repaint) {
        // Records the previous case's last stretch before letting go of it.
        stop();

        alreadyCounted = item.getDuration();
        counting = Optional.of(item);
        startedAt = System.currentTimeMillis();

        timer = new Timer(REDRAW_MS, ignored -> {
            elapse();
            repaint.run();
        });
        timer.start();
    }

    /**
     * Stops counting, and records what has elapsed since the last redraw.
     * <p>
     * That final write is the point. The ticker's first tick is a second after
     * the start, and a tester who judges a case faster than that used to get no
     * tick at all - so nothing was ever written and the case reported no
     * duration, which read as "this was never run".
     */
    void stop() {
        timer.stop();
        timer = notTicking();

        elapse();
        counting = Optional.empty();
    }

    /**
     * The time actually spent on the case so far, to the millisecond. Measured
     * from the clock rather than counted in ticks, so a missed or late tick
     * costs nothing.
     */
    private void elapse() {
        counting.ifPresent(item -> item.setDuration(alreadyCounted.plusMillis(System.currentTimeMillis() - startedAt)));
    }

    @Override
    public void dispose() {
        stop();
    }
}
