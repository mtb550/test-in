package org.testin.git;

/**
 * Failure of a {@link GitOperations} call. Unchecked so the interface stays
 * free of implementation-specific exception types (Git4Idea's VcsException,
 * process failures, ...); the git workflows already funnel errors to a single
 * handler per background task.
 */
public class GitOperationException extends RuntimeException {

    public GitOperationException(final String message) {
        super(message);
    }

    public GitOperationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
