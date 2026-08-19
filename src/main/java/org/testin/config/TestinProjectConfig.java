package org.testin.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;

import java.util.regex.Pattern;

/**
 * What an automation repository declares about the test project it exercises:
 * the contents of its {@code testin.yml} (#6).
 * <p>
 * One test project per automation repository. The repository names it, so the
 * pairing travels with a clone instead of living in one machine's IDE settings,
 * and a tester who opens the repository on a second machine is not asked to pick
 * it again.
 * <p>
 * What is deliberately <b>not</b> here is the Testin root folder. That is an
 * absolute path on one machine, so a committed file is the wrong place for it -
 * it stays an application-level setting, and this file names only which project
 * under that root the repository is about.
 * <p>
 * Every value is empty rather than null when the file leaves it out, so readers
 * are unconditional. Absence is turned into an empty value once, in
 * {@link #read}, which is why nothing downstream asks whether a key was there.
 *
 * @param version       the format version, so a file written by a later build is
 *                      recognized as one. Absent in a file written by hand, which
 *                      is why zero is a value and not a failure
 * @param testinProject the folder name of the test project under the Testin root -
 *                      not a path. The root is the machine's, the name is the
 *                      repository's
 * @param testinRepoUrl where the test project itself is cloned from - not this
 *                      repository's own remote. It is what makes the file worth
 *                      committing: a fresh machine has the automation repository
 *                      and not the test data, and the plugin can offer to fetch it
 *                      rather than showing an empty tree
 * @param defaultBranch the branch of the test project this repository expects;
 *                      empty means whatever the clone checked out
 */
public record TestinProjectConfig(int version,
                                  @NotNull String testinProject,
                                  @NotNull String testinRepoUrl,
                                  @NotNull String defaultBranch) {

    /**
     * A repository that has said nothing. Every way of failing to read one - no
     * file, no base path, unreadable, malformed - ends here, so no caller has to
     * tell the reasons apart.
     */
    public static final @NotNull TestinProjectConfig EMPTY = new TestinProjectConfig(0, "", "", "");

    /**
     * The forms {@code git clone} is given, and nothing else.
     * <p>
     * The URL arrives in a file that travels with a repository and ends up as an
     * argument to a command, so it is checked here rather than at whichever call
     * site runs it first. Written as the characters a clone URL is made of rather
     * than as the characters to fear: a list of allowed characters cannot be
     * short by one the way a list of forbidden ones can.
     */
    private static final @NotNull Pattern REPO_URL =
            Pattern.compile("^(https://|ssh://|git@)[A-Za-z0-9._~:/?#@%+-]+$");

    public TestinProjectConfig {
        testinRepoUrl = validRepoUrl(testinRepoUrl);
    }

    /**
     * Absence, as Jackson reports it, becoming the empty value this record
     * promises. A key the file leaves out arrives here as null, and a key written
     * with padding arrives with it; both are settled once, before the record
     * exists.
     */
    @JsonCreator
    static @NotNull TestinProjectConfig read(@JsonProperty("version") final int version,
                                             @JsonProperty("testinProject") final @Nullable String testinProject,
                                             @JsonProperty("testinRepoUrl") final @Nullable String testinRepoUrl,
                                             @JsonProperty("defaultBranch") final @Nullable String defaultBranch) {
        return new TestinProjectConfig(version, strip(testinProject), strip(testinRepoUrl), strip(defaultBranch));
    }

    /**
     * Whether the repository has said which test project it is about. The one
     * question every caller asks, so it is answered here instead of by comparing
     * against an empty string at each of them.
     */
    public boolean isBound() {
        return !testinProject.isEmpty();
    }

    /**
     * Whether the test project can be fetched when this machine does not have it
     * yet.
     */
    public boolean hasRepoUrl() {
        return !testinRepoUrl.isEmpty();
    }

    private static @NotNull String strip(final @Nullable String value) {
        return value == null ? "" : value.strip();
    }

    private static @NotNull String validRepoUrl(final @NotNull String value) {
        if (value.isEmpty() || REPO_URL.matcher(value).matches()) return value;

        Logger.warn("testinRepoUrl is not a clone URL and was ignored: " + value);
        return "";
    }
}
