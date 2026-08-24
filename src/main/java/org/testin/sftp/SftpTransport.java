package org.testin.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The one place the plugin talks to a server (#94).
 * <p>
 * Every read, write, delete and directory the sync performs goes through here,
 * the way every Git command goes through {@code GitCommandRunner} - so there is
 * one place that knows how a connection is made, one that knows how a path is
 * joined, and one to change when either is wrong.
 * <p>
 * <b>Never on the EDT.</b> Every method here waits on a network round trip, and
 * a test project is 2,246 files. Callers use {@code Task.Backgroundable}, and
 * nothing here may be reached while a read action is held - the rule that cost a
 * 41-second freeze when a tree node waited on a latch inside one.
 * <p>
 * <b>The host is always verified.</b> The connection is opened with
 * {@code StrictHostKeyChecking} left at its default, which refuses a host this
 * machine has not seen. That refusal is the point: a client that connected
 * anyway would connect to anything claiming to be the server. Trusting a new
 * host is the tester's decision, made once, and recorded in their
 * {@code known_hosts} - never a default here.
 * <p>
 * Failures are logged and thrown as unchecked, the way {@code GitCommandRunner}
 * throws - because a transfer that failed quietly would leave the baseline
 * claiming files were agreed that never arrived, and the next sync would read
 * that as the server having deleted them.
 */
public final class SftpTransport implements AutoCloseable {

    private static final int TIMEOUT = 30_000;

    /**
     * The folders this connection has already made or found, so a project of
     * 2,246 files does not ask about the same three levels 2,246 times.
     * <p>
     * Measured before it existed: uploading a real project spent most of its
     * time on mkdir calls for directories that had been made by the first file
     * to need them.
     */
    private final @NotNull Set<String> madeDirectories = new HashSet<>();

    private final @NotNull SftpAddress address;
    private final @NotNull Session session;
    private final @NotNull ChannelSftp sftp;

    private SftpTransport(final @NotNull SftpAddress address, final @NotNull Session session, final @NotNull ChannelSftp sftp) {
        this.address = address;
        this.session = session;
        this.sftp = sftp;
    }

    /**
     * Connects, or says why it could not.
     *
     * @param knownHosts the file of hosts this machine already trusts. A host
     *                   that is not in it is refused rather than trusted
     */
    public static @NotNull SftpTransport open(final @NotNull SftpAddress address, final @NotNull String user, final @NotNull SftpAuth auth, final @NotNull Path knownHosts) {
        Session session = null;
        try {
            final @NotNull JSch jsch = new JSch();
            jsch.setKnownHosts(knownHosts.toString());

            session = jsch.getSession(user, address.host(), address.port());
            auth.apply(jsch, session);
            session.connect(TIMEOUT);

            final @NotNull ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(TIMEOUT);

            Logger.info("Connected to " + address.display() + " as " + user);
            return new SftpTransport(address, session, sftp);

        } catch (final Exception ex) {
            // The session can already be up when the channel fails to open on top
            // of it; left alone it leaks an SSH connection per failed attempt.
            if (session != null && session.isConnected()) session.disconnect();

            Logger.error("Could not connect to " + address.display() + ": " + ex.getMessage());
            throw new IllegalStateException("Could not connect to " + address.display() + ": " + ex.getMessage());
        }
    }

    /**
     * What that file holds on the server.
     */
    public byte @NotNull [] read(final @NotNull String relative) {
        final @NotNull String path = address.resolve(relative);

        try (InputStream in = sftp.get(path)) {
            return in.readAllBytes();
        } catch (final Exception ex) {
            throw failed("read " + path, ex);
        }
    }

    /**
     * Puts the file there, making the folders above it first.
     * <p>
     * SFTP will not create a file in a directory that does not exist, and a test
     * project arrives as paths rather than as a tree, so the parents are made
     * here rather than by every caller remembering to.
     */
    public void write(final @NotNull String relative, final byte @NotNull [] content) {
        final @NotNull String path = address.resolve(relative);

        try {
            makeParentsOf(path);
            sftp.put(new ByteArrayInputStream(content), path);
        } catch (final Exception ex) {
            throw failed("write " + path, ex);
        }
    }

    /**
     * Removes the file. A file that is already gone is not a failure - the sync
     * wanted it gone, and it is.
     */
    public void delete(final @NotNull String relative) {
        final @NotNull String path = address.resolve(relative);

        try {
            sftp.rm(path);
        } catch (final SftpException ex) {
            if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) return;
            throw failed("delete " + path, ex);
        }
    }

    /**
     * Removes a directory, and says nothing when there was none.
     * <p>
     * Only ever an empty one: this exists to release the sync lock, and a lock
     * folder holding anything but its own holder file is a lock somebody else is
     * still inside. Its contents are removed first, by the caller that put them
     * there.
     */
    public void removeDirectory(final @NotNull String relative) {
        final @NotNull String path = address.resolve(relative);

        try {
            sftp.rmdir(path);
        } catch (final SftpException ex) {
            if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) return;
            throw failed("remove the directory " + path, ex);
        }
    }

    public boolean exists(final @NotNull String relative) {
        try {
            sftp.stat(address.resolve(relative));
            return true;
        } catch (final SftpException ex) {
            return false;
        }
    }

    /**
     * Makes one directory, and answers whether this call is what made it.
     * <p>
     * False means somebody else already had. That is the whole of the sync lock:
     * creating a directory is the one thing SFTP does atomically, so two testers
     * syncing at the same moment cannot both be told they made it.
     */
    public boolean makeDirectory(final @NotNull String relative) {
        final @NotNull String path = address.resolve(relative);

        // The folders above it first, and only them: the atomicity this relies
        // on is in creating the last one. Without this a project the server has
        // never held could not be locked - mkdir refuses when the parent is not
        // there, which reads as "somebody else has it", so the very first sync
        // of a project would refuse itself.
        makeParentsOf(path);

        try {
            sftp.mkdir(path);
            return true;
        } catch (final SftpException ex) {
            // Already there means somebody else holds the lock, which is the false
            // the caller is asking for. Anything else - no permission, a full disk
            // - is a real failure wearing the same false, and reading it as
            // "another sync is running" sends the tester chasing a colleague who
            // is not there. So it is raised unless the folder now exists.
            if (exists(relative)) return false;
            throw failed("make the directory " + path, ex);
        }
    }

    /**
     * Every file under that folder, at any depth, as paths relative to the
     * project - which is how a manifest names them.
     * <p>
     * Directories are walked and not listed: a manifest describes files, and an
     * empty folder carries nothing to sync. Names beginning with a dot are kept,
     * because every marker the indexer reads is one.
     */
    public @NotNull List<String> filesUnder(final @NotNull String relative) {
        final @NotNull List<String> found = new ArrayList<>();
        collect(relative, found);

        return found;
    }

    private void collect(final @NotNull String relative, final @NotNull List<String> found) {
        final @NotNull String path = address.resolve(relative);

        try {
            for (final ChannelSftp.LsEntry entry : sftp.ls(path)) {
                final @NotNull String name = entry.getFilename();
                if (name.equals(".") || name.equals("..")) continue;

                final @NotNull String child = relative.isEmpty() ? name : relative + "/" + name;
                if (entry.getAttrs().isDir()) collect(child, found);
                else found.add(child);
            }
        } catch (final SftpException ex) {
            // A folder that is not there holds no files, which is the answer a
            // first sync to an empty server needs.
            if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) return;
            throw failed("list " + path, ex);
        }
    }

    /**
     * Creates every directory above that path that is not there yet.
     */
    private void makeParentsOf(final @NotNull String path) {
        final int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) return;

        final @NotNull String[] parts = path.substring(0, lastSlash).split("/");
        final @NotNull StringBuilder walked = new StringBuilder();

        for (final String part : parts) {
            if (part.isEmpty()) {
                walked.append('/');
                continue;
            }
            if (walked.length() > 0 && walked.charAt(walked.length() - 1) != '/') walked.append('/');
            walked.append(part);

            if (!madeDirectories.add(walked.toString())) continue;

            try {
                sftp.mkdir(walked.toString());
            } catch (final SftpException alreadyThere) {
                // It was already on the server, put there by an earlier sync.
                // Asking first would cost the same round trip this saves.
                Logger.debug("Directory already on the server: " + walked);
            }
        }
    }

    private @NotNull IllegalStateException failed(final @NotNull String what, final @NotNull Exception ex) {
        Logger.error("Could not " + what + " on " + address.display() + ": " + ex.getMessage());
        return new IllegalStateException("Could not " + what + ": " + ex.getMessage());
    }

    @Override
    public void close() {
        sftp.disconnect();
        session.disconnect();
    }
}
