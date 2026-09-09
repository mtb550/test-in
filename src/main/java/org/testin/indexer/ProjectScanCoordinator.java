package org.testin.indexer;

import com.intellij.openapi.progress.ProgressIndicator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Serializes full project rescans while allowing normal scans to share the read lock.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ProjectScanCoordinator {

    private final @NotNull IndexingScanner scanner;
    private final @NotNull ReentrantReadWriteLock scanLock = new ReentrantReadWriteLock();

    void scan(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        scanLock.readLock().lock();
        try {
            scanner.scanProject(projectPath, indicator);
        } finally {
            scanLock.readLock().unlock();
        }
    }

    // UC-INTERNAL-003, Rule-INTERNAL-022
    void rescanExclusively(final @NotNull Path projectPath) {
        scanLock.writeLock().lock();
        try {
            scanner.scanProject(projectPath);
        } finally {
            scanLock.writeLock().unlock();
        }
    }
}
