package org.testin.services;

import org.testin.indexer.OwnWrites;
import org.testin.indexer.Rescan;
import org.testin.logger.LoggerService;
import org.testin.setting.AppSettingsState;
import org.testin.util.EditorUtil;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * An application service is one object for the IDE, whichever way it is asked
 * for.
 * <p>
 * Asking a project container for one does not fetch the application's - it
 * builds a second instance inside the project, and a service that persists
 * state then has two state files. That is what happened to the tester's name:
 * the settings page has no project so it wrote the application's copy, while
 * every run, marker and verdict passed a project and read the project's. Two
 * files called testinSettings.xml, two different names, and the name typed in
 * Settings never reached a test run.
 * <p>
 * The routing reads each class's own {@code @Service} annotation, so what is
 * checked here is that it reads it correctly - a service added later is routed
 * by declaring its level, not by being listed anywhere.
 */
public class ServiceRoutingTest {

    /**
     * The one that caused it. Named on its own because it is the regression: if
     * this ever answers false again, a tester's name stops reaching their runs.
     */
    @Test
    public void theSettingsBelongToTheApplication() {
        assertTrue(Services.isApplicationLevel(AppSettingsState.class),
                "the settings are one object for the IDE; a per-project copy gets its own testinSettings.xml "
                        + "and the name typed in Settings never reaches a run");
    }

    @Test
    public void everyApplicationServiceIsRecognized() {
        for (final Class<?> service : new Class<?>[]{AppSettingsState.class, OwnWrites.class, Rescan.class, LoggerService.class}) {
            assertTrue(Services.isApplicationLevel(service),
                    service.getSimpleName() + " declares Service.Level.APP but would be built per project");
        }
    }

    /**
     * The other direction matters just as much: routing a project service to the
     * application would give every project one editor bookkeeper between them.
     */
    @Test
    public void aProjectServiceStaysWithItsProject() {
        assertFalse(Services.isApplicationLevel(EditorUtil.class),
                "EditorUtil is per project - one shared across projects would close the wrong editors");
    }

    @Test
    public void somethingThatIsNotAServiceStaysWithItsProject() {
        assertFalse(Services.isApplicationLevel(String.class),
                "a class with no @Service annotation must not be diverted to the application container");
    }

    /**
     * Asked twice, because the answer is cached after the first call and a cache
     * that returns something different the second time would be worse than none.
     */
    @Test
    public void theAnswerIsTheSameEveryTime() {
        assertTrue(Services.isApplicationLevel(AppSettingsState.class) == Services.isApplicationLevel(AppSettingsState.class));
        assertFalse(Services.isApplicationLevel(EditorUtil.class) || Services.isApplicationLevel(EditorUtil.class));
    }
}
