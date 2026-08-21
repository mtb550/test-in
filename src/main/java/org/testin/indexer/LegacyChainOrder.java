package org.testin.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.util.Mapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Reads the order out of a test set written before test cases carried their own
 * rank, once, and gives the cases ranks that mean the same thing.
 * <p>
 * Order used to be a chain in the files: each case named the next one and one
 * called itself the head. Those two keys are gone from the model, so a set
 * written by an older build would otherwise come back in creation order - which
 * is not what its tester arranged, and looks to them like the plugin shuffled
 * their work.
 * <p>
 * So the chain is read from the raw JSON, which still has it, and turned into
 * ranks in the same order. It runs when a set has no ranks at all, which is true
 * once and never again.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class LegacyChainOrder {

    private static final @NotNull String HEAD = "isHead";
    private static final @NotNull String NEXT = "next";

    /**
     * Gives a set of unranked cases the order its chain described, or leaves
     * them alone when there is no chain to read - an empty set, or one an
     * importer wrote without links.
     *
     * @param files the case files as they are on disk, by the case they hold
     * @return the cases whose rank was decided here, in order, for the caller to
     *         write
     */
    static @NotNull List<TestCaseDto> apply(final @NotNull Project p, final @NotNull Map<Path, TestCaseDto> files) {
        if (files.isEmpty() || files.values().stream().anyMatch(tc -> !tc.getOrder().isEmpty())) return List.of();

        final Map<UUID, TestCaseDto> byId = new HashMap<>(files.size());
        final Map<UUID, UUID> next = new HashMap<>(files.size());
        Optional<UUID> head = Optional.empty();

        for (final Map.Entry<Path, TestCaseDto> file : files.entrySet()) {
            final JsonNode chain = read(p, file.getKey());
            final TestCaseDto testCase = file.getValue();

            byId.put(testCase.getId(), testCase);

            if (chain.path(HEAD).asBoolean(false)) head = Optional.of(testCase.getId());

            final String points = chain.path(NEXT).asText("");
            if (!points.isBlank()) {
                try {
                    next.put(testCase.getId(), UUID.fromString(points));
                } catch (final IllegalArgumentException ex) {
                    Logger.warn("Ignoring an unreadable next pointer in " + file.getKey().getFileName());
                }
            }
        }

        if (head.isEmpty()) return List.of();

        final List<TestCaseDto> ordered = new ArrayList<>(files.size());
        final Set<UUID> walked = new HashSet<>();

        // The chain ends where a case points at nobody, and a pointer that leads
        // back to a case already walked ends it too - a file written by two
        // testers at once could otherwise loop forever.
        UUID id = head.orElseThrow();
        while (walked.add(id)) {
            Optional.ofNullable(byId.get(id)).ifPresent(ordered::add);
            if (!next.containsKey(id)) break;

            id = next.get(id);
        }

        // Whatever the chain never reached goes on the end, in the order the
        // comparator would have put it - the same answer the editor used to give
        // by badging them Unsorted and showing them last.
        for (final TestCaseDto testCase : TestCaseOrder.ordered(new ArrayList<>(files.values()))) {
            if (!walked.contains(testCase.getId())) ordered.add(testCase);
        }

        TestCaseOrder.rankAll(ordered);
        Logger.info("Converted the order of " + ordered.size() + " test cases from a chain to ranks");

        return List.copyOf(ordered);
    }

    /**
     * The two keys the old format carried, straight from the file. The model no
     * longer has them, so the tree is the only place left that does.
     */
    private static @NotNull JsonNode read(final @NotNull Project p, final @NotNull Path file) {
        try {
            return Services.getInstance(p, Mapper.class).readTree(Files.readString(file, StandardCharsets.UTF_8));

        } catch (final Exception ex) {
            Logger.warn("Could not read the old order from " + file.getFileName() + ": " + ex.getMessage());
            return Services.getInstance(p, Mapper.class).createObjectNode();
        }
    }
}
