package org.testin.report.generators;

import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.Map;
import java.util.UUID;

/**
 * The test case a run item points at.
 * <p>
 * A run outlives the cases it ran. One can be deleted afterwards, or belong to
 * a test set this machine never pulled, and its id then finds nothing in the
 * details map the report was handed.
 * <p>
 * Shared because all four formats tested for that themselves and each printed
 * something different: Excel wrote "N/A", HTML wrote nothing, PDF and Word wrote
 * an em dash. They ask here now and get a case whose fields are empty rather
 * than no case at all, so what a format prints for a blank description is a
 * display decision it makes once, in one place, for both reasons a description
 * can be blank.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ReportedCase {

    static @NotNull TestCaseDto of(final @NotNull Map<UUID, TestCaseDto> detailsMap, final @NotNull UUID id) {
        return detailsMap.getOrDefault(id, new TestCaseDto());
    }
}
