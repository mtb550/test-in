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

package org.testin.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Rule-INTERNAL-088.
 * <p>
 * The one class that reads an automation repository's {@code testin.yml} (#6),
 * and no class writes it (#301). The file is the team's: when it is there, what
 * it says is read from here; when it is not, or it leaves a key out, every
 * answer below is empty and the caller goes on without it - nothing in Testin
 * needs the file to exist.
 * <p>
 * Nothing else may open it or name what it holds: the values it parses into are
 * package-private ({@link TestinProjectConfig}), and {@code ArchitectureTest}
 * keeps the YAML parser inside this class. So a question about the file has one
 * answer, asked here, instead of a raw record handed to every caller to decide
 * for itself what a missing value means.
 * <p>
 * Read once per IDE project and kept with it, then again on {@link #reload} -
 * the tester's Refresh, or Report Bug about to send. Read with {@code java.nio}
 * rather than through the VFS: the first read comes before indexing, on a file
 * the plugin has never opened. Static rather than a service because it holds
 * nothing of its own; what it read is kept on the project it was read for.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestinYml {

    /**
     * Both spellings, because both are written by hand and neither is wrong.
     */
    private static final @NotNull String[] FILE_NAMES = {"testin.yml", "testin.yaml"};

    /**
     * How an SSH address written without a scheme begins: {@code git@host:owner/repo}.
     * One owner, because the file's own check, Git's check and Report Bug's
     * parser all read it (#66, finding 149).
     */
    public static final @NotNull String SCP_PREFIX = "git@";

    /**
     * What was read for a project, kept on that project so two open repositories
     * never share an answer.
     */
    private static final @NotNull Key<TestinProjectConfig> READ = Key.create("testin.yml");

    /**
     * Unknown keys are ignored, and each one is named in the log.
     * <p>
     * The handler is what names it. {@code FAIL_ON_UNKNOWN_PROPERTIES} alone
     * would ignore the key silently, and a tester who mistyped {@code testinProject}
     * would see an unbound repository with nothing anywhere saying why.
     */
    private static final @NotNull ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .addHandler(new DeserializationProblemHandler() {
                @Override
                public boolean handleUnknownProperty(final @NotNull DeserializationContext context, final @NotNull JsonParser parser, final @NotNull JsonDeserializer<?> deserializer, final @NotNull Object beanOrClass, final @NotNull String key) {
                    Logger.warn("Unknown key in testin.yml, ignored: " + key);

                    // Jackson's contract allows this to throw; the plugin's does
                    // not, and there is nothing to propagate anyway. Failing to
                    // skip a key already being ignored is still a key ignored.
                    try {
                        parser.skipChildren();
                    } catch (final IOException ex) {
                        Logger.warn("Could not skip past " + key + " in testin.yml: " + ex.getMessage());
                    }

                    return true;
                }
            });

    /**
     * What to call the file when telling a tester about it.
     */
    public static @NotNull String fileName() {
        return FILE_NAMES[0];
    }

    /**
     * Reads the file again - after a branch switch brought a different revision
     * of it, or after a tester edited it by hand.
     */
    public static void reload(final @NotNull Project p) {
        p.putUserData(READ, load(p));
    }

    /**
     * UC-TREE-PANEL-001.
     * <p>
     * The file is there and could not be read. Everything else below then
     * answers as though it were absent.
     */
    public static boolean isUnreadable(final @NotNull Project p) {
        return config(p).isUnreadable();
    }

    /**
     * The test project the file names, empty when it names none.
     */
    public static @NotNull String projectName(final @NotNull Project p) {
        return config(p).projectName();
    }

    /**
     * Where the file says the test project is cloned from, empty when it gives
     * no clone address.
     */
    public static @NotNull String repoUrl(final @NotNull Project p) {
        return config(p).repoUrl();
    }

    /**
     * Whether the file gives a clone address for the project it names.
     */
    public static boolean hasRepoUrl(final @NotNull Project p) {
        return config(p).hasRepoUrl();
    }

    /**
     * UC-TREE-PANEL-003.
     * <p>
     * Whether this address is the one the file gives, compared with the
     * credentials taken out as the file's own is.
     */
    public static boolean isRepoUrl(final @NotNull Project p, final @NotNull String address) {
        final @NotNull String given = repoUrl(p);
        return !given.isEmpty() && given.equals(TestinProjectConfig.withoutCredentials(address.strip()));
    }

    /**
     * The development repository Report Bug files issues in, as the file wrote
     * it with any credentials taken out; empty when it gives none.
     */
    public static @NotNull String bugRepoUrl(final @NotNull Project p) {
        return config(p).bugRepoUrl();
    }

    /**
     * That repository, and empty both when the file gives none and when what it
     * gives names no repository.
     */
    public static @NotNull Optional<BugRepository> bugRepository(final @NotNull Project p) {
        return config(p).bugRepository();
    }

    /**
     * UC-TREE-PANEL-011, Rule-TREE-PANEL-110.
     * <p>
     * Opens the file in an editor for the tester to change - the one way a
     * value in it is corrected, because Testin never writes it. Nothing when the
     * repository has none.
     */
    public static void openInEditor(final @NotNull Project p) {
        file(p).flatMap(path -> Optional.ofNullable(LocalFileSystem.getInstance().findFileByNioFile(path)))
                .ifPresentOrElse(found -> FileEditorManager.getInstance(p).openFile(found, true),
                        () -> Logger.warn("No testin.yml to open in " + p.getName()));
    }

    private static @NotNull TestinProjectConfig config(final @NotNull Project p) {
        return Optional.ofNullable(p.getUserData(READ)).orElseGet(() -> {
            final @NotNull TestinProjectConfig read = load(p);
            p.putUserData(READ, read);
            return read;
        });
    }

    /**
     * Startup always completes. A file that is missing, empty, unreadable or
     * malformed says nothing and leaves a line in the log, because the answer to
     * a broken config is a panel that says so, never a failed start.
     */
    private static @NotNull TestinProjectConfig load(final @NotNull Project p) {
        return file(p)
                .map(TestinYml::read)
                .orElseGet(() -> {
                    Logger.info("No testin.yml in " + p.getName() + "; Testin goes on without it");
                    return TestinProjectConfig.EMPTY;
                });
    }

    private static @NotNull TestinProjectConfig read(final @NotNull Path file) {
        try {
            return parse(Files.readString(file), file.toString());
        } catch (final IOException ex) {
            Logger.warn("Could not read " + file + ": " + ex.getMessage());
            return TestinProjectConfig.EMPTY;
        }
    }

    /**
     * The text of a config file becoming a config, whatever the text turns out to
     * be. Separate from {@link #load} so the parsing rules can be tested without
     * a project on disk.
     */
    static @NotNull TestinProjectConfig parse(final @NotNull String yaml, final @NotNull String source) {
        if (yaml.isBlank()) {
            Logger.warn("Empty testin.yml: " + source);
            return TestinProjectConfig.EMPTY;
        }

        try {
            final @NotNull TestinProjectConfig config = YAML.readValue(yaml, TestinProjectConfig.class);
            Logger.info("Read " + source + ": test project '" + config.projectName() + "'");
            return config;

        } catch (final Exception ex) {
            // A hand-edited file: the reason belongs in the log, and the plugin
            // carries on rather than refusing to open the project. UNREADABLE
            // rather than EMPTY, so the panel can say the file is broken instead
            // of saying nothing is chosen (#66, finding 10).
            Logger.warn("Malformed " + source + ", ignored: " + ex.getMessage());
            return TestinProjectConfig.UNREADABLE;
        }
    }

    /**
     * The repository's config file, whichever spelling is on disk, and empty
     * when there is none - or no base path to look in.
     * <p>
     * Base path only, deliberately: a multi-module repository carries the file at
     * its root, which is where a clone puts it.
     */
    private static @NotNull Optional<Path> file(final @NotNull Project p) {
        final @NotNull Optional<String> basePath = Optional.ofNullable(p.getBasePath());
        if (basePath.isEmpty()) return Optional.empty();

        final @NotNull Path root = Path.of(basePath.orElseThrow());
        for (final String name : FILE_NAMES) {
            final @NotNull Path candidate = root.resolve(name);
            if (Files.isRegularFile(candidate)) return Optional.of(candidate);
        }

        return Optional.empty();
    }
}
