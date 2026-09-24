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

package org.testin.editor.run;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;

import javax.swing.Timer;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

final class RunExecutionTimer implements Disposable {
    private static final int REDRAW_MS = 1000;

    private @NotNull Timer timer = notTicking();

    private long startedAt;

    private @NotNull Optional<TestRunItems> counting = Optional.empty();

    private @NotNull Duration alreadyCounted = Duration.ZERO;

    private static @NotNull Timer notTicking() {
        return new Timer(REDRAW_MS, _ -> {
        });
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132
    boolean isOn(final @NotNull UUID testCaseId) {
        return counting.filter(item -> item.getId().equals(testCaseId)).isPresent();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132
    void start(final @NotNull TestRunItems item, final @NotNull Runnable repaint) {
        stop();

        alreadyCounted = item.getDuration();
        counting = Optional.of(item);
        startedAt = System.currentTimeMillis();

        timer = new Timer(REDRAW_MS, _ -> {
            elapse();
            repaint.run();
        });
        timer.start();
    }

    // UC-EDITOR-PANEL-035
    void stop() {
        timer.stop();
        timer = notTicking();

        elapse();
        counting = Optional.empty();
    }

    // UC-EDITOR-PANEL-039, Rule-EDITOR-PANEL-164
    void discard() {
        timer.stop();
        timer = notTicking();

        counting.ifPresent(item -> item.recordClock(alreadyCounted));
        counting = Optional.empty();
    }

    private void elapse() {
        counting.ifPresent(item -> item.recordClock(alreadyCounted.plusMillis(System.currentTimeMillis() - startedAt)));
    }

    @Override
    public void dispose() {
        stop();
    }
}
