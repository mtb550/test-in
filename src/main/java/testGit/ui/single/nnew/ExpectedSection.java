package testGit.ui.single.nnew;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import testGit.ui.bulk.UpdateField;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;

public class ExpectedSection {
    private final ExtendableTextField expectedField;
    private final JPanel wrapperPanel;
    private final String placeholder = UpdateField.EXPECTED.getLabel();
    private final Icon icon = UpdateField.EXPECTED.getIcon();
    private final float fontSize = JBUI.Fonts.label().getSize2D() + 2f;

    public ExpectedSection() {
        this.expectedField = new ExtendableTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && hasFocus()) {
                    try {
                        Rectangle2D r = modelToView2D(0);
                        if (r != null) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(UIUtil.getContextHelpForeground());
                            g2.setFont(getFont());
                            FontMetrics fm = g2.getFontMetrics();

                            int x = (int) r.getX() + JBUI.scale(1);
                            int y = (int) r.getY() + fm.getAscent();

                            g2.drawString(placeholder, x, y);
                            g2.dispose();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        Font fieldFont = JBFont.regular().deriveFont(fontSize);
        this.expectedField.setFont(fieldFont);
        this.expectedField.getEmptyText().setFont(fieldFont);
        this.expectedField.getEmptyText().setText(placeholder);
        this.expectedField.setBorder(JBUI.Borders.empty(10));
        this.expectedField.setExtensions(new ExtendableTextComponent.Extension() {
            @Override
            public Icon getIcon(boolean hovered) {
                return icon;
            }

            @Override
            public boolean isIconBeforeText() {
                return true;
            }

            @Override
            public int getIconGap() {
                return JBUI.scale(8);
            }
        });

        this.wrapperPanel = new JPanel(new BorderLayout());
        this.wrapperPanel.setOpaque(false);
        this.wrapperPanel.add(this.expectedField, BorderLayout.CENTER);
        this.wrapperPanel.setBorder(JBUI.Borders.emptyTop(8));
    }

    // unused
    public void registerShortcut(JPanel mainPanel, JPanel contentPanel, Runnable repackPopup) {
        CustomShortcutSet shortcut = KeyboardSet.getShortcutFor(UpdateField.EXPECTED.getShortcut(), InputEvent.CTRL_DOWN_MASK);

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (wrapperPanel.getParent() == null) {
                    contentPanel.add(wrapperPanel);
                }
                repackPopup.run();
                expectedField.requestFocus();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(shortcut, mainPanel);
    }

    public ExtendableTextField getField() {
        return expectedField;
    }

    public JPanel getWrapper() {
        return wrapperPanel;
    }

    public Runnable getShowAction(JPanel contentPanel, Runnable repackPopup) {
        return () -> {
            if (wrapperPanel.getParent() == null) {
                contentPanel.add(wrapperPanel);
            }
            repackPopup.run();
            expectedField.requestFocus();
        };
    }
}