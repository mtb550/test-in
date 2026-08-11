package org.testin.mappers.dto.dirs;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.CreateNodeMenu;
import org.testin.mappers.markers.IMarker;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.Config;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString()
public abstract class DirectoryDto {
    @NonNull
    @Builder.Default
    private String name = "";

    @NonNull
    @Builder.Default
    private Path path = Path.of("");

    @NonNull
    @Builder.Default
    private ArrayList<String> path2 = new ArrayList<>();

    @ToString.Exclude
    private DirectoryDto parent;

    /**
     * The node's marker; each subtype's Lombok-generated getter returns its
     * concrete marker type and satisfies this by covariant return. Audit info
     * (created/modified by and when) lives only here — the marker JSON is the
     * persisted truth, the DTO stores none of it.
     */
    @NonNull
    public abstract IMarker getMarker();

    /** File name of this node's marker JSON inside the directory. */
    @NonNull
    public abstract String getMarkerFileName();

    @NonNull
    public abstract CreateNodeMenu getMenu();

    @Nullable
    public abstract Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer);

    // Capability flags replace the instanceof chains that used to branch on
    // node type across the actions (issue #37): a new node type declares what
    // it supports here instead of being hunted for at every call site.

    /** True when the user may rename this node; the fixed root containers say no. */
    public boolean isRenamable() {
        return true;
    }

    /** True when test cases can be imported into or exported from this node. */
    public boolean isTestCaseContainer() {
        return false;
    }

    /** True when the node opens in an editor tab (test sets and test runs). */
    public boolean isOpenableInEditor() {
        return false;
    }
}
