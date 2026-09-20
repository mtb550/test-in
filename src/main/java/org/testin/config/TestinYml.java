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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Rule-INTERNAL-089.
 * <p>
 * The one class that reads an automation repository's {@code testin.yml} (#6),
 * and the one that writes it - for Save to testin.yml, which is the only thing
 * that ever does (#335). The file is the team's: when it is there, what it says
 * is read from here; when it is not, or it leaves a key out, every answer below
 * is empty and the caller goes on without it. Only the code features need it
 * (Rule-CODEGEN-082).
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
    /*
     * The shape of an address rather than anything about this file, so it does
     * not belong here - and it stays anyway. Its two readers are
     * GitRefs.isRepositoryUrl and BugRepository, one in git and one in config;
     * git already depends on config, so moving it to git would make config
     * depend on git and close the cycle ArchitectureTest calls "a cycle waiting
     * for its second edge" (#112). It is four characters in one place, which is
     * cheaper than the edge (#301, D10).
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
     * Reads the file again: on Refresh, before Report Bug sends, and after Save
     * to testin.yml writes it.
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
     * Rule-CODEGEN-082.
     * <p>
     * Whether the file names this test project - the one question behind code
     * being on, a clone address being offered, and a rename being told the file
     * still names the old name. Never for an empty name: a file that names
     * nothing names no project.
     */
    public static boolean names(final @NotNull Project p, final @NotNull String projectName) {
        return !projectName.isEmpty() && projectName.equals(projectName(p));
    }

    /**
     * UC-TREE-PANEL-001, UC-TREE-PANEL-003, Rule-SHARE-060.
     * <p>
     * Where this test project is cloned from, and pushed to when its folder has
     * no remote yet: the file's {@code RepoUrl}, only while the file names this
     * project and says it is shared. The address is the named project's, so no
     * other project is cloned from it or pushed to it (#301, R7).
     */
    public static @NotNull Optional<String> cloneAddress(final @NotNull Project p, final @NotNull String projectName) {
        final @NotNull TestinProjectConfig config = config(p);
        return names(p, projectName) && config.hasRepoUrl() ? Optional.of(config.repoUrl()) : Optional.empty();
    }

    /**
     * UC-TREE-PANEL-003.
     * <p>
     * Whether this address is the one the file gives, compared with the
     * credentials taken out as the file's own is.
     */
    public static boolean isRepoUrl(final @NotNull Project p, final @NotNull String address) {
        final @NotNull TestinProjectConfig config = config(p);
        return config.hasRepoUrl() && config.repoUrl().equals(TestinProjectConfig.withoutCredentials(address));
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
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-113.
     * <p>
     * The line Save to testin.yml writes when Git cannot be asked: the project,
     * and nothing about where it lives - without the Git plugin, calling a Git
     * project local would be a guess, so those lines are left as they are.
     */
    public static @NotNull Map<String, String> lines(final @NotNull String projectName) {
        final @NotNull Map<String, String> lines = new LinkedHashMap<>();
        lines.put(TestinProjectConfig.PROJECT_KEY, projectName);
        return lines;
    }

    /**
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-113, Rule-SHARE-004.
     * <p>
     * The lines Save to testin.yml writes when Git said where the project
     * lives: a remote gives {@code location: remote} and its address with any
     * account and token taken out - the file is committed - and a folder with
     * none gives {@code location: local}.
     */
    public static @NotNull Map<String, String> lines(final @NotNull String projectName, final @NotNull String remote) {
        final @NotNull Map<String, String> lines = lines(projectName);
        lines.put(TestinProjectConfig.LOCATION_KEY, (remote.isEmpty() ? TestinLocation.LOCAL : TestinLocation.REMOTE).written());
        if (!remote.isEmpty()) lines.put(TestinProjectConfig.REPO_URL_KEY, TestinProjectConfig.withoutCredentials(remote));
        return lines;
    }

    /**
     * UC-TREE-PANEL-029.
     * <p>
     * Where Save to testin.yml writes: the file there is, whichever spelling, or
     * {@code testin.yml} in the code project's folder. Empty for a project with
     * no folder.
     */
    public static @NotNull Optional<Path> savePath(final @NotNull Project p) {
        return file(p).or(() -> Optional.ofNullable(p.getBasePath()).map(base -> Path.of(base).resolve(FILE_NAMES[0])));
    }

    /**
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-113.
     * <p>
     * What the file says now for each of these keys, as written, and nothing
     * for a key it does not have - so the preview can say what a save changes.
     */
    public static @NotNull Map<String, String> writtenValues(final @NotNull Project p, final @NotNull Set<String> keys) {
        return valuesIn(file(p).map(TestinYml::textOf).orElse(""), keys);
    }

    /**
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-112, Rule-TREE-PANEL-114.
     * <p>
     * Writes these lines into the file, creating it when there is none, and
     * reads it again. The only writer of {@code testin.yml}: Save to testin.yml
     * calls it after the tester has seen what it writes.
     * <p>
     * A file open in an editor is written through its document, so what the
     * tester typed there and has not saved is kept around the lines this
     * changes; any other is written as text. Either way only these lines
     * change ({@link #withLines}).
     */
    public static boolean save(final @NotNull Project p, final @NotNull Map<String, String> owned) {
        final @NotNull Optional<Path> path = savePath(p);
        final @NotNull Optional<VirtualFile> folder = path.map(Path::getParent).map(LocalFileSystem.getInstance()::refreshAndFindFileByNioFile);
        if (folder.isEmpty()) {
            Logger.warn("No folder to save testin.yml in for " + p.getName());
            return false;
        }

        final @NotNull String name = String.valueOf(path.orElseThrow().getFileName());
        final boolean saved = WriteCommandAction.writeCommandAction(p).withName(Bundle.message("yml.save.command"))
                .compute(() -> write(folder.orElseThrow(), name, owned));
        reload(p);
        return saved;
    }

    private static boolean write(final @NotNull VirtualFile folder, final @NotNull String name, final @NotNull Map<String, String> owned) {
        try {
            final @NotNull Optional<VirtualFile> existing = Optional.ofNullable(folder.findChild(name));
            final @NotNull VirtualFile file = existing.isPresent() ? existing.orElseThrow() : folder.createChildData(TestinYml.class, name);
            final @NotNull Optional<Document> open = Optional.ofNullable(FileDocumentManager.getInstance().getCachedDocument(file));

            if (open.isPresent()) {
                final @NotNull Document document = open.orElseThrow();
                document.setText(withLines(document.getText(), owned));
                FileDocumentManager.getInstance().saveDocument(document);
            } else {
                VfsUtil.saveText(file, withLines(VfsUtilCore.loadText(file), owned));
            }

            Logger.info("Saved " + file.getPath() + ": " + owned);
            return true;
        } catch (final IOException ex) {
            Logger.warn("Could not save testin.yml in " + folder.getPath() + ": " + ex.getMessage());
            return false;
        }
    }

    /**
     * Rule-TREE-PANEL-114.
     * <p>
     * The file's text with each owned key's line set to its value - in place
     * where the file has that key at the top level, every such line when it has
     * the key twice so the reader cannot find the old value after it, and at the
     * end where it has none - and every other line as it was, comments and keys
     * Testin does not know included. A file that ends without a newline keeps
     * that ending unless a line has to be added. Its line separator is kept.
     */
    static @NotNull String withLines(final @NotNull String text, final @NotNull Map<String, String> owned) {
        final @NotNull String separator = text.contains("\r\n") ? "\r\n" : "\n";
        final @NotNull List<String> lines = new ArrayList<>(text.isEmpty() ? List.of() : Arrays.asList(text.replace("\r\n", "\n").split("\n", -1)));

        final boolean endedWithNewline = !lines.isEmpty() && lines.getLast().isEmpty();
        if (endedWithNewline) lines.removeLast();

        final @NotNull Set<String> placed = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            final int at = i;
            keyOf(lines.get(i), owned.keySet()).ifPresent(key -> {
                lines.set(at, line(key, owned.get(key)));
                placed.add(key);
            });
        }

        final int before = lines.size();
        owned.forEach((key, value) -> {
            if (!placed.contains(key)) lines.add(line(key, value));
        });

        return String.join(separator, lines) + (endedWithNewline || lines.size() > before ? separator : "");
    }

    /**
     * What each of these keys holds in the text, as the reader reads it - quotes
     * and a trailing comment are YAML's, not the value's - and nothing for a key
     * the text does not have, or for text that does not parse.
     */
    static @NotNull Map<String, String> valuesIn(final @NotNull String text, final @NotNull Set<String> keys) {
        if (text.isBlank()) return Map.of();

        try {
            final @NotNull Map<String, Object> read = Optional.ofNullable(YAML.readValue(text, new TypeReference<Map<String, Object>>() {
            })).orElse(Map.of());

            final @NotNull Map<String, String> values = new LinkedHashMap<>();
            keys.stream().filter(read::containsKey).forEach(key -> values.put(key, Objects.toString(read.get(key), "")));
            return values;
        } catch (final IOException ex) {
            Logger.warn("Could not read the values in testin.yml: " + ex.getMessage());
            return Map.of();
        }
    }

    /**
     * The owned key a line sets, when it sets one at the top level: indented
     * lines belong to something else, and a comment sets nothing.
     */
    private static @NotNull Optional<String> keyOf(final @NotNull String line, final @NotNull Set<String> keys) {
        return keys.stream().filter(key -> line.startsWith(key + ":")).findFirst();
    }

    private static @NotNull String line(final @NotNull String key, final @NotNull String value) {
        return key + ": " + scalar(value);
    }

    /**
     * The value as YAML reads it back unchanged: plain where plain means the
     * same text, single-quoted where it would not - a name starting with
     * {@code #} would be a comment, one holding {@code ": "} a second key, and
     * {@code null} or {@code true} not a name at all.
     */
    private static @NotNull String scalar(final @NotNull String value) {
        final boolean plain = !value.isEmpty()
                && value.equals(value.strip())
                && "-?:,[]{}#&*!|>'\"%@`".indexOf(value.charAt(0)) < 0
                && !value.contains(": ") && !value.contains(" #") && !value.endsWith(":")
                && !Set.of("null", "~", "true", "false", "yes", "no", "on", "off").contains(value.toLowerCase(Locale.ROOT));

        return plain ? value : "'" + value.replace("'", "''") + "'";
    }

    private static @NotNull String textOf(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            Logger.warn("Could not read " + file + ": " + ex.getMessage());
            return "";
        }
    }

    /**
     * UC-TREE-PANEL-011, Rule-TREE-PANEL-110.
     * <p>
     * Opens the file in an editor for the tester to change by hand - a rename
     * never writes it. Nothing when the repository has none.
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
