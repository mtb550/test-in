package org.testin.sftp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * One sync between this machine and a server (#94).
 * <p>
 * The server holds a folder per project, and inside it the project exactly as it
 * is here: {@code test-01/.tp}, {@code test-01/Test Cases/...}, every case its
 * own file. So a colleague can read it over SSH, and a second Testin root can be
 * pointed straight at it.
 * <p>
 * That shape costs round trips. SFTP charges one per file, and a real project is
 * 2,246 of them - measured at 88 seconds against a server on this same machine,
 * and about six minutes over a 20 ms link. Which is why almost nothing here
 * moves files: {@link TransferAction} decides from three hashes, and only what
 * actually differs is sent or fetched. The expensive sync is the first one, when
 * everything differs.
 * <p>
 * The server keeps a manifest beside each project, so the hashes on its side are
 * read in one file rather than by fetching 2,246 to hash them.
 * <p>
 * <b>Off the EDT.</b> Every step waits on a network or on disk.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SftpSync {

    /**
     * What the server keeps beside a project: the hash and size of every file in
     * it, so a sync can tell what changed without fetching anything.
     * <p>
     * In a dot folder, so a tester listing the project sees their test cases and
     * not the plugin's bookkeeping.
     */
    private static final @NotNull String MANIFEST = ".testin/manifest.json";

    private static final @NotNull TypeReference<Map<String, Manifest.Entry>> ENTRIES = new TypeReference<>() {
    };

    /**
     * What one sync did, for the sentence the tester reads afterwards.
     *
     * @param conflicts files both sides changed, and files one side deleted while
     *                  the other changed them. Reported rather than guessed at -
     *                  the merge that settles them field by field is
     *                  {@code TestCaseMerge}, and wiring it in is its own step
     */
    public record Outcome(int uploaded, int downloaded, int unchanged, int conflicts,
                          @NotNull List<String> conflicting, @NotNull List<String> removedOnServer) {

        public @NotNull String describe() {
            if (uploaded == 0 && downloaded == 0 && conflicts == 0 && removedOnServer.isEmpty()) {
                return "Already up to date";
            }

            final @NotNull StringBuilder said = new StringBuilder();
            if (uploaded > 0) said.append("Sent ").append(uploaded);
            if (downloaded > 0) said.append(said.isEmpty() ? "Took " : ", took ").append(downloaded);
            if (conflicts > 0) said.append(said.isEmpty() ? "" : ", ").append(conflicts).append(" need you");
            if (!removedOnServer.isEmpty()) {
                said.append(said.isEmpty() ? "" : ", ").append(removedOnServer.size()).append(" gone from the server");
            }

            return said.toString();
        }
    }

    /**
     * Runs one sync and answers what it did.
     */
    public static @NotNull Outcome run(final @NotNull Project p, final @NotNull Path projectRoot,
                                       final @NotNull SftpAddress address, final @NotNull String user,
                                       final @NotNull SftpAuth auth, final @NotNull Path knownHosts,
                                       final @NotNull ProgressIndicator indicator) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        final @NotNull Path baselineFile = BaselineStore.fileFor(projectRoot);
        // Every path below is relative to the project's folder on the server.
        // The address already points at it - composed once in the config, from
        // the root and the project's name - so nothing here joins one on.

        indicator.setText("Reading this machine's copy...");
        final @NotNull Map<String, byte[]> local = indexer.filesUnder(projectRoot);
        final @NotNull Baseline baseline = BaselineStore.read(mapper, baselineFile);

        try (SftpTransport transport = SftpTransport.open(address, user, auth, knownHosts)) {
            indicator.setText("Asking the server what it has...");
            final boolean serverKnowsThisProject = transport.exists(MANIFEST);
            final @NotNull Manifest remote = readManifest(transport, mapper);

            // A server with no manifest has never heard of this project - which
            // is not the same as a manifest saying a file is gone. Reading the
            // first as the second turns an emptied server into thousands of
            // deletions the tester never asked for, and no way out of them: the
            // baseline keeps insisting the files were there. So when the server
            // knows nothing, neither does the baseline, and everything here is
            // simply sent.
            final @NotNull Baseline against = serverKnowsThisProject ? baseline : Baseline.EMPTY;
            if (!serverKnowsThisProject && !baseline.contents().isEmpty()) {
                Logger.info("The server has no record of " + address.path() + ", so this is a first sync");
            }

            final @NotNull Plan plan = decide(Manifest.of(local), remote, against.manifest());

            // Two records, kept apart on purpose. The manifest says what the
            // server holds, so it starts from what the server already held and
            // changes only where something was actually sent or removed. The
            // baseline says what both sides agreed, so a file that could not be
            // settled keeps its old entry - and is therefore still unsettled
            // next time, instead of one side quietly winning.
            // Seeded without Git's own files, so entries an earlier build wrote
            // are dropped rather than carried forward - the next sync cleans up
            // after this one without anybody deleting anything by hand.
            final @NotNull Map<String, Manifest.Entry> onServer = withoutGit(remote.entries());
            final @NotNull Map<String, String> agreed = withoutGit(against.contents());
            final @NotNull Map<String, byte[]> incoming = new TreeMap<>();

            transfer(transport, indicator, plan, local, onServer, agreed, incoming);

            if (!incoming.isEmpty()) {
                indicator.setText("Writing " + incoming.size() + " files from the server...");
                indexer.acceptIncoming(projectRoot, incoming);
            }

            indicator.setText("Recording what both sides now hold...");
            writeManifest(transport, mapper, new Manifest(onServer));
            BaselineStore.write(mapper, baselineFile, new Baseline(agreed));

            final @NotNull Outcome outcome = plan.outcome();
            Logger.info("Synced " + projectRoot.getFileName() + " with " + address.display() + ": "
                    + outcome.describe());

            return outcome;
        }
    }

    /**
     * Sends and fetches only what the decisions asked for, saying how far along
     * it is - a first sync moves every file, and an operation that long with no
     * progress reads as a hang.
     */
    private static void transfer(final @NotNull SftpTransport transport, final @NotNull ProgressIndicator indicator,
                                 final @NotNull Plan plan,
                                 final @NotNull Map<String, byte[]> local,
                                 final @NotNull Map<String, Manifest.Entry> onServer,
                                 final @NotNull Map<String, String> agreed,
                                 final @NotNull Map<String, byte[]> incoming) {
        final int total = plan.toUpload.size() + plan.toDownload.size() + plan.toDeleteRemotely.size();
        int done = 0;

        indicator.setIndeterminate(total == 0);
        if (total > 0) indicator.setText("Moving " + total + " files...");

        for (final String path : plan.toUpload) {
            indicator.setText2(path);
            transport.write(path, local.get(path));

            onServer.put(path, Manifest.Entry.of(local.get(path)));
            agreed.put(path, text(local.get(path)));
            indicator.setFraction((double) ++done / total);
        }

        for (final String path : plan.toDownload) {
            indicator.setText2(path);
            final byte @NotNull [] content = transport.read(path);

            incoming.put(path, content);
            // The server already had it, so its entry is unchanged.
            agreed.put(path, text(content));
            indicator.setFraction((double) ++done / total);
        }

        for (final String path : plan.toDeleteRemotely) {
            indicator.setText2(path);
            transport.delete(path);

            onServer.remove(path);
            agreed.remove(path);
            indicator.setFraction((double) ++done / total);
        }

        // A file neither side touched is agreed at whatever it holds now - and
        // one that both sides have stopped holding is agreed to be gone.
        for (final String path : plan.unchangedPaths) {
            if (local.containsKey(path)) agreed.put(path, text(local.get(path)));
            else agreed.remove(path);
        }

        // Nothing is recorded for a file that could not be settled. Writing this
        // machine's copy into the baseline would make the next sync read it as
        // agreed, and quietly take the other side's version over the top of the
        // work this tester was asked about and never answered.
    }

    /**
     * The same map without anything belonging to Git.
     */
    private static <V> @NotNull Map<String, V> withoutGit(final @NotNull Map<String, V> entries) {
        final @NotNull Map<String, V> kept = new TreeMap<>();
        entries.forEach((path, value) -> {
            if (!ProjectIndexer.isGitsOwn(path)) kept.put(path, value);
        });

        return kept;
    }


    private static @NotNull String text(final byte @NotNull [] content) {
        return new String(content, StandardCharsets.UTF_8);
    }

    /**
     * What every file's three states say should happen to it.
     */
    private static @NotNull Plan decide(final @NotNull Manifest local, final @NotNull Manifest remote,
                                        final @NotNull Manifest base) {
        final @NotNull Plan plan = new Plan();

        final @NotNull Set<String> everyPath = new TreeSet<>(local.pathsWith(remote));
        everyPath.addAll(base.entries().keySet());

        for (final String path : everyPath) {
            // Git's own directory is not test data, wherever it turns up. Kept
            // out here as well as out of the walk, because a server written by
            // an older build - or by a colleague who had the same bug - carries
            // those entries in its manifest, and they would then be argued about
            // on every sync forever with no way for a tester to settle them.
            if (ProjectIndexer.isGitsOwn(path)) continue;

            plan.add(path, TransferAction.of(base.at(path).sha256(), local.at(path).sha256(), remote.at(path).sha256()));
        }

        return plan;
    }

    /**
     * The hashes the server holds, and empty when it holds no manifest - a
     * project nobody has synced yet, which is the ordinary first case.
     */
    private static @NotNull Manifest readManifest(final @NotNull SftpTransport transport, final @NotNull Mapper mapper) {
        if (!transport.exists(MANIFEST)) return Manifest.EMPTY;

        try {
            return new Manifest(mapper.readValue(
                    new String(transport.read(MANIFEST), StandardCharsets.UTF_8), ENTRIES));
        } catch (final RuntimeException ex) {
            // A manifest that will not parse is treated as none, so the sync asks
            // about everything rather than acting on half an answer.
            Logger.warn("The server's manifest could not be read, comparing from nothing: " + ex.getMessage());
            return Manifest.EMPTY;
        }
    }

    private static void writeManifest(final @NotNull SftpTransport transport, final @NotNull Mapper mapper,
                                      final @NotNull Manifest manifest) {
        transport.write(MANIFEST,
                mapper.writeValueAsString(manifest.entries()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * What the decisions add up to, before anything moves.
     */
    private static final class Plan {

        private final @NotNull List<String> toUpload = new ArrayList<>();
        private final @NotNull List<String> toDownload = new ArrayList<>();
        private final @NotNull List<String> toDeleteRemotely = new ArrayList<>();
        private final @NotNull List<String> unchangedPaths = new ArrayList<>();
        private final @NotNull List<String> conflicting = new ArrayList<>();
        private final @NotNull List<String> removedOnServer = new ArrayList<>();

        private void add(final @NotNull String path, final @NotNull TransferAction action) {
            switch (action) {
                case UPLOAD -> toUpload.add(path);
                case DOWNLOAD -> toDownload.add(path);
                case DELETE_REMOTE -> toDeleteRemotely.add(path);
                case NOTHING -> unchangedPaths.add(path);

                // Both keep what is on this machine and say so, because guessing
                // loses work with no other copy - but they are different things
                // and a tester told the wrong one goes looking in the wrong place.
                case DELETE_LOCAL -> removedOnServer.add(path);
                case RESOLVE -> conflicting.add(path);
            }
        }

        private @NotNull Outcome outcome() {
            return new Outcome(toUpload.size(), toDownload.size(), unchangedPaths.size(),
                    conflicting.size(), List.copyOf(conflicting), List.copyOf(removedOnServer));
        }
    }
}
