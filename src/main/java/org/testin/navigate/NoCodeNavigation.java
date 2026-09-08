package org.testin.navigate;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

/**
 * What {@link CodeNavigation#available()} answers with in an IDE that has no
 * Java plugin, so the content module never loaded.
 * <p>
 * A class rather than a null or a lambda, the same reason {@code NoJavaCode}
 * exists beside {@code GenAction}: "there is no code here" is stated by the
 * type, and no caller asks whether navigation exists before asking it to
 * navigate.
 */
public final class NoCodeNavigation implements CodeNavigation {

    @Override
    public void toCode(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        Logger.debug("No code navigation in this IDE; nothing opened for '" + tc.getDescription() + "'");
    }

    /**
     * None of them, and said once rather than per case.
     * <p>
     * The caller must not read this as "none of these are automated": in an IDE
     * with no Java plugin nothing can be known, and {@code AutomationState} asks
     * {@code OptionalPlugin.JAVA} before it asks this.
     */
    @Override
    public @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        Logger.debug("No code navigation in this IDE; no generated methods for " + cases.size() + " test case(s)");

        return Map.of();
    }

    @Override
    public @NotNull Optional<List<String>> methodOf(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        Logger.debug("No code navigation in this IDE; no generated method for '" + tc.getDescription() + "'");

        return Optional.empty();
    }
}
