package org.testin;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The architecture rules, read off the bytecode instead of off CLAUDE.md (#114).
 * <p>
 * They were prose, and prose gets re-litigated by hand. #49 is an entire story
 * about auditing indexer-rule callers after the fact, and every sweep since has
 * found drift a test would have refused at commit time. Two of them shaped
 * decisions in a single afternoon: #175's C4 could not put an {@code EditorType}
 * on {@code DirectoryType} and its C11 could put a panel on {@code ViewTab}, and
 * both answers came from reading package names by hand rather than from anything
 * the build would have said.
 * <p>
 * <b>Known violations are frozen by name, with the issue that owns each.</b> A
 * rule that quietly excluded a package would document nothing; a list that names
 * six files says exactly what is wrong and where it is being fixed. Deleting a
 * name from one of these lists is how a story closes.
 * <p>
 * The fourth rule #114 asked for - Swing and AWT types only from the UI families
 * - is deliberately absent. See {@link #theRuleThisFileDoesNotEnforce()}.
 * <p>
 * <b>These read bytecode, so they catch use rather than imports.</b> An unused
 * import of a forbidden package leaves nothing in the class file and passes -
 * which is correct, since it does nothing, but it also means the rules cannot be
 * exercised by adding an import and watching them fail. Add a reference. That
 * mistake was made while writing this file, and both rules "passed" against a
 * violation that was not one.
 */
public class ArchitectureTest {

    /**
     * The plugin's own compiled classes, taken from the directory rather than
     * from the classpath.
     * <p>
     * {@code importPackages} walks everything the test classloader can see, which
     * here is the tests themselves and the instrumented copies the platform
     * plugin makes of both - so the rules reported test classes reading their own
     * fixtures off disk, and no combination of {@code ImportOption} excluded them
     * without excluding the production classes too. Naming the one directory that
     * holds what these rules are about is shorter than describing the four that
     * hold what they are not.
     */
    private static final @NotNull JavaClasses CLASSES = productionClasses();

    private static @NotNull JavaClasses productionClasses() {
        final @NotNull Path compiled = Path.of("build", "classes", "java", "main").toAbsolutePath();

        if (!Files.isDirectory(compiled)) {
            throw new AssertionError("No compiled classes at " + compiled + " - the architecture rules have"
                    + " nothing to read. Run this through Gradle, which compiles src/main first.");
        }

        return new ClassFileImporter().importPath(compiled);
    }

    /**
     * Everything a leaf may not reach: the feature packages, which is every
     * package but {@code model}, {@code util} and {@code logger}.
     */
    private static final String @NotNull [] ABOVE_MODEL = {
            "org.testin.indexer..", "org.testin.editor..", "org.testin.view..", "org.testin.codegen..",
            "org.testin.services..", "org.testin.creator..", "org.testin.explorer..", "org.testin.statusbar..",
            "org.testin.ui..", "org.testin.git..", "org.testin.sftp..", "org.testin.report..",
            "org.testin.importexport..", "org.testin.testcase..", "org.testin.testrun..", "org.testin.testproject..",
            "org.testin.testset..", "org.testin.search..", "org.testin.undo..", "org.testin.rename..",
            "org.testin.remove..", "org.testin.open..", "org.testin.clipboard..", "org.testin.run..",
            "org.testin.automate..", "org.testin.notifications..", "org.testin.setting..", "org.testin.config..",
            "org.testin.actions..", "org.testin.dialogs.."
    };

    /**
     * {@code model} is not a leaf yet, and these are the six that make it so
     * (#111). Measured 2026-09-04.
     * <p>
     * {@code TestRunStatus} is the newest and was added on 2026-09-04 by #175's
     * C12, which gave the shortcut-menu rows an interface to implement. It did
     * not breach that issue's own criterion - which named {@code editor},
     * {@code view}, {@code indexer} and {@code codegen} - and it is a violation
     * of this broader rule all the same. Recorded rather than quietly allowed,
     * which is the whole argument for this file existing.
     */
    private static final @NotNull Set<String> MODEL_LEAF_EXCEPTIONS = Set.of(
            "org.testin.model.DirectoryMapper",
            "org.testin.model.DirectoryType",
            "org.testin.model.NodeStatistics",
            "org.testin.model.RunEditorAttributes",
            "org.testin.model.TestEditorAttributes",
            "org.testin.model.TestRunStatus");

    /**
     * What {@code util} may not import: the features, but not {@code services} or
     * {@code notifications}.
     * <p>
     * Those two are the plugin's own infrastructure - the service locator and the
     * one way anything tells a tester something - and every layer calls them,
     * util included. #112 states this rule as "imports editor, view or dialogs",
     * and that is the rule enforced. Counting the infrastructure would have
     * listed three more files and said nothing about any of them.
     */
    private static final String @NotNull [] FEATURES = {
            "org.testin.indexer..", "org.testin.editor..", "org.testin.view..", "org.testin.codegen..",
            "org.testin.creator..", "org.testin.explorer..", "org.testin.statusbar..", "org.testin.ui..",
            "org.testin.git..", "org.testin.sftp..", "org.testin.report..", "org.testin.importexport..",
            "org.testin.testcase..", "org.testin.testrun..", "org.testin.testproject..", "org.testin.testset..",
            "org.testin.search..", "org.testin.undo..", "org.testin.rename..", "org.testin.remove..",
            "org.testin.open..", "org.testin.clipboard..", "org.testin.run..", "org.testin.automate..",
            "org.testin.dialogs.."
    };

    /**
     * {@code util} is not a util: these two import feature packages (#112).
     * Measured 2026-09-04, down from the four that story counted.
     */
    private static final @NotNull Set<String> UTIL_EXCEPTIONS = Set.of(
            "org.testin.util.EditorUtil",
            "org.testin.util.FontSync");

    /**
     * The one class outside the indexer and its exempt list that reads or writes
     * files directly (#49). Measured 2026-09-04.
     * <p>
     * Far fewer than #49 assumed. It reads and writes the sync baseline, which is
     * transfer bookkeeping rather than test data - the same argument that puts
     * {@code git} on the exempt list - so this is likely a decision to record on
     * that list rather than a call to move.
     */
    private static final @NotNull Set<String> FILE_ACCESS_EXCEPTIONS = Set.of(
            "org.testin.sftp.BaselineStore");

    /**
     * Matched on the outermost class, so freezing a name covers the anonymous
     * classes inside it.
     * <p>
     * An enum constant with a body compiles to {@code Enum$1}, and the dependency
     * that breaks a rule usually lives in exactly those bodies -
     * {@code TestEditorAttributes} declares its per-attribute behavior that way.
     * Freezing the outer name and missing the six numbered classes beside it was
     * the first thing this file got wrong.
     */
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

    /**
     * The rule CLAUDE.md opens with, and the one #49 exists because nothing
     * enforced.
     * <p>
     * The exempt packages are exempt for one reason: none of them reads or writes
     * <b>test data</b>. They handle generated source, the automation repository's
     * own {@code testin.yml}, the Git working tree, files outside the tree,
     * generated report output, the IDE settings path, and the log.
     */
    @Test
    public void onlyTheIndexerAndItsExemptListTouchFiles() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideOutsideOfPackages("org.testin.indexer..", "org.testin.codegen..",
                        "org.testin.config..", "org.testin.git..", "org.testin.importexport..",
                        "org.testin.report..", "org.testin.setting..", "org.testin.logger..")
                .and(notOneOf(FILE_ACCESS_EXCEPTIONS))
                .should().dependOnClassesThat().haveFullyQualifiedName("java.nio.file.Files")
                .because("the indexer is the single owner of file access, so its cache stays authoritative over"
                        + " test data and every read is a fast in-memory lookup. The exempt packages are exempt"
                        + " because none of them touches test data (CLAUDE.md, #49)");

        rule.check(CLASSES);
    }

    /**
     * <b>Swing and AWT types only from the UI families is not enforced here, on
     * purpose.</b>
     * <p>
     * #114 asked for it. It contradicts a convention CLAUDE.md states in the same
     * document: <i>"Enums carry their own presentation and actions (see
     * TestStatus, TestRunStatus)"</i>. Ten classes in {@code model} import Swing
     * or AWT and every one of them does it deliberately - a status carries its own
     * color, a directory kind carries its own icon, an attribute carries its own
     * renderer. That is the design, not drift.
     * <p>
     * A rule with ten frozen exceptions out of ten occurrences enforces nothing
     * and costs a list to maintain. If the convention is ever narrowed - colors
     * as hex, icons by key - the rule becomes worth writing, and this is where it
     * goes.
     * <p>
     * Kept as a test rather than a comment so it is read: a paragraph in a file
     * nobody opens is how the prose these rules replaced went stale.
     */
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

        org.testng.Assert.assertFalse(deliberate.isEmpty(),
                "If model no longer carries any Swing or AWT type, the convention has changed and the fourth"
                        + " rule is now worth writing. Read this method's javadoc before deleting it.");
    }
}
