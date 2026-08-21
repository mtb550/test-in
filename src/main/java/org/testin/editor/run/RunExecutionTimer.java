package org.testin.editor.run;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;

import javax.swing.*;
import java.time.Duration;

/**
 * Updates the duration of the currently executing run item on the EDT.
 */
final class RunExecutionTimer implements Disposable {

    /**
     * The ticker, and one that ticks nothing before a case is started and after
     * one is stopped - so stopping is always something that can be done, and the
     * item a finished timer was counting is let go of with it.
     */
    private @NotNull Timer timer = notTicking();
    private long startedAt;

    private static @NotNull Timer notTicking() {
        return new Timer(1000, ignored -> {
        });
    }

    /**
     * Counts on from what the case already carries rather than resetting it.
     * <p>
     * A tester who stops halfway and resumes is continuing the same case, so the
     * time already spent on it is part of its duration. Resetting to zero here
     * discarded that silently, and the case reported only the last sitting.
     */
    void start(final @NotNull TestRunItems item, final @NotNull Runnable repaint) {
        stop();

        final @NotNull Duration alreadyCounted = item.getDuration();
        startedAt = System.currentTimeMillis();

        timer = new Timer(1000, ignored -> {
            final long elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000;
            item.setDuration(alreadyCounted.plusSeconds(elapsedSeconds));
            repaint.run();
        });
        timer.start();
    }

    void stop() {
        timer.stop();
        timer = notTicking();
    }

    @Override
    public void dispose() {
        stop();
    }
}
