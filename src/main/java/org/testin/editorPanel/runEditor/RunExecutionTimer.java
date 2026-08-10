package org.testin.editorPanel.runEditor;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.TestRunItems;

import javax.swing.*;
import java.time.Duration;

/**
 * Updates the duration of the currently executing run item on the EDT.
 */
final class RunExecutionTimer implements Disposable {

    private Timer timer;
    private long startedAt;

    void start(final @NotNull TestRunItems item, final @NotNull Runnable repaint) {
        stop();
        item.setDuration(Duration.ZERO);
        startedAt = System.currentTimeMillis();
        timer = new Timer(1000, ignored -> {
            final long elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000;
            item.setDuration(Duration.ofSeconds(elapsedSeconds));
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
