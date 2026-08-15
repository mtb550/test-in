package org.testin.testcase.create;

import org.testin.statusbar.StatusBarBase;
import org.testin.testcase.CreateTestCaseFields;

public class StatusBarSection extends StatusBarBase {

    public StatusBarSection() {
        super(CreateTestCaseFields.DESCRIPTION.getStatusBarItems());
    }

}