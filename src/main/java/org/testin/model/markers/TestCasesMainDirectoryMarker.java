package org.testin.model.markers;

import lombok.ToString;

/**
 * The test cases directory carries the audit block and nothing else. Still a
 * class of its own, because the node it belongs to is: the DTO's marker is
 * typed to it, so a runs marker cannot be handed to a test-cases directory.
 */
@ToString(callSuper = true)
public class TestCasesMainDirectoryMarker extends AbstractMarker {
}
