package org.testin.codegen.method.update;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.logger.Logger;

/**
 * Update action for data-only fields (module, test data, steps, order, ...):
 * the caller already persists the value via the indexer and there is no
 * {@code @Test} annotation attribute to change.
 */
@AllArgsConstructor
public final class NoOpCodeUpdate implements GenAction {

    private final @NotNull String fieldName;

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        Logger.info("Update " + fieldName + ": data-only field, no Java code change");
    }
}

