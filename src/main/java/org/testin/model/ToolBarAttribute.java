package org.testin.model;

import org.jetbrains.annotations.NotNull;

/**
 * An attribute the editor's Details popup can list: what it is called, and how
 * it starts out.
 * <p>
 * Both attribute enums implement it, so the popup works from the enum class
 * alone - it reads the name and the default off each constant instead of being
 * handed an accessor for each. A third editor needs no new plumbing.
 */
public interface ToolBarAttribute {

    @NotNull String getName();

    @NotNull ToolBarDefault getToolBarDefault();
}
