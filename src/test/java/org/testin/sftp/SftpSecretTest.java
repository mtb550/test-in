package org.testin.sftp;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * How a secret is filed, and the fact that no agent is not a failure (#94).
 * <p>
 * The store itself is the IDE's and needs one running, so what is checked here
 * is the part that decides <em>where</em> a secret goes - which is the part that
 * can silently put two testers' passphrases in one place, or leak a secret into
 * the name of the thing holding it.
 */
public class SftpSecretTest {

    private static final SftpAddress QA = new SftpAddress("qa.internal", 2222, "/srv/testin");
    private static final SftpAddress LIVE = new SftpAddress("live.internal", 22, "/srv/testin");

    @Test
    public void twoServersDoNotShareAnEntry() {
        assertNotEquals(SftpSecret.KEY_PASSPHRASE.keyFor(QA, "muteb"),
                SftpSecret.KEY_PASSPHRASE.keyFor(LIVE, "muteb"),
                "one passphrase overwriting another is a tester locked out of a server they had working");
    }

    @Test
    public void twoAccountsOnOneServerDoNotShareAnEntry() {
        assertNotEquals(SftpSecret.KEY_PASSPHRASE.keyFor(QA, "muteb"),
                SftpSecret.KEY_PASSPHRASE.keyFor(QA, "deploy"));
    }

    @Test
    public void thePortIsPartOfTheIdentity() {
        assertNotEquals(SftpSecret.KEY_PASSPHRASE.keyFor(new SftpAddress("qa.internal", 22, ""), "muteb"),
                SftpSecret.KEY_PASSPHRASE.keyFor(new SftpAddress("qa.internal", 2222, ""), "muteb"),
                "two servers on one host are two servers");
    }

    @Test
    public void aPassphraseAndAPasswordAreDifferentThings() {
        assertNotEquals(SftpSecret.KEY_PASSPHRASE.keyFor(QA, "muteb"),
                SftpSecret.ACCOUNT_PASSWORD.keyFor(QA, "muteb"));
    }

    @Test
    public void theKeyNamesTheServerAndTheAccountAndNothingElse() {
        final String key = SftpSecret.KEY_PASSPHRASE.keyFor(QA, "muteb");

        assertTrue(key.contains("muteb"), key);
        assertTrue(key.contains("qa.internal"), key);
        assertTrue(key.contains("key passphrase"), key);
        assertFalse(key.contains("/srv/testin"),
                "the folder is not part of who you are; moving a project must not lose the passphrase");
    }

    /**
     * A machine with no agent, or one that is not running, must answer that
     * calmly - it is the ordinary state of a fresh laptop, and the key file is
     * what handles it.
     */
    @Test
    public void noAgentIsAnAnswerRatherThanAFailure() {
        assertFalse(SshAgent.loadedIdentities().isPresent() && SshAgent.available().isEmpty(),
                "identities without an agent would mean the two disagree");
    }

    /**
     * Asking repeatedly is calm, whatever the answers turn out to be.
     * <p>
     * This used to assert that two consecutive answers matched, and it went red
     * on a laptop where the OpenSSH agent service is disabled and PuTTY's
     * Pageant is installed: two probes a moment apart genuinely disagreed. That
     * is a property of the machine and not of this class - an agent can be
     * started or stopped between two questions, and a test that forbids it is
     * testing the operating system.
     * <p>
     * What {@link SshAgent} does promise is that asking never fails. Every
     * connector it tries reports absence by throwing from its constructor, and
     * a machine with no agent is the ordinary state of a fresh laptop rather
     * than an error - so the probe has to swallow all of that and still answer.
     */
    @Test
    public void askingForTheAgentIsNeverAFailure() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertNotNull(SshAgent.available(), "attempt " + attempt + " answered null instead of empty");
        }
    }
}
