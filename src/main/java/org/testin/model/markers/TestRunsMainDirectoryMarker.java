package org.testin.model.markers;

/**
 * The test runs directory carries the audit block and nothing else. Still a
 * class of its own, because the node it belongs to is: the DTO's marker is
 * typed to it, so a test cases marker cannot be handed to a runs directory.
 */
public class TestRunsMainDirectoryMarker extends AbstractMarker {
}
