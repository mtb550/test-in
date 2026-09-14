/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.bug;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.BugRepository;
import org.testin.git.GitRepositoryService;
import org.testin.indexer.TestCaseFile;
import org.testin.logger.Logger;
import org.testin.services.OptionalPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Where a bug report's test case can be opened on the web (#28).
 * <p>
 * Built from the test project folder's own Git remote and checked-out branch,
 * not from {@code repoUrl} in {@code testin.yml}, which can go stale while the
 * remote cannot. The link opens once the file has been pushed.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseLink {

    /**
     * UC-VIEW-PANEL-016, Rule-VIEW-PANEL-068.
     * <p>
     * The link, read off Git, and empty when it cannot be built: no Git plugin,
     * a test project folder that is not its repository's root, no remote, no
     * branch, or a remote that does not name a host, an owner and a repository.
     * <p>
     * Off the EDT: every answer here is a git command.
     */
    public static @NotNull Optional<String> read(final @NotNull Project p, final @NotNull TestCaseFile file) {
        if (!OptionalPlugin.GIT.isAvailable()) return Optional.empty();

        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        if (git.isNotRepository(file.testProject())) return Optional.empty();

        final @NotNull String remoteUrl = git.remoteUrl(file.testProject());
        if (remoteUrl.isEmpty()) return Optional.empty();

        return of(remoteUrl, git.getCurrentBranch(file.testProject()), file.inProject());
    }

    /**
     * The link from what Git answered: {@code https://HOST/OWNER/REPO/blob/BRANCH/PATH}.
     * A clone or SSH address becomes its web form, and {@code .git} is dropped -
     * the same reading {@code bugRepoUrl} gets.
     */
    static @NotNull Optional<String> of(final @NotNull String remoteUrl, final @NotNull String branch, final @NotNull Path inProject) {
        if (branch.isBlank()) return Optional.empty();

        return BugRepository.of(remoteUrl).flatMap(repository -> web(repository, branch, inProject));
    }

    /**
     * Percent-encoded, parentheses included: a Markdown link ends at the first
     * unbalanced one, and a test set may be called anything.
     */
    private static @NotNull Optional<String> web(final @NotNull BugRepository repository, final @NotNull String branch, final @NotNull Path inProject) {
        final @NotNull String file = StreamSupport.stream(inProject.spliterator(), false)
                .map(Path::toString)
                .collect(Collectors.joining("/"));
        final @NotNull String path = "/" + repository.owner() + "/" + repository.name() + "/blob/" + branch + "/" + file;

        try {
            return Optional.of(new URI("https", repository.host(), path, null).toASCIIString()
                    .replace("(", "%28")
                    .replace(")", "%29"));
        } catch (final URISyntaxException ex) {
            Logger.warn("No link to the test case's file could be built from " + path + ": " + ex.getMessage());
            return Optional.empty();
        }
    }
}
