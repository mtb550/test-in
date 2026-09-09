package org.testin.ui;

import com.intellij.openapi.Disposable;
import com.intellij.util.ui.Animator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;

/**
 * How long anything in Testin takes to move, and the shape of the movement.
 * <p>
 * The plugin's first animation, and so the convention every later one copies.
 * It is one class rather than a timer at each call site for the reason #178
 * gives: a duration written out twice is two durations the moment somebody
 * tunes one of them, and a window whose height eases at one speed while its
 * content slides at another reads as two things happening rather than one.
 * <p>
 * <b>Built on the platform's {@link Animator}</b>, which is on the classpath
 * already, ticks on the EDT - which every Swing surface here requires - and
 * stops itself when the {@link Disposable} it is given goes. A hand-rolled
 * {@code javax.swing.Timer} would have to be told all three.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Motion {

    /**
     * Rule-EDITOR-PANEL-203.
     * <p>
     * How long a state change takes, everywhere.
     * <p>
     * 200ms rather than the 350 the light mode story first asked for. The keys
     * it sits behind are pressed once per test case - a hundred-case run is a
     * hundred of these - and the tester is watching the application under test
     * rather than the window, so the movement has to be seen without being
     * waited for. The platform's own transitions sit here for the same reason.
     */
    public static final int DURATION_MS = 200;

    /**
     * Frames per run: about sixty a second for the duration above, which is
     * smooth on every display and is not worth making a setting.
     */
    private static final int FRAMES = 12;

    /**
     * Runs {@code step} with a fraction from just above 0 to exactly 1, eased,
     * and calls {@code done} on the last frame.
     * <p>
     * The animator is handed back so a caller can cut a run short. Starting a
     * second movement while the first is still going is ordinary here - a
     * tester holding down a verdict key - and the answer is always to drop the
     * old one and start from wherever it had reached, never to queue.
     */
    public static @NotNull Animator run(final @NotNull Disposable parent, final @NotNull String name, final @NotNull DoubleConsumer step, final @NotNull Runnable done) {
        final @NotNull Animator animator = new Animator(name, FRAMES, DURATION_MS, false, true, parent) {
            @Override
            public void paintNow(final int frame, final int totalFrames, final int cycle) {
                final double fraction = Math.min(1.0, (frame + 1.0) / totalFrames);

                step.accept(ease(fraction));
                if (fraction >= 1.0) done.run();
            }
        };

        animator.resume();
        return animator;
    }

    /**
     * Fast to begin with and slowing into place - the curve every interface
     * uses for something arriving, because a movement that ends abruptly reads
     * as a jump that happened to take time.
     */
    private static double ease(final double fraction) {
        final double remaining = 1.0 - fraction;

        return 1.0 - remaining * remaining * remaining;
    }
}
