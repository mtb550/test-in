package testGit.ui.single.nnew;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class TitleSection {
    private final ExtendableTextField titleField;
    private final JPanel wrapperPanel;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);


    public TitleSection() {
        this.titleField = new ExtendableTextField() {
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

                            g2.drawString(CreateField.TITLE.getLabel(), x, y);
                            g2.dispose();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        this.titleField.setFont(fieldFont);
        this.titleField.getEmptyText().setFont(fieldFont);
        this.titleField.getEmptyText().setText(CreateField.TITLE.getPlaceHolder());
        this.titleField.setBorder(JBUI.Borders.empty(10));

        this.titleField.setExtensions(new ExtendableTextComponent.Extension() {
            @Override
            public Icon getIcon(boolean hovered) {
                return CreateField.TITLE.getIcon();
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
        this.wrapperPanel.add(this.titleField, BorderLayout.CENTER);
        this.wrapperPanel.setBorder(JBUI.Borders.emptyTop(8));
    }

    public ExtendableTextField getField() {
        return titleField;
    }

    public JPanel getWrapper() {
        return wrapperPanel;
    }

    public void showSection(JPanel contentPanel) {
        if (wrapperPanel.getParent() == null)
            contentPanel.add(wrapperPanel, 0);
        titleField.requestFocus();
    }

}