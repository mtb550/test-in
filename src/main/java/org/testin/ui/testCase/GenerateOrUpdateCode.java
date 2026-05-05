package org.testin.ui.testCase;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import org.testin.ui.testCase.update.UpdateTestCaseUI;

import java.awt.*;

// todo, to put it in new package and separate each action in separate class (create project package, create test set , create method, update method
public class GenerateOrUpdateCode extends JBCheckBox {

    public static final String PROP_KEY = "testin.CreateTestCase.GenerateCode";

    public GenerateOrUpdateCode(final TestCaseUIBase ui) {
        if (ui instanceof UpdateTestCaseUI) setToolTipText("Update automated test case");
        else setToolTipText("Create automated test case");

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(JBUI.Borders.emptyRight(8));

        setSelected(PropertiesComponent.getInstance().getBoolean(PROP_KEY, true));
        addItemListener(e -> PropertiesComponent.getInstance().setValue(PROP_KEY, isSelected(), true));
    }
}