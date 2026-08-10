package org.testin.indexer;

import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Serializes full project rescans while allowing normal scans to share the read lock.
 */
final class ProjectScanCoordinator {

    private final @NotNull IndexingScanner scanner;
    private final @NotNull ReentrantReadWriteLock scanLock = new ReentrantReadWriteLock();

    ProjectScanCoordinator(final @NotNull IndexingScanner scanner) {
        this.scanner = scanner;
    }

    void scan(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        scanLock.readLock().lock();
        try {
            scanner.scanProject(projectPath, indicator);
        } finally {
            scanLock.readLock().unlock();
        }
    }

    void scan(final @NotNull Path projectPath) {
        scanLock.readLock().lock();
        try {
            scanner.scanProject(projectPath);
        } finally {
            scanLock.readLock().unlock();
        }
    }

    void rescanExclusively(final @NotNull Path projectPath) {
        scanLock.writeLock().lock();
        try {
            scanner.scanProject(projectPath);
        } finally {
            scanLock.writeLock().unlock();
        }
    }
}
