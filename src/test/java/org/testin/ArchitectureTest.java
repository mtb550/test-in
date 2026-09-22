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

package org.testin;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTest {

    private static final @NotNull JavaClasses CLASSES = productionClasses();
    private static final String @NotNull [] ABOVE_MODEL = {
            "org.testin.indexer..", "org.testin.editor..", "org.testin.view..", "org.testin.codegen..",
            "org.testin.services..", "org.testin.creator..", "org.testin.explorer..",
            "org.testin.ui..", "org.testin.git..", "org.testin.report..",
            "org.testin.importexport..", "org.testin.testcase..", "org.testin.testrun..", "org.testin.testproject..",
            "org.testin.search..", "org.testin.undo..", "org.testin.rename..", "org.testin.remove..",
            "org.testin.open..", "org.testin.clipboard..", "org.testin.runner..", "org.testin.notifications..",
            "org.testin.setting..", "org.testin.config..", "org.testin.actions..", "org.testin.bug.."
    };
    private static final @NotNull Set<String> MODEL_LEAF_EXCEPTIONS = Set.of();
    private static final String @NotNull [] FEATURES = {
            "org.testin.indexer..", "org.testin.editor..", "org.testin.view..", "org.testin.codegen..",
            "org.testin.creator..", "org.testin.explorer..", "org.testin.ui..",
            "org.testin.git..", "org.testin.report..", "org.testin.importexport..",
            "org.testin.testcase..", "org.testin.testrun..", "org.testin.testproject..", "org.testin.search..",
            "org.testin.undo..", "org.testin.rename..", "org.testin.remove..", "org.testin.open..",
            "org.testin.clipboard..", "org.testin.runner..", "org.testin.bug.."
    };
    private static final @NotNull Set<String> UTIL_EXCEPTIONS = Set.of();
    private static final @NotNull Set<String> FILE_ACCESS_EXCEPTIONS = Set.of();

    private static @NotNull JavaClasses productionClasses() {
        final @NotNull Path compiled = Path.of("build", "classes", "java", "main").toAbsolutePath();

        if (!Files.isDirectory(compiled)) {
            throw new AssertionError("No compiled classes at " + compiled + " - the architecture rules have"
                    + " nothing to read. Run this through Gradle, which compiles src/main first.");
        }

        return new ClassFileImporter().importPath(compiled);
    }

    private static @NotNull DescribedPredicate<JavaClass> notOneOf(final @NotNull Set<String> frozen) {
        return new DescribedPredicate<>("not one of the " + frozen.size() + " frozen violations") {
            @Override
            public boolean test(final @NotNull JavaClass javaClass) {
                final @NotNull String outermost = javaClass.getName().split("[$]")[0];

                return !frozen.contains(outermost);
            }
        };
    }

    @Test
    public void modelImportsNothingAboveIt() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideInAPackage("org.testin.model..")
                .and(notOneOf(MODEL_LEAF_EXCEPTIONS))
                .should().dependOnClassesThat().resideInAnyPackage(ABOVE_MODEL)
                .because("model is a leaf: the DTOs, markers and enums are what every other package reads,"
                        + " so a model class reaching back into a feature makes the two impossible to move apart (#111)."
                        + " If this is a class the leaf rule should not apply to, say so on #111 rather than here");

        rule.check(CLASSES);
    }

    @Test
    public void utilImportsNoFeaturePackage() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideInAPackage("org.testin.util..")
                .and(notOneOf(UTIL_EXCEPTIONS))
                .should().dependOnClassesThat().resideInAnyPackage(FEATURES)
                .because("util is what everything else may call, so a util class that calls a feature back"
                        + " is a cycle waiting for its second edge (#112)");

        rule.check(CLASSES);
    }

    @Test
    public void onlyTheIndexerAndItsExemptListTouchFiles() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideOutsideOfPackages("org.testin.indexer..", "org.testin.codegen..",
                        "org.testin.config..", "org.testin.git..", "org.testin.importexport..",
                        "org.testin.report..", "org.testin.setting..", "org.testin.logger..",
                        "org.testin.bug..")
                .and(notOneOf(FILE_ACCESS_EXCEPTIONS))
                .should().dependOnClassesThat().haveFullyQualifiedName("java.nio.file.Files")
                .because("the indexer is the single owner of file access, so its cache stays authoritative over"
                        + " test data and every read is a fast in-memory lookup. The exempt packages are exempt"
                        + " because none of them touches test data (CLAUDE.md, #49)");

        rule.check(CLASSES);
    }

    @Test
    public void onlyTestinYmlReadsTheConfigFile() {
        final @NotNull ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName("org.testin.config.TestinYml")
                .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson.dataformat.yaml..")
                .because("testin.yml is read by one class, so a missing value means the same thing everywhere,"
                        + " and nothing else can open, parse or write the file (Rule-INTERNAL-089)");

        rule.check(CLASSES);
    }

    @Test
    public void onlyTheSaveButtonWritesTheConfigFile() {
        final @NotNull ArchRule oneCaller = methods()
                .that().areDeclaredIn("org.testin.config.TestinYml")
                .and().haveName("save")
                .should().onlyBeCalled().byClassesThat().haveFullyQualifiedName("org.testin.testproject.SaveTestinYml")
                .because("Save to testin.yml is the only gesture that writes the file, so cloning, picking a test"
                        + " project, creating one and renaming one leave a committed file alone (Decision-013)."
                        + " A second caller is a second gesture writing it");

        final @NotNull ArchRule oneWriter = noClasses()
                .that().doNotHaveFullyQualifiedName("org.testin.config.TestinYml")
                .and().doNotHaveFullyQualifiedName("org.testin.codegen.JavaSourceRoot")
                .should().callMethodWhere(target(name("createChildData")))
                .because("TestinYml holds the only write of testin.yml and JavaSourceRoot the only write of"
                        + " generated code; everything else asks the indexer, whose cache is authoritative"
                        + " (CLAUDE.md, Decision-013)");

        oneCaller.check(CLASSES);
        oneWriter.check(CLASSES);
    }

    @Test
    public void theRuleThisFileDoesNotEnforce() {
        final @NotNull List<String> deliberate = CLASSES.stream()
                .filter(javaClass -> javaClass.getPackageName().equals("org.testin.model"))
                .filter(javaClass -> javaClass.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> dependency.getTargetClass().getPackageName().startsWith("javax.swing")
                                || dependency.getTargetClass().getPackageName().startsWith("java.awt")))
                .map(JavaClass::getSimpleName)
                .sorted()
                .toList();

        Assert.assertFalse(deliberate.isEmpty(),
                "If model no longer carries any Swing or AWT type, the convention has changed and the fourth"
                        + " rule is now worth writing. Read this method's javadoc before deleting it.");
    }
}
