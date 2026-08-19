package org.testin.model.markers;

import lombok.ToString;

/**
 * The test cases directory carries the audit block and nothing else.
 * <p>
 * It is still a class of its own, because the node it belongs to is one. The
 * DTO types its marker field to this class, so a runs marker cannot be handed
 * to a test-cases directory.
 */
@ToString(callSuper = true)
public class TestCasesMainDirectoryMarker extends AbstractMarker {
}
