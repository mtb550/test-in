package org.testin.git;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Anything Git says or is asked, with the credentials taken out of it.
 * <p>
 * An HTTPS remote can carry them in the URL - {@code https://user:token@host/x}
 * or {@code https://ghp_token@host/x} - and Git echoes the remote it was working
 * against in its own failure messages. So a failed fetch, push or clone put the
 * token into {@code idea.log} and into the balloon the tester read, and a
 * {@code git remote add} that failed put it there a second way, because that URL
 * is an argument on the command line the failure names.
 * <p>
 * One owner for the redaction rather than one at each of those, because a token
 * reaching a log is not the kind of thing to fix in the places somebody
 * remembered.
 * <p>
 * Public for exactly that reason. Every Git command runs through
 * {@link GitCommandRunner}, which redacts its own failures - except the clone,
 * which runs through git4idea's own {@code Git.clone} because a clone creates
 * the repository rather than looking one up. It is outside this package and it
 * reports the one failure a tester can still put a token into, so it calls this
 * rather than growing a second copy of the pattern (#66, finding 4).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitSafeText {

    /**
     * The userinfo of a URL: everything between the scheme and the {@code @}
     * that introduces the host.
     * <p>
     * Both forms are one pattern, because a token used as a username has no
     * colon in it and a password does. Deliberately unanchored and applied to
     * whole messages, since Git's output is prose with URLs in it rather than a
     * URL.
     */
    private static final @NotNull Pattern CREDENTIALS = Pattern.compile("([a-zA-Z][a-zA-Z0-9+.\\-]*://)[^/@\\s]+@");

    // UC-SHARE-013, Rule-SHARE-062
    public static @NotNull String withoutCredentials(final @NotNull String text) {
        return CREDENTIALS.matcher(text).replaceAll("$1***@");
    }
}
