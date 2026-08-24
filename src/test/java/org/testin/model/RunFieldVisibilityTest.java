package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Which run-configuration fields apply to which kind of run.
 * <p>
 * A browser belongs to a web frontend and a handset to a mobile one. Both
 * questions are declared on {@link TestRunConfiguration} rather than in the
 * form, because the answer decides two things and they have to agree: what the
 * tester is shown, and what the run is saved with. A browser left behind on a
 * run that moved to mobile is a value no report could explain.
 */
public class RunFieldVisibilityTest {

    private static final @NotNull String WEB = "Web";
    private static final @NotNull String MOBILE = "Mobile";
    private static final @NotNull String FRONTEND = "Frontend";
    private static final @NotNull String BACKEND = "Backend";

    @Test
    public void aWebFrontendIsAskedWhichBrowser() {
        assertTrue(TestRunConfiguration.BROWSER.isShownFor(run(WEB, FRONTEND)));
    }

    @Test
    public void aMobileFrontendIsAskedWhichDevice() {
        assertTrue(TestRunConfiguration.DEVICE_TYPE.isShownFor(run(MOBILE, FRONTEND)));
    }

    /**
     * The two never both apply. A run is on one platform, and asking for a
     * browser and a handset at once would be asking the tester to describe two
     * runs.
     */
    @Test
    public void aRunIsNeverAskedForBothAtOnce() {
        for (final String platform : List.of(WEB, MOBILE, BACKEND, "")) {
            for (final String component : List.of(FRONTEND, BACKEND, "")) {
                final @NotNull TestRunConfiguration.Chosen chosen = run(platform, component);

                assertFalse(TestRunConfiguration.BROWSER.isShownFor(chosen)
                                && TestRunConfiguration.DEVICE_TYPE.isShownFor(chosen),
                        "both were asked for a " + platform + " " + component + " run");
            }
        }
    }

    @Test
    public void aBackendRunIsAskedForNeither() {
        assertFalse(TestRunConfiguration.BROWSER.isShownFor(run(WEB, BACKEND)),
                "a backend has no browser");
        assertFalse(TestRunConfiguration.DEVICE_TYPE.isShownFor(run(MOBILE, BACKEND)),
                "a backend has no handset");
    }

    /**
     * Before anything is chosen, neither is on the form. A field waiting on an
     * answer must not look like a field the tester forgot to fill in.
     */
    @Test
    public void nothingChosenYetMeansNeitherIsAskedFor() {
        assertFalse(TestRunConfiguration.BROWSER.isShownFor(run("", "")));
        assertFalse(TestRunConfiguration.DEVICE_TYPE.isShownFor(run("", "")));
    }

    /**
     * Every other field is on every run. Only the two that depend on an answer
     * may ever be missing, or a run would quietly lose a value nobody was told
     * about.
     */
    @Test
    public void everyOtherFieldIsOnEveryRun() {
        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (field == TestRunConfiguration.BROWSER || field == TestRunConfiguration.DEVICE_TYPE) continue;

            assertTrue(field.isShownFor(run("", "")),
                    field.getDisplayName() + " depends on an answer but nothing says when it applies");
        }
    }

    /**
     * The rules look for words the lists actually offer.
     * <p>
     * This is the failure the whole arrangement is exposed to: renaming a
     * platform from "Mobile" to "Mobile App" in the dropdown does not break the
     * build, it silently stops offering the device field, and nobody finds out
     * until a tester cannot say which handset they used.
     */
    @Test
    public void theRulesLookForAnswersTheListsOffer() {
        final @NotNull List<String> platforms = Arrays.asList(TestRunConfiguration.PLATFORM.getOptions());
        final @NotNull List<String> components = Arrays.asList(TestRunConfiguration.COMPONENT.getOptions());

        assertTrue(platforms.contains(WEB) && platforms.contains(MOBILE),
                "the platform list must still offer " + WEB + " and " + MOBILE + ", it offers " + platforms);
        assertTrue(components.contains(FRONTEND),
                "the component list must still offer " + FRONTEND + ", it offers " + components);
    }

    @Test
    public void theDeviceListOffersTheThreeHandsets() {
        assertEquals(Arrays.asList(TestRunConfiguration.DEVICE_TYPE.getOptions()),
                List.of("", "iPhone", "Samsung", "Huawei"));
    }

    /**
     * A run described by its platform and component, and nothing chosen
     * anywhere else - which is all the rules read.
     */
    private static @NotNull TestRunConfiguration.Chosen run(final @NotNull String platform, final @NotNull String component) {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);
        answers.put(TestRunConfiguration.PLATFORM, platform);
        answers.put(TestRunConfiguration.COMPONENT, component);

        return field -> answers.getOrDefault(field, "");
    }
}
