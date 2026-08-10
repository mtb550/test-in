package org.testin.testRun.createDialog;

import org.testin.enums.BugPriority;
import org.testin.mappers.TestRunItems;

public class BugPrioritySection extends AbstractEnumRadioSection<BugPriority> {

    public BugPrioritySection() {
        super(BugPriority.values(), BugPriority::getName, BugPriority::valueOf,
                TestRunItems::getBugPriority, TestRunItems::setBugPriority);
    }
}
