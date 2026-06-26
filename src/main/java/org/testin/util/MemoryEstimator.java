package org.testin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testin.util.logger.Log;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemoryEstimator {
    private static final long KB = 1024;
    private static final long MAP_ENTRY_OVERHEAD = 256;
    private static final long TC_SIZE = 2 * KB;
    private static final long TR_SIZE = KB;
    private static final long DIR_SIZE = 512;

    public static long estimate(
            final Map<?, ?> testCasesById,
            final Map<?, ?> testRunsById,
            final Map<String, ?> testProjectsByPath,
            final Map<String, ?> testSetsByPath,
            final Map<String, ?> testRunDirsByPath,
            final Map<String, ?> testSetPackagesByPath,
            final Map<String, ?> testRunPackagesByPath,
            final Map<String, ?> testCasesMainDirsByPath,
            final Map<String, ?> testRunsMainDirsByPath,
            final Map<String, List<UUID>> testSetCaseIds,
            final Map<String, ?> testRunsByPath) {

        long total = 0;

        total += testCasesById.size() * (TC_SIZE + MAP_ENTRY_OVERHEAD);
        total += testRunsById.size() * (TR_SIZE + MAP_ENTRY_OVERHEAD);

        total += testProjectsByPath.size() * (DIR_SIZE + MAP_ENTRY_OVERHEAD);
        total += testSetsByPath.size() * (DIR_SIZE + MAP_ENTRY_OVERHEAD);
        total += testRunDirsByPath.size() * (DIR_SIZE + MAP_ENTRY_OVERHEAD);
        total += testSetPackagesByPath.size() * (DIR_SIZE + MAP_ENTRY_OVERHEAD);
        total += testRunPackagesByPath.size() * (DIR_SIZE + MAP_ENTRY_OVERHEAD);
        total += testCasesMainDirsByPath.size() * (DIR_SIZE + MAP_ENTRY_OVERHEAD);
        total += testRunsMainDirsByPath.size() * (DIR_SIZE + MAP_ENTRY_OVERHEAD);

        for (final List<UUID> ids : testSetCaseIds.values()) {
            total += MAP_ENTRY_OVERHEAD;
            total += 64;
            total += ids.size() * (64L + 16L);
        }

        total += testRunsByPath.size() * (TR_SIZE + MAP_ENTRY_OVERHEAD);

        return total;
    }

    public static long measureActual(final Runnable disposalRunnable) {
        forceGc();

        final Runtime rt = Runtime.getRuntime();
        final long usedBefore = rt.totalMemory() - rt.freeMemory();

        disposalRunnable.run();

        forceGc();

        final long usedAfter = rt.totalMemory() - rt.freeMemory();

        return Math.max(0, usedBefore - usedAfter);
    }

    public static long snapshotHeapUsage() {
        final Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    public static String formatBytes(final long bytes) {
        final long kb = bytes / KB;
        final long mb = kb / KB;
        return "~" + mb + " MB (" + kb + " KB)";
    }

    public static void logStats(
            final String tag,
            final int testCasesCount,
            final int testRunsCount,
            final int testProjectsCount,
            final int testSetsCount,
            final int testRunDirsCount,
            final int testSetPackagesCount,
            final int testRunPackagesCount,
            final int testCasesMainDirsCount,
            final int testRunsMainDirsCount,
            final int testSetCaseSetCount,
            final int testSetCaseTotalIds,
            final int testRunsByPathCount,
            final long estimatedBytes) {

        Log.info("=== " + tag + " ===");
        Log.info("Test cases by ID:     " + testCasesCount);
        Log.info("Test runs by ID:      " + testRunsCount);
        Log.info("Test projects:        " + testProjectsCount);
        Log.info("Test sets:            " + testSetsCount);
        Log.info("Test run dirs:        " + testRunDirsCount);
        Log.info("Test set packages:    " + testSetPackagesCount);
        Log.info("Test run packages:    " + testRunPackagesCount);
        Log.info("Test cases main dirs: " + testCasesMainDirsCount);
        Log.info("Test runs main dirs:  " + testRunsMainDirsCount);
        Log.info("Test set case IDs:    " + testSetCaseSetCount + " sets, " + testSetCaseTotalIds + " total IDs");
        Log.info("Test runs by path:    " + testRunsByPathCount);
        Log.info("Estimated RAM:        " + formatBytes(estimatedBytes));
        Log.info("==========================");
    }

    private static void forceGc() {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
