package org.testin.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import javax.lang.model.SourceVersion;

/**
 * The two things a generated name has to promise (#250).
 * <p>
 * <b>The same name every time.</b> The package fallback was
 * {@code "generated" + currentTimeMillis()}, so a test set whose name sanitized
 * to nothing was in one package when its code was written and in another when
 * anything came looking - and a rename, a move or a remove then found nothing
 * and did nothing, silently.
 * <p>
 * <b>Two names, two classes.</b> The class fallback was the opposite fault:
 * every such name answered {@code DefaultTest}, so two test sets wrote into one
 * file and the second set's methods landed in the first set's class.
 * <p>
 * Pinned as properties rather than as the strings they currently produce. What
 * matters is that the answer is a function of the name and that different names
 * differ; the exact spelling of the fallback is free to change.
 */
public class GeneratedNamesTest {

    /**
     * Names with nothing a Java identifier can keep. Each is a real thing a
     * tester can type into the tree, which is why the fallback is reachable at
     * all.
     */
    private static final String[] UNNAMEABLE = {"!!!", "???", "...", "---", "@#$", "()"};

    @Test
    public void aPackageNameIsTheSameEveryTimeItIsAskedFor() {
        for (final String name : UNNAMEABLE) {
            assertEquals(NameSanitizer.packageName(name), NameSanitizer.packageName(name),
                    "'" + name + "' named a different package on two calls, so nothing that generated code"
                            + " under it could ever find that code again");
        }
    }

    @Test
    public void aClassNameIsTheSameEveryTimeItIsAskedFor() {
        for (final String name : UNNAMEABLE) {
            assertEquals(NameSanitizer.className(name), NameSanitizer.className(name));
        }
    }

    @Test
    public void twoTestSetsNeverWriteIntoOneClass() {
        for (int i = 0; i < UNNAMEABLE.length; i++) {
            for (int j = i + 1; j < UNNAMEABLE.length; j++) {
                assertNotEquals(NameSanitizer.className(UNNAMEABLE[i]), NameSanitizer.className(UNNAMEABLE[j]),
                        "'" + UNNAMEABLE[i] + "' and '" + UNNAMEABLE[j] + "' both name "
                                + NameSanitizer.className(UNNAMEABLE[i]) + ", so one set's methods land in the other's class");
            }
        }
    }

    @Test
    public void twoTestSetsNeverWriteIntoOnePackage() {
        for (int i = 0; i < UNNAMEABLE.length; i++) {
            for (int j = i + 1; j < UNNAMEABLE.length; j++) {
                assertNotEquals(NameSanitizer.packageName(UNNAMEABLE[i]), NameSanitizer.packageName(UNNAMEABLE[j]));
            }
        }
    }

    /**
     * A fallback that Java will not accept is not a fallback - it is the
     * uncompilable file the tester finds later.
     */
    @Test
    public void everyFallbackIsSomethingJavaAccepts() {
        for (final String name : UNNAMEABLE) {
            assertTrue(SourceVersion.isName(NameSanitizer.className(name)),
                    "'" + name + "' names the class " + NameSanitizer.className(name) + ", which Java will not accept");

            assertTrue(SourceVersion.isName(NameSanitizer.packageName(name)),
                    "'" + name + "' names the package " + NameSanitizer.packageName(name) + ", which Java will not accept");
        }
    }

    /**
     * The ordinary names still come out as they always did. The fallback is for
     * what cannot be named, and must not reach anything that can.
     */
    @Test
    public void anOrdinaryNameIsUntouchedByAnyOfThis() {
        assertEquals(NameSanitizer.className("Login"), "LoginTest");
        assertEquals(NameSanitizer.className("user login"), "UserLoginTest");
        assertEquals(NameSanitizer.packageName("Checkout"), "checkout");
        assertEquals(NameSanitizer.packageName("payment methods"), "paymentMethods");
    }
}
