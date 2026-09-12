/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.services;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.util.Bundle;
import org.testin.util.Once;

import java.util.Objects;

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
            Bundle.message("plugin.java.requirement")
    ),

    TESTNG(
            "TestNG-J",
            "TestNG",
            Bundle.message("plugin.testng.requirement")
    ),

    GIT(
            "Git4Idea",
            "Git",
            Bundle.message("plugin.git.requirement")
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

    // Rule-CODEGEN-005
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
        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("plugin.not.available.title", label), requirement);
    }

    /**
     * UC-SHARE-010, Rule-SHARE-105.
     * <p>
     * The same, for an action, which already knows what it is called.
     * <p>
     * From the <b>template</b> presentation: it is the name the action was
     * constructed with and nothing ever writes to it, so reading it cannot pick
     * up a suffix an earlier pass appended. The three callers used to pass the
     * name as a literal, which put a string a tester reads in two places in one
     * file - and the copy is the one that keeps the old name when somebody
     * renames the entry.
     */
    public boolean enableOrExplain(final @NotNull AnAction action, final @NotNull Presentation presentation) {
        return enableOrExplain(presentation, Objects.requireNonNullElse(action.getTemplatePresentation().getText(), ""));
    }

    /**
     * UC-SHARE-010, Rule-SHARE-105.
     * <p>
     * Leaves a menu entry alone when the plugin is there, and grays it with the
     * reason written into it when it is not. Answers whether the caller should
     * go on deciding for itself.
     * <p>
     * Shown and grayed rather than left out. The two Git entries were simply not
     * added without the Git plugin, while Sync With SFTP beside them was added
     * in every IDE - so the menu had a different shape in two IDEs, with nothing
     * to say why, and a tester could not learn the Git integration existed at
     * all (#273).
     * <p>
     * The reason goes in the <b>text</b>, not only the description: a grayed
     * entry in a popup menu is not hovered, so a description nobody sees is the
     * same silence in a different place.
     * <p>
     * The name is given rather than read off the live presentation, because
     * {@code update} runs many times over one entry and appending to what is
     * already there would grow the label on every pass. An action takes the
     * overload above, which reads it from the template - the name it was
     * constructed with, which nothing ever writes to.
     */
    public boolean enableOrExplain(final @NotNull Presentation presentation, final @NotNull String entryName) {
        if (isAvailable()) return true;

        presentation.setEnabled(false);
        presentation.setText(Bundle.message("plugin.needs", entryName, label));
        presentation.setDescription(requirement);

        return false;
    }
}
