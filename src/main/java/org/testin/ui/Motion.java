package org.testin.ui;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.Animator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
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
     * UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-216.
     * <p>
     * Runs {@code step} with a fraction from just above 0 to exactly 1, eased,
     * and calls {@code done} on the last frame - unless the tester has turned
     * animation off, in which case {@code done} runs at once and nothing moves.
     * <p>
     * The answer is empty when nothing is moving, so a caller holding the
     * animator to cut a run short has nothing to cut and nothing to check. That
     * is the same shape the callers already had.
     * <p>
     * <b>Asked of the IDE, not of a setting of ours.</b> "Animate windows" on
     * the Appearance page is where a tester turns motion off, and somebody who
     * turned it off there means it - they are on a remote desktop, or they find
     * movement distracting, or they need the screen to hold still. A window that
     * floats above every other application is the worst place to ignore that,
     * and asking again in our own settings would be a second switch that
     * disagrees with the first.
     */
    public static @NotNull Optional<Animator> run(final @NotNull Disposable parent, final @NotNull String name, final @NotNull DoubleConsumer step, final @NotNull Runnable done) {
        if (!UISettings.getInstance().getAnimateWindows()) {
            done.run();
            return Optional.empty();
        }

        final @NotNull Animator animator = new Animator(name, FRAMES, DURATION_MS, false, true, parent) {
            @Override
            public void paintNow(final int frame, final int totalFrames, final int cycle) {
                final double fraction = Math.min(1.0, (frame + 1.0) / totalFrames);

                step.accept(ease(fraction));
                if (fraction >= 1.0) done.run();
            }
        };

        animator.resume();
        return Optional.of(animator);
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
