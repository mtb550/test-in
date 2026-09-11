/**
 * How everything looks: the dialog framework, the badges, the menus, the
 * animation, the zoom and the fonts - parts named for what they are on screen,
 * never for what they hold.
 * <p>
 * Told apart from its two neighbours by who it serves: {@code ui} serves every
 * surface, {@link org.testin.view} is one surface, and {@link org.testin.editor}
 * is another (#110). A class here is reached by an editor, a tree and a tool
 * window alike, which is the test of whether it belongs.
 */
package org.testin.ui;
