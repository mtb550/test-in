package org.testin.util.automationGenerator;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class GenerateOrUpdateCode extends JBCheckBox {
    // todo, put all stored props in separate class.
    private final String PROP_KEY = "testin.automation.generateCode";
    private int typeOfUpdate; // todo, here to put the type of update -> group or priority so you can assign the proper update class based on that

    public GenerateOrUpdateCode(final @NotNull GeneratorType generatorType) {
        setToolTipText(generatorType.getTooltip());

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(JBUI.Borders.emptyRight(8));

        final PropertiesComponent properties = PropertiesComponent.getInstance();
        setSelected(properties.getBoolean(PROP_KEY, true));

        addItemListener(e -> properties.setValue(PROP_KEY, isSelected(), true));
    }
}