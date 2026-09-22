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
            known = isEnabledInIde() ? Availability.PRESENT : Availability.ABSENT;
            availability = known;
        }
        return known == Availability.PRESENT;
    }

    private boolean isEnabledInIde() {
        final @NotNull PluginId id = PluginId.getId(pluginId);

        return PluginManagerCore.isPluginInstalled(id) && !PluginManagerCore.isDisabled(id);
    }

    public boolean isMissingAndWarned(final @NotNull Project p) {
        if (isAvailable()) return false;
        warn(p);
        return true;
    }

    public boolean isAvailableOrWarnOnce(final @NotNull Project p) {
        if (isAvailable()) return true;
        if (Once.claim(p, warned)) warn(p);
        return false;
    }

    private void warn(final @NotNull Project p) {
        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("plugin.not.available.title", label), requirement);
    }

    // UC-SHARE-010, Rule-SHARE-105
    public boolean grayedWithReason(final @NotNull AnAction action, final @NotNull Presentation presentation) {
        return !enableOrExplain(presentation, Objects.requireNonNullElse(action.getTemplatePresentation().getText(), ""));
    }

    // UC-SHARE-010, Rule-SHARE-105
    public boolean enableOrExplain(final @NotNull Presentation presentation, final @NotNull String entryName) {
        if (isAvailable()) return true;

        presentation.setEnabled(false);
        presentation.setText(needs(entryName));
        presentation.setDescription(requirement);

        return false;
    }

    // UC-CODEGEN-016, Rule-CODEGEN-062
    public @NotNull String needs(final @NotNull String entryName) {
        return Bundle.message("plugin.needs", entryName, label);
    }

    private enum Availability {
        UNKNOWN, PRESENT, ABSENT
    }
}
