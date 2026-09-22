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

package org.testin.java.codegen.method;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ExecutionPosition;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.JavaSourceRoot;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.JavaLiteral;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.NameSanitizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CreateTestMethod implements GenAction {
    private static final @NotNull String TESTNG_TEST = "org.testng.annotations.Test";

    static @NotNull Optional<Target> parse(final @NotNull List<String> fqcn) {
        if (fqcn.size() < 2) return Optional.empty();

        return Optional.of(new Target(
                String.join(".", fqcn.subList(0, fqcn.size() - 1)),
                fqcn.subList(0, fqcn.size() - 2),
                fqcn.get(fqcn.size() - 2),
                fqcn.getLast()));
    }

    private static @NotNull String noMethodTitle(final @NotNull List<TestCaseDto> without) {
        return without.size() == 1
                ? Bundle.message("codegen.no.method.one")
                : Bundle.message("codegen.no.method.many", String.valueOf(without.size()));
    }

    private static @NotNull String named(final @NotNull List<TestCaseDto> cases) {
        final @NotNull String names = cases.stream().limit(3).map(TestCaseDto::getDescription).collect(Collectors.joining("\", \"", "\"", "\""));

        return cases.size() > 3 ? Bundle.message("codegen.named.and.more", names, String.valueOf(cases.size() - 3)) : names;
    }

    private static boolean alreadyImportsTest(final @NotNull PsiImportList imports) {
        return imports.findSingleClassImportStatement(TESTNG_TEST) != null;
    }

    private static @NotNull String testMethods(final int howMany) {
        return howMany + " test method" + (howMany == 1 ? "" : "s");
    }

    // UC-CODEGEN-002, Rule-CODEGEN-014, Rule-CODEGEN-046
    private static @NotNull String methodText(final @NotNull Project p, final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        final @NotNull StringBuilder attributes = new StringBuilder();

        if (!tc.getGroup().isEmpty()) {
            final @NotNull List<String> quoted = tc.getGroup().stream().map(JavaLiteral::of).toList();

            attributes.append(", groups = {").append(String.join(", ", quoted)).append("}");
        }

        attributes.append(", priority = ").append(ExecutionPosition.of(p, tc));

        final @NotNull String annotation = String.format("@Test(description = %s, testName = \"%s\"%s)",
                JavaLiteral.of(tc.getDescription()),
                tc.getId(),
                attributes);

        return annotation + "\npublic void " + methodName + "() {\n    // TODO: Auto-generated test steps for "
                + methodName + "\n}";
    }

    // UC-CODEGEN-002
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        executeAll(p, List.of(tc));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-018
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        final @NotNull Map<String, List<TestCaseDto>> byClass = new LinkedHashMap<>();

        for (final Object item : items) {
            if (!(item instanceof TestCaseDto tc)) continue;

            final @NotNull List<String> fqcn = Fqcn.ofMethod(tc);
            parse(fqcn).ifPresentOrElse(
                    target -> byClass.computeIfAbsent(target.path(), ignored -> new ArrayList<>()).add(tc),
                    () -> noMethodFor(tc, fqcn));
        }

        WriteCommandAction.runWriteCommandAction(p, GenType.CREATE_TEST_CASE.getDescription(), null,
                () -> byClass.values().forEach(group -> createMethods(p, group)));
    }

    private void noMethodFor(final @NotNull TestCaseDto tc, final @NotNull List<String> fqcn) {
        if (fqcn.isEmpty()) Logger.debug("No description yet, so no method to write for " + tc.getId());
        else Logger.error("FQCN list is too short to generate a method: " + fqcn);
    }

    private void createMethods(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        final @NotNull Optional<Target> first = parse(Fqcn.ofMethod(cases.getFirst()));
        if (first.isEmpty()) return;

        final @NotNull Target target = first.orElseThrow();
        Logger.info("Creating " + testMethods(cases.size()) + " in " + target.path());

        GeneratedClass.findOrWrite(p, target.packageList(), target.className()).ifPresentOrElse(
                targetClass -> injectAsText(p, targetClass, cases),
                () -> cases.forEach(tc -> retryInjectPhysically(p, target.packageList(), target.className(),
                        Fqcn.methodNameOf(tc), tc)));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-016
    private void injectAsText(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> cases) {
        final @NotNull PsiFile file = targetClass.getContainingFile();
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(p);
        final @NotNull Optional<Document> document = Optional.ofNullable(documents.getDocument(file));

        if (document.isEmpty()) {
            oneAtATime(p, targetClass, cases, "it has no document to edit");
            return;
        }

        final @NotNull Map<String, String> owners = new HashMap<>();
        final @NotNull Map<String, PsiMethod> byKey = new HashMap<>();
        for (final PsiMethod pm : targetClass.getMethods()) {
            final @NotNull String methodKey = NameSanitizer.methodKey(pm.getName());

            owners.putIfAbsent(methodKey, GeneratedMethod.caseIdOf(pm).orElse(""));
            byKey.putIfAbsent(methodKey, pm);
        }

        final @NotNull Map<String, PsiMethod> generated = GeneratedMethod.byCaseId(targetClass);

        final @NotNull StringBuilder methods = new StringBuilder();
        final @NotNull List<TestCaseDto> lostTheName = new ArrayList<>();
        final @NotNull List<TestCaseDto> cannotBeNamed = new ArrayList<>();
        int alreadyThere = 0;
        int adopted = 0;

        for (final TestCaseDto tc : cases) {
            final @NotNull String id = tc.getId().toString();

            if (generated.containsKey(id)) {
                alreadyThere++;
                continue;
            }

            if (NameSanitizer.cannotMakeMethodName(tc.getDescription())) {
                cannotBeNamed.add(tc);
                continue;
            }

            final @NotNull String methodName = Fqcn.methodNameOf(tc);
            final @NotNull String key = NameSanitizer.methodKey(methodName);
            final @NotNull Optional<String> owner = Optional.ofNullable(owners.get(key));

            if (owner.isPresent()) {
                final @NotNull Optional<PsiMethod> theTestersOwn = Optional.ofNullable(byKey.get(key))
                        .filter(pm -> owner.orElseThrow().isEmpty() && GeneratedMethod.testAnnotationOf(pm).isPresent());

                if (theTestersOwn.isPresent()) {
                    GeneratedMethod.adopt(p, theTestersOwn.orElseThrow(), tc);
                    owners.put(key, id);
                    adopted++;
                } else lostTheName.add(tc);
                continue;
            }

            owners.put(key, id);
            methods.append('\n').append(methodText(p, methodName, tc)).append('\n');
        }

        if (alreadyThere > 0) {
            Logger.info(alreadyThere + " of " + testMethods(cases.size())
                    + " already in " + targetClass.getQualifiedName());
        }

        if (adopted > 0) {
            Logger.info("Linked " + adopted + " hand-written method(s) in "
                    + targetClass.getQualifiedName() + " to the cases that describe them");
        }

        reportLostTheName(p, targetClass, lostTheName);
        reportCannotBeNamed(p, targetClass, cannotBeNamed);

        if (methods.isEmpty()) return;

        if (file instanceof PsiJavaFile javaFile) addTestImport(p, javaFile, JavaPsiFacade.getElementFactory(p));

        documents.doPostponedOperationsAndUnblockDocument(document.orElseThrow());

        final @NotNull Optional<PsiElement> closingBrace = Optional.ofNullable(targetClass.getRBrace());
        if (closingBrace.isEmpty()) {
            oneAtATime(p, targetClass, cases, "it has no closing brace to write before");
            return;
        }

        final int insertAt = closingBrace.orElseThrow().getTextRange().getStartOffset();
        document.orElseThrow().insertString(insertAt, methods);
        documents.commitDocument(document.orElseThrow());

        CodeStyleManager.getInstance(p).reformatText(file, insertAt, insertAt + methods.length());
    }

    // UC-CODEGEN-002, Rule-CODEGEN-011
    private void reportCannotBeNamed(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> cannotBeNamed) {
        if (cannotBeNamed.isEmpty()) return;

        Logger.warn("No method for " + cannotBeNamed.size() + " case(s) in " + targetClass.getQualifiedName()
                + ": the description cannot name a Java method");

        Services.getInstance(p, Notifier.class).warn(p, noMethodTitle(cannotBeNamed),
                Bundle.message("codegen.cannot.name.message", named(cannotBeNamed)));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-001
    private void reportLostTheName(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> lost) {
        if (lost.isEmpty()) return;

        Logger.warn("No method for " + lost.size() + " case(s) in " + targetClass.getQualifiedName()
                + ": the name is already taken by another method");

        Services.getInstance(p, Notifier.class).warn(p, noMethodTitle(lost),
                Bundle.message("codegen.name.taken.message", named(lost)));
    }

    private void oneAtATime(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull List<TestCaseDto> cases, final @NotNull String reason) {
        Logger.warn("Writing " + cases.size() + " methods one at a time into "
                + targetClass.getQualifiedName() + ": " + reason);

        int written = 0;
        for (final TestCaseDto tc : cases) {
            if (injectMethod(p, targetClass, Fqcn.methodNameOf(tc), tc).isPresent()) written++;
        }

        if (written > 0) CodeStyleManager.getInstance(p).reformat(targetClass);
    }

    private void retryInjectPhysically(final @NotNull Project p, final @NotNull List<String> packageList, final @NotNull String className, final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        JavaSourceRoot.find(p).ifPresentOrElse(
                sourceRoot -> injectIntoFile(p, sourceRoot, packageList, className, methodName, tc),
                () -> Logger.error("retryInjectPhysically: no Java test source root, cannot inject method '"
                        + methodName + "'"));
    }

    private void injectIntoFile(final @NotNull Project p, final @NotNull VirtualFile sourceRoot, final @NotNull List<String> packageList, final @NotNull String className, final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        try {
            final @NotNull String relativePath = String.join("/", packageList) + "/" + className + ".java";
            final @NotNull Optional<VirtualFile> found = JavaSourceRoot.under(sourceRoot, relativePath);
            if (found.isEmpty()) {
                Logger.error("retryInjectPhysically: file not found at " + relativePath + " for method '" + methodName + "'");
                return;
            }

            if (!(PsiManager.getInstance(p).findFile(found.orElseThrow()) instanceof PsiJavaFile javaPsiFile)) {
                Logger.error("retryInjectPhysically: file " + className + ".java is not a valid Java file for method '" + methodName + "'");
                return;
            }

            final PsiClass @NotNull [] classes = javaPsiFile.getClasses();
            if (classes.length == 0) {
                Logger.error("retryInjectPhysically: no classes found in " + className + ".java for method '" + methodName + "'");
                return;
            }

            injectMethod(p, classes[0], methodName, tc)
                    .ifPresent(added -> CodeStyleManager.getInstance(p).reformat(added));

        } catch (final Exception ex) {
            Logger.error("retryInjectPhysically failed for method '" + methodName + "': " + ex.getMessage());
        }
    }

    // UC-CODEGEN-002
    private void addTestImport(final @NotNull Project p, final @NotNull PsiJavaFile javaFile, final @NotNull PsiElementFactory factory) {
        Optional.ofNullable(javaFile.getImportList())
                .filter(imports -> !alreadyImportsTest(imports))
                .ifPresent(imports -> Optional
                        .ofNullable(JavaPsiFacade.getInstance(p).findClass(TESTNG_TEST, GlobalSearchScope.allScope(p)))
                        .ifPresent(testClass -> imports.add(factory.createImportStatement(testClass))));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-016
    private @NotNull Optional<PsiElement> injectMethod(final @NotNull Project p, final @NotNull PsiClass targetClass, final @NotNull String methodName, final @NotNull TestCaseDto tc) {
        try {
            final @NotNull PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);
            final @NotNull PsiFile file = targetClass.getContainingFile();

            if (file instanceof PsiJavaFile javaFile) addTestImport(p, javaFile, factory);

            if (GeneratedMethod.forCase(targetClass, tc).isPresent()) {
                Logger.info("Method already exists: " + methodName);
                return Optional.empty();
            }

            final @NotNull String key = NameSanitizer.methodKey(methodName);
            final @NotNull Optional<PsiMethod> sameName = Arrays.stream(targetClass.getMethods())
                    .filter(pm -> key.equals(NameSanitizer.methodKey(pm.getName())))
                    .findFirst();

            if (sameName.isPresent()) {
                if (GeneratedMethod.caseIdOf(sameName.orElseThrow()).isPresent() || GeneratedMethod.testAnnotationOf(sameName.orElseThrow()).isEmpty())
                    reportLostTheName(p, targetClass, List.of(tc));
                else
                    Logger.info("Method already exists: " + methodName);

                return Optional.empty();
            }

            final @NotNull PsiMethod newMethod = factory.createMethodFromText(methodText(p, methodName, tc), targetClass);
            final @NotNull PsiElement addedElement = targetClass.add(newMethod);

            Logger.info("Injected method: " + methodName + " with Priority: " + tc.getPriority().getLabel());
            return Optional.of(addedElement);

        } catch (final Exception ex) {
            Logger.error("injectMethod failed for '" + methodName + "': " + ex.getMessage());
            return Optional.empty();
        }
    }

    record Target(@NotNull String path, @NotNull List<String> packageList, @NotNull String className,
                  @NotNull String methodName) {
    }
}