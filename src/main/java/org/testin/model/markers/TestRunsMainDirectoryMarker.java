package org.testin.model.markers;

import lombok.ToString;

/**
 * The test runs directory carries the audit block and nothing else.
 * <p>
 * It is still a class of its own, because the node it belongs to is one. The
 * DTO types its marker field to this class, so a test-cases marker cannot be
 * handed to a runs directory.
 */
@ToString(callSuper = true)
public class TestRunsMainDirectoryMarker extends AbstractMarker {
}
