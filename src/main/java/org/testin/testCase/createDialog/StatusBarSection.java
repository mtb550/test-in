package org.testin.testCase.createDialog;

import org.testin.enums.CreateTestCaseFields;
import org.testin.util.statusBar.StatusBarBase;

public class StatusBarSection extends StatusBarBase {

    public StatusBarSection() {
        super(CreateTestCaseFields.DESCRIPTION.getStatusBarItems());
    }

}