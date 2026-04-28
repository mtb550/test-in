package testGit.ui.TestCase;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import testGit.ui.TestCase.update.UpdateTestCaseUI;

import java.awt.*;

public class GenerateOrUpdateCodeCheckBox extends JBCheckBox {

    public static final String PROP_KEY = "TestGit.CreateTestCase.GenerateCode";

    public GenerateOrUpdateCodeCheckBox(final TestCaseUIBase ui) {
        if (ui instanceof UpdateTestCaseUI) setToolTipText("Update automated test case");
        else setToolTipText("Create automated test case");

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(JBUI.Borders.emptyRight(8));

        setSelected(PropertiesComponent.getInstance().getBoolean(PROP_KEY, true));
        addItemListener(e -> PropertiesComponent.getInstance().setValue(PROP_KEY, isSelected(), true));
    }
}