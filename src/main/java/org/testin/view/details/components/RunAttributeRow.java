package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.RunEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;

import java.awt.*;

/**
 * A row of what a run recorded about the case on display: the verdict it
 * reached, how long it took, what actually happened, the stacktrace behind it,
 * and how bad the bug is.
 * <p>
 * The counterpart of {@link AttributeRow}, and separate from it because the two
 * read different things. A case attribute is a fact about the case wherever it
 * is shown; a run attribute is a fact about one execution of it, and the same
 * case in two runs has two different answers. Handing one row type both
 * vocabularies would mean a row that has to ask which kind it is.
 * <p>
 * The run item is held rather than looked up, so the row is built only for a
 * case that has one and never has to answer what it would draw without one -
 * {@code DetailsTab} decides that once, where it builds the list.
 * <p>
 * The {@code dto} parameter is ignored: it is the case, and this row is not
 * about the case. It stays in the signature because every other row needs it.
 */
public final class RunAttributeRow extends BaseDetails {

    private final @NotNull RunEditorAttributes attribute;
    private final @NotNull TestRunItems item;

    public RunAttributeRow(final @NotNull RunEditorAttributes attribute, final @NotNull TestRunItems item) {
        this.attribute = attribute;
        this.item = item;
    }

    /**
     * Nothing recorded draws no row. A case that passed carries no bug severity
     * and no stacktrace - the verdict cleared them, because a case that passed
     * has nothing to explain - and a row labelled with a blank beside it is a
     * line the tester reads on every pass and needed on none.
     * <p>
     * Not decided here: {@link LabelValueRow} already drops a blank value and
     * leaves the row number where it was, which is the same rule the marker
     * popup relies on.
     */
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, attribute.getName(), attribute.getRunValueExtractor().execute(item, p), currentRow);
    }
}
