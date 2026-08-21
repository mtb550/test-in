package org.testin.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.nio.file.Path;

/**
 * Finds and reads an automation repository's {@code testin.yml} (#6).
 * <p>
 * Startup always completes. A file that is missing, empty, unreadable or
 * malformed produces {@link TestinProjectConfig#EMPTY} and a line in the log,
 * because the answer to a broken config is an unbound panel that says how to
 * bind it, never a failed start.
 * <p>
 * Read with {@code java.nio} rather than through the VFS: this runs before
 * indexing, off the EDT, on a file the plugin has never opened.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestinConfigLoader {

    /**
     * Both spellings, because both are written by hand and neither is wrong.
     * {@code testin.yml} is first, and is what the plugin writes.
     */
    private static final @NotNull String[] FILE_NAMES = {"testin.yml", "testin.yaml"};

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
                public boolean handleUnknownProperty(final @NotNull DeserializationContext context,
                                                     final @NotNull JsonParser parser,
                                                     final @NotNull JsonDeserializer<?> deserializer,
                                                     final @NotNull Object beanOrClass,
                                                     final @NotNull String key) throws IOException {
                    Logger.warn("Unknown key in testin.yml, ignored: " + key);
                    parser.skipChildren();
                    return true;
                }
            });

    static @NotNull TestinProjectConfig load(final @NotNull Project p) {
        return file(p)
                .filter(Files::isRegularFile)
                .map(TestinConfigLoader::read)
                .orElseGet(() -> {
                    Logger.info("No testin.yml in " + p.getName()
                            + ", so this repository is not bound to a test project");
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
            final TestinProjectConfig config = YAML.readValue(yaml, TestinProjectConfig.class);
            Logger.info("Read " + source + ": test project '" + config.testinProject() + "'");
            return config;

        } catch (final Exception ex) {
            // A hand-edited file: the reason belongs in the log, and the plugin
            // carries on unbound rather than refusing to open the project.
            Logger.warn("Malformed " + source + ", ignored: " + ex.getMessage());
            return TestinProjectConfig.EMPTY;
        }
    }

    /**
     * The config file of this repository: whichever spelling is on disk, or where
     * {@code testin.yml} would go when neither is. Null only when the project has
     * no base path at all, which is the one case with nowhere to read from and
     * nowhere to write to.
     * <p>
     * Base path only, deliberately: a multi-module repository carries the file at
     * its root, which is where a clone puts it.
     */
    static @NotNull Optional<Path> file(final @NotNull Project p) {
        final String basePath = p.getBasePath();
        if (basePath == null) {
            Logger.info("No base path for " + p.getName() + ", so there is no testin.yml to read or write");
            return Optional.empty();
        }

        final Path root = Path.of(basePath);
        for (final String name : FILE_NAMES) {
            final Path candidate = root.resolve(name);
            if (Files.isRegularFile(candidate)) return Optional.of(candidate);
        }

        // Nothing there yet, so the first name is where a write would put it.
        return Optional.of(root.resolve(FILE_NAMES[0]));
    }
}
