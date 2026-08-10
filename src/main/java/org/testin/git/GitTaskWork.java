package org.testin.git;

import com.intellij.openapi.progress.ProgressIndicator;

/**
 * Background body of a {@link GitBackgroundTask}; may throw, the task reports
 * the failure on the EDT.
 */
@FunctionalInterface
public interface GitTaskWork {
    void run(ProgressIndicator indicator) throws Exception;
}
