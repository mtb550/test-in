package testGit.ui.single.nnew;

import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class TitleSection implements CreateTestCaseSection {
    @Getter
    private final ExtendableTextField titleField;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    private boolean isError = false;

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
                            g2.setColor(isError ? JBColor.RED : UIUtil.getContextHelpForeground());
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
        this.titleField.getEmptyText().setText(CreateField.TITLE.getLabel());
        this.titleField.setBorder(JBUI.Borders.empty(10));

        this.titleField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                resetError();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resetError();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resetError();
            }

            private void resetError() {
                if (isError) setError(false);
            }
        });

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

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(this.titleField, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    public void setError(boolean error) {
        this.isError = error;
        titleField.getEmptyText().clear();

        if (error) {
            titleField.getEmptyText().appendText(CreateField.TITLE.getLabel(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED));
            titleField.requestFocus();
        } else
            titleField.getEmptyText().appendText(CreateField.TITLE.getLabel());

        titleField.repaint();
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        titleField.requestFocus();
    }

    @Override
    public void applyTo(TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setTitle(titleField.getText().trim());
        }
    }
}