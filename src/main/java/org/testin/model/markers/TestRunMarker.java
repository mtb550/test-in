package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunStatus;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestRunMarker extends AbstractMarker {
    @NonNull
    private TestRunStatus status = TestRunStatus.CREATED;

    /**
     * What the tester answered when the run was created, kept under the field
     * that asked.
     * <p>
     * On the marker so the node can describe itself without its run file being
     * opened: the tree holds every marker in memory already, so Details on a run
     * - from the tree or from the run's own toolbar - is a lookup rather than a
     * read.
     * <p>
     * One map rather than a field per question. The captions, the order and the
     * set of questions all belong to {@link TestRunConfiguration}, so a field
     * added there is stored, shown and reported without this class changing -
     * and there is no second list of names here to drift from the form's.
     * <p>
     * Left out of the file when nothing was answered, so a marker written for a
     * run with no configuration reads as it always did.
     */
    @NonNull
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<TestRunConfiguration, String> configuration = new EnumMap<>(TestRunConfiguration.class);

    @JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }

    /**
     * The configuration, in the order the form asked for it and under the names
     * it used.
     * <p>
     * Every question is offered, answered or not: a blank one is dropped by the
     * details builder, so a web run does not show an empty Device Type and this
     * does not have to know that it should not.
     */
    @JsonIgnore
    @Override
    public @NotNull List<DetailRow> getDetailRows() {
        return Arrays.stream(TestRunConfiguration.values())
                .map(field -> new DetailRow(field.getDisplayName(), configuration.getOrDefault(field, "")))
                .toList();
    }
}
