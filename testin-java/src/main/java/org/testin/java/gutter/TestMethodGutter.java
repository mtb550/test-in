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

package org.testin.java.gutter;

import org.testin.codegen.CodeOn;
import org.testin.util.FailureText;
import org.testin.util.Bundle;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestMethodGutter extends RelatedItemLineMarkerProvider implements DumbAware {
    private static @NotNull Optional<UUID> parseUuid(final @NotNull String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (final IllegalArgumentException notAnId) {
            return Optional.empty();
        }
    }

    // UC-CODEGEN-007, Rule-CODEGEN-028, Rule-CODEGEN-029
    @Override
    protected void collectNavigationMarkers(final @NotNull PsiElement element, final @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        final @NotNull Project p = element.getProject();

        if (!(element instanceof PsiJavaToken token) || token.getTokenType() != JavaTokenType.STRING_LITERAL) {
            return;
        }

        // Rule-CODEGEN-082
        if (!CodeOn.isOn(p)) return;

        Optional.ofNullable(PsiTreeUtil.getParentOfType(token, PsiLiteralExpression.class))
                .filter(TestMethodGutter::namesATestCase)
                .map(literal -> StringUtil.unquoteString(literal.getText()).trim())
                .flatMap(TestMethodGutter::parseUuid)
                .ifPresent(testCaseId -> result.add(new RelatedItemLineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Nodes.Related,
                psiElement -> Bundle.message("gutter.view.details"),
                (mouseEvent, psiElement) -> openViewPanel(p, testCaseId, methodName(psiElement)),
                GutterIconRenderer.Alignment.RIGHT,
                Collections::emptyList
        )));
    }

    // UC-CODEGEN-007, Rule-CODEGEN-029
    private static boolean namesATestCase(final @NotNull PsiLiteralExpression literal) {
        return Optional.ofNullable(PsiTreeUtil.getParentOfType(literal, PsiNameValuePair.class))
                .filter(pair -> "testName".equals(pair.getName()))
                .map(pair -> PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class))
                .filter(annotation -> annotation.hasQualifiedName("org.testng.annotations.Test"))
                .isPresent();
    }

    private static @NotNull String methodName(final @NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.getParentOfType(element, PsiMethod.class)).map(PsiMethod::getName).orElseGet(() -> Bundle.message("gutter.this.method"));
    }

    // UC-CODEGEN-007, Rule-CODEGEN-030, Rule-CODEGEN-069
    private void openViewPanel(final @NotNull Project p, final @NotNull UUID uuid, final @NotNull String methodName) {
        Logger.info("Searching for UUID: " + uuid);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                indexer.awaitIndexing();

                indexer.findTestCase(uuid).ifPresentOrElse(
                        dto -> {
                            Logger.info("Found in indexer: " + dto.getDescription());

                            ApplicationManager.getApplication().invokeLater(() ->
                                    ViewToolWindowFactory.showPanel(p, List.of(dto), dto.getParent().getPath2(), ViewPanel::focusDetailsTab));
                        },
                        () -> refuseMissingCase(p, uuid, methodName));

            } catch (final Exception ex) {
                Logger.error("Could not open the test case behind this mark: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(p, Notifier.class).error(p, Bundle.message("gutter.not.opened.title"), Bundle.message("gutter.not.opened.message", FailureText.of(ex)))
                );
            }
        });
    }

    // UC-CODEGEN-007, Rule-CODEGEN-069
    private static void refuseMissingCase(final @NotNull Project p, final @NotNull UUID uuid, final @NotNull String methodName) {
        Logger.info("No test case behind " + methodName + ": " + uuid);

        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(p, Notifier.class).softRefuse(p, Refused.NO_TEST_CASE_BEHIND_IT, methodName));
    }
}
