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

package org.testin.codegen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testproject.SaveTestinYml;
import org.testin.util.Bundle;
import org.testin.util.Once;

import java.util.Objects;
import java.util.Optional;

// Rule-CODEGEN-082
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CodeOn {
    private static final @NotNull Key<Boolean> SAID = Key.create("testin.codeOn.said");

    // Rule-CODEGEN-082
    public static boolean isOn(final @NotNull Project p) {
        return OptionalPlugin.JAVA.isAvailable() && whyOff(p).isEmpty();
    }

    // Rule-CODEGEN-082
    public static @NotNull Optional<String> whyOff(final @NotNull Project p) {
        final @NotNull String open = Services.getInstance(p, BoundTestProject.class).name();
        if (TestinYml.names(p, open)) return Optional.empty();

        final @NotNull String named = TestinYml.projectName(p);
        return Optional.of(named.isEmpty() ? Bundle.message("code.off.not.named") : Bundle.message("code.off.names.other", named, open));
    }

    // Rule-CODEGEN-082
    public static boolean isOffAndWarned(final @NotNull Project p) {
        if (OptionalPlugin.JAVA.isMissingAndWarned(p)) return true;

        final @NotNull Optional<String> why = whyOff(p);
        why.ifPresent(reason -> Services.getInstance(p, Notifier.class).softRefuse(p, reason));
        return why.isPresent();
    }

    // Rule-CODEGEN-082, Rule-CODEGEN-005
    public static boolean isOnOrWarnOnce(final @NotNull Project p) {
        if (!OptionalPlugin.JAVA.isAvailableOrWarnOnce(p)) return false;

        final @NotNull Optional<String> why = whyOff(p);
        if (why.isEmpty()) return true;

        if (Once.claim(p, SAID)) {
            final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
            notifier.infoWithActions(p, Bundle.message("code.off.title"), why.orElseThrow(),
                    notifier.action(Bundle.message("yml.save.name"), () -> SaveTestinYml.start(p)));
        }

        Logger.debug("Automation code left as it is: " + why.orElseThrow());
        return false;
    }

    // Rule-CODEGEN-082, Rule-TREE-PANEL-104
    public static boolean grayedWithReason(final @NotNull AnAction action, final @NotNull AnActionEvent e) {
        final @NotNull Presentation presentation = e.getPresentation();
        if (OptionalPlugin.JAVA.grayedWithReason(action, presentation)) return true;

        final @NotNull Presentation own = action.getTemplatePresentation();
        final @NotNull Optional<String> why = Optional.ofNullable(e.getProject()).flatMap(CodeOn::whyOff);
        if (why.isEmpty()) {
            presentation.setText(own.getText());
            presentation.setDescription(own.getDescription());
            return false;
        }

        presentation.setEnabled(false);
        presentation.setText(Bundle.message("code.needs", Objects.requireNonNullElse(own.getText(), "")));
        presentation.setDescription(why.orElseThrow());
        return false;
    }
}
