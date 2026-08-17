package org.testin.editor.run;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.TestRunItems;

import javax.swing.*;
import java.time.Duration;

/**
 * Updates the duration of the currently executing run item on the EDT.
 */
final class RunExecutionTimer implements Disposable {

    private @Nullable Timer timer;
    private long startedAt;

    /**
     * Counts on from what the case already carries rather than resetting it.
     * <p>
     * A tester who stops halfway and resumes is continuing the same case, so the
     * time already spent on it is part of its duration. Resetting to zero here
     * discarded that silently, and the case reported only the last sitting.
     */
    void start(final @NotNull TestRunItems item, final @NotNull Runnable repaint) {
        stop();

        final Duration alreadyCounted = item.getDuration();
        startedAt = System.currentTimeMillis();

        timer = new Timer(1000, ignored -> {
            final long elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000;
            item.setDuration(alreadyCounted.plusSeconds(elapsedSeconds));
            repaint.run();
        });
        timer.start();
    }

    void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    @Override
    public void dispose() {
        stop();
    }
}
