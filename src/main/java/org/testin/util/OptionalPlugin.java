package org.testin.util;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

/**
 * The IDE plugins Testin can run without. plugin.xml declares them as optional
 * dependencies so Testin installs in IDEs where they do not exist (PyCharm,
 * GoLand, WebStorm, ...); every feature that needs one checks here first,
 * keeping classes from a missing plugin out of the executed code paths.
 */
public enum OptionalPlugin {
    JAVA(
            "com.intellij.java",
            "Java",
            "Automation code generation and navigation require the Java plugin, which is not available in this IDE."
    ),

    TESTNG(
            "TestNG-J",
            "TestNG",
            "Executing tests requires the TestNG plugin, which is not available in this IDE."
    ),

    GIT(
            "Git4Idea",
            "Git",
            "Git synchronization and cloning require the Git plugin, which is not available in this IDE."
    );

    private final @NotNull String pluginId;
    private final @NotNull String label;
    private final @NotNull String requirement;
    private final @NotNull Key<Boolean> warned;
    /**
     * Whether the IDE has this plugin, once asked. Three states rather than a
     * nullable Boolean: "not looked yet" is a state of its own, and saying so
     * with a constant means nothing has to test for a missing answer (#71).
     */
    private volatile @NotNull Availability availability = Availability.UNKNOWN;

    OptionalPlugin(final @NotNull String pluginId, final @NotNull String label, final @NotNull String requirement) {
        this.pluginId = pluginId;
        this.label = label;
        this.requirement = requirement;
        this.warned = Key.create("testin.optionalPlugin.warned." + pluginId);
    }

    public boolean isAvailable() {
        Availability known = availability;
        if (known == Availability.UNKNOWN) {
            // Enabled = installed and not disabled; a disabled plugin's classes
            // are just as absent as an uninstalled one's.
            known = isEnabledInIde() ? Availability.PRESENT : Availability.ABSENT;
            availability = known;
        }
        return known == Availability.PRESENT;
    }

    /**
     * Whether the IDE has this plugin enabled, asked as the two things that
     * makes: it is installed, and it has not been switched off.
     * <p>
     * Said in two public questions rather than one internal one.
     * {@code PluginManager.findEnabledPlugin} answers both at once and is
     * marked internal on the 2026.2 branch, where the verifier reports it;
     * {@code PluginManager.getPlugin} is deprecated and
     * {@code PluginManagerCore.findPlugin} is internal too. These two are
     * neither, and they return booleans - so the answer no longer arrives as a
     * descriptor that has to be tested for null.
     */
    private boolean isEnabledInIde() {
        final @NotNull PluginId id = PluginId.getId(pluginId);

        return PluginManagerCore.isPluginInstalled(id) && !PluginManagerCore.isDisabled(id);
    }

    /**
     * What is known about a plugin: nothing yet, or the answer.
     */
    private enum Availability {
        UNKNOWN, PRESENT, ABSENT
    }

    /**
     * True when available; otherwise notifies on every call — for explicit user
     * actions (run, navigate, sync) that must always visibly respond.
     */
    // Always called as "if (!isAvailableOrWarn(p)) return;", which is what the
    // inspection objects to. Inverting it to isUnavailable...() would make the
    // name say the opposite of what the method returns on the happy path, and
    // every call site reads as a guard. Kept as it is.
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isAvailableOrWarn(final @NotNull Project p) {
        if (isAvailable()) return true;
        warn(p);
        return false;
    }

    /**
     * Like {@link #isAvailableOrWarn} but notifies once per project — for
     * implicit skips (code generation) that would otherwise spam.
     */
    public boolean isAvailableOrWarnOnce(final @NotNull Project p) {
        if (isAvailable()) return true;
        if (Once.claim(p, warned)) warn(p);
        return false;
    }

    private void warn(final @NotNull Project p) {
        Services.getInstance(p, Notifier.class).softRefuse(p, label + " Plugin Not Available", requirement);
    }
}
