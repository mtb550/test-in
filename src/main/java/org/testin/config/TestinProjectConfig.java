package org.testin.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.sftp.SftpAddress;

import java.util.Objects;
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
 * What is deliberately <b>not</b> here is anything about one machine or one
 * person - the Testin root folder, the tester's name, the account used to reach
 * a server, and above all no secret. This file is committed, so a value in it is
 * shared with everyone who clones and lives in the repository's history forever.
 * <p>
 * <b>{@link #location} is the authority.</b> The file can contradict itself -
 * say {@code remote} and give no address, or {@code local} and leave a URL
 * behind - so the mode decides and the rest is checked against it, with what
 * does not add up written to the log rather than guessed at.
 * <p>
 * Every value is empty rather than null when the file leaves it out, so readers
 * are unconditional.
 *
 * @param location      whether this project is shared at all
 * @param connection    how it is reached when it is
 * @param repoUrl       where the test project is cloned from, for {@code git}
 * @param sftpHost      the machine holding it, for {@code sftp}
 * @param sftpPort      its SSH port; 22 unless the file says otherwise
 * @param sftpPath      the folder on it that holds the test projects
 * @param testinProject which test project this repository is about, whichever
 *                      way it is reached. One key rather than one per
 *                      connection: a tester who switches this file from git to
 *                      sftp changes how the project is reached, not which
 *                      project it is
 */
public record TestinProjectConfig(@NotNull TestinLocation location, @NotNull ConnectionType connection, @NotNull String repoUrl, @NotNull String sftpHost, int sftpPort, @NotNull String sftpPath, @NotNull String testinProject) {

    /**
     * The port an address is assumed to be on when the file does not say.
     */
    private static final int DEFAULT_PORT = 22;

    /**
     * A repository that has said nothing. Every way of failing to read one - no
     * file, no base path, unreadable, malformed - ends here, so no caller has to
     * tell the reasons apart.
     */
    public static final @NotNull TestinProjectConfig EMPTY = new TestinProjectConfig(
            TestinLocation.LOCAL, ConnectionType.NONE, "", "", DEFAULT_PORT, "", "");

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

    /**
     * A host name or address, and nothing that could be anything else.
     */
    private static final @NotNull Pattern HOST = Pattern.compile("^[A-Za-z0-9.-]+$");

    public TestinProjectConfig {
        repoUrl = validRepoUrl(repoUrl);
        sftpHost = validHost(sftpHost);
        sftpPort = sftpPort <= 0 || sftpPort > 65535 ? DEFAULT_PORT : sftpPort;

        // The mode decides. A project the file calls local is local, whatever
        // addresses were left in it - so an address commented back in later is
        // the only thing that changes the answer.
        connection = location.isRemote() ? connection : ConnectionType.NONE;

        report(location, connection, repoUrl, sftpHost);
    }

    /**
     * Says what does not add up, once, where it can be read.
     * <p>
     * Not corrected: a file that says {@code sftp} and gives no host has a
     * mistake in it, and quietly behaving as something else would hide the one
     * fact the tester needs.
     */
    private static void report(final @NotNull TestinLocation location, final @NotNull ConnectionType connection, final @NotNull String repoUrl, final @NotNull String host) {
        if (!location.isRemote()) return;

        if (connection == ConnectionType.NONE) {
            Logger.warn("testin.yml says the project is remote but does not say how to reach it - "
                    + "set connection to git or sftp");
        }
        if (connection == ConnectionType.GIT && repoUrl.isEmpty()) {
            Logger.warn("testin.yml says connection: git but has no RepoUrl");
        }
        if (connection == ConnectionType.SFTP && host.isEmpty()) {
            Logger.warn("testin.yml says connection: sftp but has no sftpHost");
        }
    }

    /**
     * Absence, as Jackson reports it, becoming the empty value this record
     * promises.
     */
    @JsonCreator
    static @NotNull TestinProjectConfig read(@JsonProperty("location") final @Nullable String location, @JsonProperty("connection") final @Nullable String connection, @JsonProperty("RepoUrl") final @Nullable String repoUrl, @JsonProperty("sftpHost") final @Nullable String sftpHost, @JsonProperty("sftpPort") final @Nullable Integer sftpPort, @JsonProperty("sftpPath") final @Nullable String sftpPath, @JsonProperty("testinProject") final @Nullable String testinProject) {
        return new TestinProjectConfig(TestinLocation.of(strip(location)),
                ConnectionType.of(strip(connection)),
                strip(repoUrl),
                strip(sftpHost),
                Objects.requireNonNullElse(sftpPort, DEFAULT_PORT),
                strip(sftpPath),
                strip(testinProject));
    }

    private static @NotNull String strip(final @Nullable String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }

    private static @NotNull String validRepoUrl(final @NotNull String value) {
        final @NotNull String url = withoutCredentials(value);
        if (url.isEmpty() || REPO_URL.matcher(url).matches()) return url;

        Logger.warn("RepoUrl is not a clone URL and was ignored: " + url);
        return "";
    }

    /**
     * A clone URL with any account and token taken out of it.
     * <p>
     * Git will hand out {@code https://user:token@host/repo} as a remote's URL
     * without being asked, and this file is committed - so a token that reached
     * it would be in the repository's history forever, readable by everyone who
     * clones. Stripped on the way in as well as on the way out, so the record
     * cannot hold one however it arrived: a file somebody committed by hand is
     * as much of a leak as one this plugin wrote.
     * <p>
     * {@code git@github.com} survives. That is the conventional account name for
     * every SSH clone URL and not a secret; what goes is the {@code user:secret}
     * form, which is the only one that carries one.
     */
    static @NotNull String withoutCredentials(final @NotNull String url) {
        final int scheme = url.indexOf("://");
        if (scheme < 0) return url;

        final int start = scheme + "://".length();
        final int end = url.indexOf('/', start);
        final @NotNull String authority = end < 0 ? url.substring(start) : url.substring(start, end);

        final int at = authority.lastIndexOf('@');
        if (at < 0 || authority.lastIndexOf(':', at) < 0) return url;

        return url.substring(0, start) + authority.substring(at + 1) + (end < 0 ? "" : url.substring(end));
    }

    private static @NotNull String validHost(final @NotNull String value) {
        if (value.isEmpty() || HOST.matcher(value).matches()) return value;

        Logger.warn(value.contains("@")
                ? "sftpHost must not carry an account - that belongs in this machine's settings, "
                + "because this file is shared with everyone. Ignored: " + value
                : "sftpHost is not a host name and was ignored: " + value);
        return "";
    }

    /**
     * Which test project this repository is about.
     * <p>
     * One key, read the same way however the project is reached. There is one
     * Testin root folder and it holds several projects, so "which one" is a
     * question local, Git and SFTP all have to answer - none of them can take
     * it from the root, and a key per connection would have made switching this
     * file from git to sftp read as switching to a different project.
     * <p>
     * Deliberately not taken from the clone URL when the key is missing. That
     * would have been a second rule, for one connection type, and the address
     * naming the project is what this replaced - a repository renamed on GitHub
     * would silently re-point the binding. A repository that names nothing is
     * unbound, and the tester picks once.
     */
    public @NotNull String projectName() {
        return testinProject;
    }


    /**
     * Whether the repository has said which test project it is about.
     */
    public boolean isBound() {
        return !projectName().isEmpty();
    }

    /**
     * Whether the test project can be fetched when this machine does not have it
     * yet.
     */
    public boolean hasRepoUrl() {
        return connection == ConnectionType.GIT && !repoUrl.isEmpty();
    }

    /**
     * Whether this project is reachable on a server.
     */
    public boolean hasSftp() {
        return connection == ConnectionType.SFTP && !sftpHost.isEmpty() && !projectName().isEmpty();
    }

    /**
     * Where it is on that server, and {@link SftpAddress#NONE} when it is not on
     * one.
     * <p>
     * The address points at <b>this project's own folder</b>, not at the root
     * holding several - composed here and nowhere else, so nothing downstream
     * joins a project name onto a path a second time.
     */
    public @NotNull SftpAddress sftpAddress() {
        return hasSftp() ? new SftpAddress(sftpHost, sftpPort, projectFolder()) : SftpAddress.NONE;
    }

    /**
     * This project's folder on the server: the root with its name under it.
     * <p>
     * A root that already ends in that name is left alone. Writing the whole
     * path in {@code sftpPath} is the obvious thing for a tester to do, and it
     * must not be read as asking for the folder twice - {@code /Testin/test-01}
     * is where the project is, never the parent of another {@code test-01}.
     */
    private @NotNull String projectFolder() {
        final @NotNull String root = trimmed(sftpPath);
        final @NotNull String name = projectName();

        if (root.isEmpty() || root.equals(name)) return name;

        return root.endsWith("/" + name) ? root : root + "/" + name;
    }

    /**
     * The folder without a trailing separator, so joining a name onto it needs
     * no test for one.
     */
    private static @NotNull String trimmed(final @NotNull String path) {
        if (path.isEmpty() || path.equals("/")) return "";

        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
