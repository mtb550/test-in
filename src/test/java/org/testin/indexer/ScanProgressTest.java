package org.testin.indexer;

import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * A rescan started from a progress bar fills that bar in and stops when the
 * tester presses Cancel (#20).
 * <p>
 * The bar was there before this and reported nothing: the task built an
 * indicator, never handed it to the scan, and the scan was given a fresh
 * {@code EmptyProgressIndicator} instead. So the tester watched a title with no
 * test set named under it, over a Cancel button that did nothing - a promise
 * the window made and the code did not keep.
 * <p>
 * Checked by reading the sources, for the reason {@link
 * org.testin.LightServiceContractTest} gives: what went wrong is a wire that was
 * not connected, and an unconnected wire compiles. There is no seam to assert
 * against at runtime without the platform, a real project on disk and a tester
 * to press the button.
 */
public class ScanProgressTest {

    private static final @NotNull Path SOURCE_ROOT = Paths.get("src", "main", "java", "org", "testin", "indexer");

    /**
     * The scan takes an indicator at all - the overload the bar needs. Asked of
     * the class rather than of its source, because this one the compiler can be
     * made to care about.
     */
    @Test
    public void theScanCanBeGivenAProgressBar() {
        try {
            ProjectIndexer.class.getDeclaredMethod("scanSingleProject", Path.class, ProgressIndicator.class);
        } catch (final NoSuchMethodException missing) {
            fail("ProjectIndexer has no scanSingleProject(Path, ProgressIndicator): "
                    + "a caller with a progress bar has nowhere to hand it, so the bar reports nothing");
        }
    }

    /**
     * The rescan passes its own indicator down. Reading the file is the point:
     * calling the one-argument overload compiles perfectly and silently throws
     * the bar away, which is exactly the defect.
     */
    @Test
    public void theRescanHandsItsIndicatorToTheScan() {
        final @NotNull String source = read("Rescan.java");

        assertTrue(source.contains("scanSingleProject(testProject, indicator)"),
                "Rescan builds a progress bar and must hand it to the scan; calling the overload without it "
                        + "leaves the tester watching a bar that names nothing and cannot be stopped");
    }

    /**
     * Both directory loops ask whether the tester has cancelled. One check per
     * loop, because a loop that never asks cannot be stopped however cancellable
     * the task above it claims to be.
     */
    @Test
    public void bothScanLoopsStopWhenTheTesterCancels() {
        final @NotNull String source = read("IndexingScanner.java");
        final int checks = countOf(source, "indicator.isCanceled()");

        assertTrue(checks >= 2,
                "the test-set loop and the test-run loop must each check indicator.isCanceled(), found "
                        + checks + ": a scan that never asks turns Cancel into a button that does nothing");
    }


    /**
     * How many times a literal appears. Counted rather than split on, so the
     * thing being looked for is written exactly as it is in the file instead of
     * as a regular expression with everything escaped twice.
     */
    private static int countOf(final @NotNull String source, final @NotNull String literal) {
        int found = 0;
        int at = source.indexOf(literal);

        while (at >= 0) {
            found++;
            at = source.indexOf(literal, at + literal.length());
        }

        return found;
    }

    private static @NotNull String read(final @NotNull String fileName) {
        final @NotNull Path file = SOURCE_ROOT.resolve(fileName);

        try {
            return Files.readString(file);
        } catch (final IOException notThere) {
            fail("Could not read " + file.toAbsolutePath() + ": " + notThere.getMessage());
            return "";
        }
    }
}
