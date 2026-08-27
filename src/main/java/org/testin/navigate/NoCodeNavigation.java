package org.testin.navigate;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import java.util.Optional;

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

    @Override
    public @NotNull Optional<List<String>> methodOf(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        Logger.debug("No code navigation in this IDE; no generated method for '" + tc.getDescription() + "'");

        return Optional.empty();
    }
}
