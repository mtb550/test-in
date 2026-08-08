package org.testin.enums;

import org.testin.testRun.updateDialog.RunItemEditSection;
import org.testin.testRun.updateDialog.UpdateRunItemDialog;

@FunctionalInterface
public interface SectionExtractor {
    RunItemEditSection create(final UpdateRunItemDialog ui);
}
