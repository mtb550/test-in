package org.testin.testcase.create;

import org.testin.enums.CreateTestCaseFields;
import org.testin.statusbar.StatusBarBase;

public class StatusBarSection extends StatusBarBase {

    public StatusBarSection() {
        super(CreateTestCaseFields.DESCRIPTION.getStatusBarItems());
    }

}