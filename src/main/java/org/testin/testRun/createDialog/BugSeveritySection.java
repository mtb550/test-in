package org.testin.testRun.createDialog;

import org.testin.enums.BugSeverity;
import org.testin.mappers.TestRunItems;

public class BugSeveritySection extends AbstractEnumRadioSection<BugSeverity> {

    public BugSeveritySection() {
        super(BugSeverity.values(), BugSeverity::getName, BugSeverity::valueOf,
                TestRunItems::getBugSeverity, TestRunItems::setBugSeverity);
    }
}
