package org.testin.codegen;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

/**
 * The generator of the node types that produce no Java: the two fixed root
 * containers, test run packages and test runs. Test cases carry the automation,
 * and a run only records what was executed.
 * <p>
 * A class rather than a null on {@link org.testin.enums.DirectoryType}, so
 * "generates nothing" is stated by the type instead of every caller having to
 * ask whether there is a generator at all.
 */
@AllArgsConstructor
public final class NoJavaCode implements GeneratorAction {

    private final @NotNull String nodeType;

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        Logger.debug("Create " + nodeType + ": generates no Java code");
    }
}

