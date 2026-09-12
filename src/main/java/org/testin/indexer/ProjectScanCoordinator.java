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
