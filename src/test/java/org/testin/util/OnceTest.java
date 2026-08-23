package org.testin.util;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The guard that decides whether Testin has already started for a project.
 * <p>
 * Three doors lead to {@code StartupActivity.execute} and none of them shares a
 * thread with the others: the platform runs its startup extension on a
 * background coroutine while a tool window builds its content on the EDT. What
 * this has to hold is therefore not just "the second caller is refused" but
 * "exactly one of the callers is admitted, however they arrive" - the version
 * that read the flag and then set it could admit two, and two admissions is a
 * second scan of the whole Testin root and a second subscription to every test
 * event.
 */
public class OnceTest {

    private static final Key<Boolean> STARTED = Key.create("test.started");

    private static final Key<Boolean> OTHER = Key.create("test.other");

    @Test
    public void theFirstCallerIsAdmittedAndEveryOneAfterIsRefused() {
        final UserDataHolderBase project = new UserDataHolderBase();

        assertTrue(Once.claim(project, STARTED), "nobody had claimed it");
        assertFalse(Once.claim(project, STARTED), "the second door finds it taken");
        assertFalse(Once.claim(project, STARTED), "and so does the third");
    }

    @Test
    public void oneProjectClaimingSaysNothingAboutAnother() {
        final UserDataHolderBase opened = new UserDataHolderBase();
        final UserDataHolderBase alsoOpened = new UserDataHolderBase();

        assertTrue(Once.claim(opened, STARTED));
        assertTrue(Once.claim(alsoOpened, STARTED),
                "a second project opening has its own startup to run");
    }

    @Test
    public void twoQuestionsOnOneProjectAreTwoAnswers() {
        final UserDataHolderBase project = new UserDataHolderBase();

        assertTrue(Once.claim(project, STARTED));
        assertTrue(Once.claim(project, OTHER), "a different key is a different question");
    }

    @Test
    public void exactlyOneOfManyThreadsArrivingTogetherIsAdmitted() {
        try {
            final UserDataHolderBase project = new UserDataHolderBase();
            final AtomicInteger admitted = new AtomicInteger();
            final int doors = 32;
            final CountDownLatch open = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(doors);
            final ExecutorService threads = Executors.newFixedThreadPool(doors);

            try {
                for (int i = 0; i < doors; i++) {
                    threads.execute(() -> {
                        try {
                            open.await();
                            if (Once.claim(project, STARTED)) admitted.incrementAndGet();
                        } catch (final InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
                }

                open.countDown();
                assertTrue(done.await(10, TimeUnit.SECONDS), "the threads should all have finished");
            } finally {
                threads.shutdownNow();
            }

            assertEquals(admitted.get(), 1,
                    "reading the flag and setting it have to be one step, or two threads both start Testin");
        } catch (final InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void aClaimIsRememberedOnTheProjectItWasMadeOn() {
        final UserDataHolderBase project = new UserDataHolderBase();

        Once.claim(project, STARTED);

        assertEquals(project.getUserData(STARTED), Boolean.TRUE,
                "the flag is the project's own data, so it lives exactly as long as the project does");
    }
}
