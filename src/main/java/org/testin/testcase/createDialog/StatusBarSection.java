package org.testin.testcase.createDialog;

import org.testin.enums.CreateTestCaseFields;
import org.testin.statusbar.StatusBarBase;

public class StatusBarSection extends StatusBarBase {

    public StatusBarSection() {
        super(CreateTestCaseFields.DESCRIPTION.getStatusBarItems());
    }

}