package testGit.editorPanel.toolBar.components;

import com.intellij.util.ui.JBUI;
import testGit.util.IconManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ToolbarActionButton extends JButton {
    public ToolbarActionButton(String tooltip, Icon icon) {
        super(null, icon);
        setToolTipText(tooltip);
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        final Icon zoomedIcon = IconManager.zoomStandardIcon(icon, this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                setContentAreaFilled(true);
                setBackground(JBUI.CurrentTheme.ActionButton.hoverBackground());
                setIcon(zoomedIcon);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                setContentAreaFilled(false);
                setIcon(icon);
            }
        });

    }

}