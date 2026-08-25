package org.testin.report.generators;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Joins the parts of a line that are actually there.
 * <p>
 * The reports print several pairs - the platform and the component, the project
 * and what it ran on - and each was built by putting a separator between two
 * values without asking whether both were set. A run with no platform came out
 * as ", Backend", and one with no component as "Web, ".
 * <p>
 * Six places did it, in three files, so a fix in one of them would have left the
 * other five printing the stray comma. Here instead: an empty part is not a part.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportText {

    /**
     * Where the plugin lives. All three reports sign themselves with a link to
     * it, and a marketplace id written out three times is two chances to fix
     * only some of them.
     */
    public static final @NotNull String PLUGIN_URL = "https://plugins.jetbrains.com/plugin/31514-testin";

    public static @NotNull String joined(final @NotNull String separator, final String @NotNull ... parts) {
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(separator));
    }
}
