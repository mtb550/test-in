package org.testin.testcase.create;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Set;

/**
 * UC-EDITOR-PANEL-005.
 * <p>
 * The groups a test case is filed under, one box each, added with CTRL+G.
 * <p>
 * It was a row of tick boxes, one per constant of an enum - so the groups a team
 * could use were the groups somebody shipped, and the row grew wider every time
 * one was added. #200 made all eight visible, which was right and made the shape
 * plain: a list that only grows, drawn as a control that only widens.
 * <p>
 * A group is a word now, completed from every group the project has used, so
 * this is the same section the steps are: type to add one, clear the box to take
 * it off (#296).
 */
public class GroupSection extends AbstractMultiValueSection {

    public GroupSection(final @NotNull Project p) {
        super(p);
    }

    @Override
    protected @NotNull CreateTestCaseFields field() {
        return CreateTestCaseFields.GROUP;
    }

    @Override
    protected @NotNull Set<String> completions(final @NotNull TestCaseValues cache) {
        return cache.getGroups();
    }

    @Override
    protected @NotNull List<String> valuesOf(final @NotNull TestCaseDto dto) {
        return dto.getGroup();
    }

    @Override
    protected void write(final @NotNull TestCaseDto dto, final @NotNull List<String> values) {
        dto.setGroup(values);
    }

    @Override
    protected @NotNull Shortcuts addKey() {
        return Shortcuts.CreateTestCaseGroup;
    }
}
