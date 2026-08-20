package org.testin.creator;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The creator of the node types the tree cannot create under a selection.
 * <p>
 * Two different reasons, both ending here: the Test Cases and Test Runs
 * containers are made with their project and never by the tester, and a test
 * project has no parent node — it is created at the Testin root from the panel,
 * by name or by cloning a URL, which {@link NodeCreator#execute} has no
 * argument for.
 */
@AllArgsConstructor
public final class NotCreatableFromTree implements NodeCreator {

    private final @NotNull String nodeType;

    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final DirectoryDto parentDir,
                                                   final @NotNull Path newDirPath) {
        Logger.info(nodeType + " is not created from the tree");
        return Optional.empty();
    }
}

