package org.testin.sftp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.git.TestCaseMerge;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Mapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * @param merged        files both sides changed that {@link TestCaseMerge}
     *                      settled field by field, with nobody asked
     * @param conflicts     what is left after that: cases where both testers
     *                      rewrote the same field, and files that are not test
     *                      cases at all, which have no fields to merge
     * @param unsettled     the same cases, carrying the question to put to the
     *                      tester and the merge so far
     * @param blockedBy     why nothing ran, and empty when something did. A
     *                      second tester syncing the same project at the same
     *                      moment is told who has it rather than made to guess
     */
    public record Outcome(int uploaded, int downloaded, int unchanged, int merged, int conflicts, @NotNull List<String> conflicting, @NotNull List<String> removedOnServer, @NotNull List<Unsettled> unsettled, @NotNull String blockedBy) {

        /**
         * Nothing ran, because somebody else is syncing this project.
         */
        public static @NotNull Outcome blocked(final @NotNull String because) {
            return new Outcome(0, 0, 0, 0, 0, List.of(), List.of(), List.of(), because);
        }

        public boolean isBlocked() {
            return !blockedBy.isEmpty();
        }

        public @NotNull String describe() {
            if (isBlocked()) return blockedBy;

            if (uploaded == 0 && downloaded == 0 && merged == 0 && conflicts == 0 && removedOnServer.isEmpty()) {
                return "Already up to date";
            }

            final @NotNull StringBuilder said = new StringBuilder();
            if (uploaded > 0) said.append("Sent ").append(uploaded);
            if (downloaded > 0) said.append(said.isEmpty() ? "Took " : ", took ").append(downloaded);
            if (merged > 0) said.append(said.isEmpty() ? "Merged " : ", merged ").append(merged);
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
    public static @NotNull Outcome run(final @NotNull Project p, final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull String user, final @NotNull SftpAuth auth, final @NotNull Path knownHosts, final @NotNull ProgressIndicator indicator) {
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
            final @NotNull SyncLock lock = new SyncLock(transport);

            // Before anything is read, because what this protects is the record
            // rather than the transfer: two syncs reading one manifest both
            // write it back, and the second one's describes a server it never
            // looked at.
            final @NotNull Optional<String> heldBy =
                    lock.takenBy(Services.getInstance(AppSettingsState.class).testerName);
            if (heldBy.isPresent()) return Outcome.blocked(heldBy.get());

            try {
                return inside(projectRoot, address, indicator, transport, indexer, mapper, baselineFile,
                        local, baseline);
            } finally {
                lock.release();
            }
        }
    }

    /**
     * Everything one sync does while it holds the lock.
     * <p>
     * Split out so the lock is taken and given back in one place, in a finally
     * that no early return can slip past - a lock left behind blocks every
     * tester on the team until somebody deletes a hidden folder over SSH.
     */
    private static @NotNull Outcome inside(final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull ProgressIndicator indicator, final @NotNull SftpTransport transport, final @NotNull ProjectIndexer indexer, final @NotNull Mapper mapper, final @NotNull Path baselineFile, final @NotNull Map<String, byte[]> local, final @NotNull Baseline baseline) {
        indicator.setText("Asking the server what it has...");
        final @NotNull Manifest remote = readManifest(transport, mapper);

        // A server with no manifest has never heard of this project - which
        // is not the same as a manifest saying a file is gone. Reading the
        // first as the second turns an emptied server into thousands of
        // deletions the tester never asked for, and no way out of them: the
        // baseline keeps insisting the files were there. So when the server
        // knows nothing, neither does the baseline, and everything here is
        // simply sent.
        //
        // Read off the manifest rather than asked of the server a second time.
        // It was two questions about one fact, and they disagreed in the case
        // that matters most: a manifest present but unparsable answers yes to
        // "does the file exist" and comes back empty from the read, which is
        // every file the baseline knows classified as deleted on the server -
        // exactly the offer this guard exists to prevent, and the one
        // readManifest already says it treats as no manifest at all.
        //
        // An empty manifest counts as knowing nothing for the same reason: a
        // server holding no files has nothing for anyone to have deleted.
        final boolean serverKnowsThisProject = !remote.entries().isEmpty();
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

        // After the transfer, so a case both sides changed is settled
        // against what the server actually holds rather than against what
        // it held before this sync moved anything.
        final @NotNull List<Unsettled> unsettled =
                settle(transport, mapper, indicator, plan, local, onServer, agreed, incoming);

        if (!incoming.isEmpty()) {
            indicator.setText("Writing " + incoming.size() + " files from the server...");
            indexer.acceptIncoming(projectRoot, incoming);
        }

        indicator.setText("Recording what both sides now hold...");
        writeManifest(transport, mapper, new Manifest(onServer));
        BaselineStore.write(mapper, baselineFile, new Baseline(agreed));

        final @NotNull Outcome outcome = plan.outcome(unsettled);
        Logger.info("Synced " + projectRoot.getFileName() + " with " + address.display() + ": "
                + outcome.describe());

        return outcome;
    }

    /**
     * Sends and fetches only what the decisions asked for, saying how far along
     * it is - a first sync moves every file, and an operation that long with no
     * progress reads as a hang.
     */
    private static void transfer(final @NotNull SftpTransport transport, final @NotNull ProgressIndicator indicator, final @NotNull Plan plan, final @NotNull Map<String, byte[]> local, final @NotNull Map<String, Manifest.Entry> onServer, final @NotNull Map<String, String> agreed, final @NotNull Map<String, byte[]> incoming) {
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
     * Puts the cases a tester answered onto both sides, and records that they
     * now agree (#94).
     * <p>
     * Its own pass rather than part of the sync that raised the questions,
     * because that sync runs on a background thread and the questions are a
     * dialog. Blocking a background task on the tester is how an IDE freezes;
     * asking afterward costs one more connection on the rarest path there is.
     * <p>
     * Both sides and the record, in that order and all three: writing only this
     * machine's copy would leave the case unsettled on the server and ask the
     * same question again on the next sync, forever.
     */
    public static boolean finish(final @NotNull Project p, final @NotNull Path projectRoot, final @NotNull SftpAddress address, final @NotNull String user, final @NotNull SftpAuth auth, final @NotNull Path knownHosts, final @NotNull Map<String, String> answered) {
        if (answered.isEmpty()) return false;

        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        final @NotNull Path baselineFile = BaselineStore.fileFor(projectRoot);

        try (SftpTransport transport = SftpTransport.open(address, user, auth, knownHosts)) {
            final @NotNull SyncLock lock = new SyncLock(transport);
            if (lock.takenBy(Services.getInstance(AppSettingsState.class).testerName).isPresent()) {
                Logger.warn("Somebody else is syncing " + address.path() + ", so the answers were not sent");
                return false;
            }

            try {
                final @NotNull Map<String, Manifest.Entry> onServer =
                        new TreeMap<>(readManifest(transport, mapper).entries());
                final @NotNull Map<String, String> agreed =
                        new TreeMap<>(BaselineStore.read(mapper, baselineFile).contents());
                final @NotNull Map<String, byte[]> incoming = new TreeMap<>();

                answered.forEach((path, settled) -> {
                    final byte @NotNull [] content = settled.getBytes(StandardCharsets.UTF_8);

                    transport.write(path, content);
                    incoming.put(path, content);
                    onServer.put(path, Manifest.Entry.of(content));
                    agreed.put(path, settled);
                });

                indexer.acceptIncoming(projectRoot, incoming);
                writeManifest(transport, mapper, new Manifest(onServer));
                BaselineStore.write(mapper, baselineFile, new Baseline(agreed));

                Logger.info("Settled " + answered.size() + " test cases on both sides of " + address.display());
                return true;
            } finally {
                lock.release();
            }
        }
    }

    /**
     * Settles what it can of the files both sides changed, and hands back the
     * rest as questions (#94).
     * <p>
     * A test case is JSON with named fields, so two testers editing different
     * ones is not a conflict at all - it only looked like one because the sync
     * compares whole files. {@link TestCaseMerge} is the same three-way merge
     * the Git channel uses, on the same three versions: what both sides last
     * agreed, what this machine holds, and what the server holds. Being the same
     * one matters more than being here at all - a team whose test cases merge
     * one way over Git and another way over SSH has two answers to one question.
     * <p>
     * A settled case is written to both sides at once and recorded as agreed, so
     * the next sync sees nothing to do. An unsettled one is left exactly as it
     * was on both sides: nothing is sent, nothing is fetched, and the baseline
     * keeps its old entry, so the case is still unsettled next time rather than
     * one side having quietly won.
     * <p>
     * Anything that is not a test case stays a conflict. A marker or a run has no
     * fields to merge, and which of two versions a team meant is not a question
     * about bytes.
     */
    private static @NotNull List<Unsettled> settle(final @NotNull SftpTransport transport, final @NotNull Mapper mapper, final @NotNull ProgressIndicator indicator, final @NotNull Plan plan, final @NotNull Map<String, byte[]> local, final @NotNull Map<String, Manifest.Entry> onServer, final @NotNull Map<String, String> agreed, final @NotNull Map<String, byte[]> incoming) {
        final @NotNull List<Unsettled> unsettled = new ArrayList<>();
        final @NotNull List<String> mergeable = plan.conflicting.stream()
                .filter(TestCaseMerge::isTestCase)
                .toList();

        if (mergeable.isEmpty()) return List.of();

        indicator.setText("Merging " + mergeable.size() + " changed on both sides...");

        for (final String path : mergeable) {
            indicator.setText2(path);

            final @NotNull String base = agreed.getOrDefault(path, "");
            final @NotNull String mine = text(local.getOrDefault(path, new byte[0]));
            // Read only while the server still holds it. A case the server
            // deleted is not there to fetch, and asking for it throws and takes
            // the whole sync down with it - so an absent side is read as blank,
            // which is exactly the delete-versus-edit the next line hands over.
            final @NotNull String theirs = onServer.containsKey(path) ? text(transport.read(path)) : "";

            // One side deleted the case and the other edited it. Which of those
            // a team meant is not a field question, so it stays with the tester.
            if (mine.isBlank() || theirs.isBlank()) continue;

            final @NotNull TestCaseMerge.Merge merge = TestCaseMerge.of(mapper, base, mine, theirs);

            if (!merge.isSettled()) {
                unsettled.add(new Unsettled(path, name(mapper, mine, path), merge.merged(),
                        merge.questions(), theirs));
                continue;
            }

            keep(transport, mapper, merge.merged(), path, onServer, agreed, incoming);
            plan.settled.add(path);
        }

        plan.conflicting.removeAll(plan.settled);

        return List.copyOf(unsettled);
    }

    /**
     * Writes one merged case to both sides and records that they now agree.
     * <p>
     * The same bytes to the server and into this machine's copy, from the one
     * serialization: writing each side from its own would let the two differ by
     * a space and conflict again on the next sync forever.
     */
    private static void keep(final @NotNull SftpTransport transport, final @NotNull Mapper mapper, final @NotNull ObjectNode merged, final @NotNull String path, final @NotNull Map<String, Manifest.Entry> onServer, final @NotNull Map<String, String> agreed, final @NotNull Map<String, byte[]> incoming) {
        final @NotNull String settled = mapper.writeValueAsString(merged);
        final byte @NotNull [] content = settled.getBytes(StandardCharsets.UTF_8);

        transport.write(path, content);
        incoming.put(path, content);
        onServer.put(path, Manifest.Entry.of(content));
        agreed.put(path, settled);
    }

    /**
     * What the case is called, for the dialog title - its description, and its
     * file name when the description is blank or the side being read will not
     * parse.
     */
    private static @NotNull String name(final @NotNull Mapper mapper, final @NotNull String json, final @NotNull String path) {
        final @NotNull String description = mapper.readTree(json).path("description").asText("");
        if (!description.isBlank()) return description;

        return Path.of(path).getFileName().toString();
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
     * What a sync would do, without doing any of it.
     * <p>
     * Here so the decision can be driven against a real server without a running
     * IDE. {@link #run} needs a project - the indexer reads this machine's copy
     * and the mapper reads the server's manifest - and neither can be built
     * outside one, so a test of the sync had either to skip or to reimplement
     * this loop and test its own copy of it.
     */
    static @NotNull Outcome wouldDo(final @NotNull Manifest local, final @NotNull Manifest remote, final @NotNull Manifest base) {
        return decide(local, remote, base).outcome(List.of());
    }

    /**
     * What every file's three states say should happen to it.
     */
    private static @NotNull Plan decide(final @NotNull Manifest local, final @NotNull Manifest remote, final @NotNull Manifest base) {
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

    private static void writeManifest(final @NotNull SftpTransport transport, final @NotNull Mapper mapper, final @NotNull Manifest manifest) {
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

        /**
         * The ones the field merge finished, which are no longer conflicts and
         * are counted as their own outcome - a tester who is told "merged 3"
         * knows something happened that they did not have to do.
         */
        private final @NotNull List<String> settled = new ArrayList<>();

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

        private @NotNull Outcome outcome(final @NotNull List<Unsettled> unsettled) {
            return new Outcome(toUpload.size(), toDownload.size(), unchangedPaths.size(), settled.size(),
                    conflicting.size(), List.copyOf(conflicting), List.copyOf(removedOnServer),
                    unsettled, "");
        }
    }
}
