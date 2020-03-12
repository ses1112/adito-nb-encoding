package de.adito.nbm.encoding.statusline;

import org.openide.util.ImageUtilities;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * @author m.kaspera, 13.02.2020
 */
public class EncodingListCellRenderer extends JPanel implements ListCellRenderer<String>
{

  private final Set<String> supportedEncodings;
  private final JLabel encodingLabel = new JLabel();
  private final JLabel isSupportedLabel = new JLabel();
  private final ImageIcon warningIcon;
  private final Color backgroundColor = getBackground();
  private final Color foregroundColor = getForeground();

  public EncodingListCellRenderer(Set<String> pSupportedEncodings)
  {
    supportedEncodings = pSupportedEncodings;
    warningIcon = new ImageIcon(ImageUtilities.loadImage("de/adito/nbm/encoding/warning12.png"));
    setLayout(new BorderLayout(3, 0));
    isSupportedLabel.setPreferredSize(new Dimension(16, 16));
    add(isSupportedLabel, BorderLayout.WEST);
    add(encodingLabel, BorderLayout.CENTER);
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends String> pList, String value, int index, boolean pIsSelected, boolean cellHasFocus)
  {
    setComponentOrientation(pList.getComponentOrientation());

    if (pIsSelected)
    {
      setBackground(pList.getSelectionBackground());
      setForeground(pList.getSelectionForeground());
      encodingLabel.setBackground(pList.getSelectionBackground());
      encodingLabel.setForeground(pList.getSelectionForeground());
    }
    else
    {
      setBackground(backgroundColor);
      setForeground(foregroundColor);
      encodingLabel.setBackground(backgroundColor);
      encodingLabel.setForeground(foregroundColor);
    }
    setEnabled(pList.isEnabled());

    if (supportedEncodings.contains(value.toUpperCase()))
    {
      isSupportedLabel.setIcon(null);
    }
    else
    {
      isSupportedLabel.setIcon(warningIcon);
    }
    encodingLabel.setText(value);
    return this;
  }

  /*
  Below are methods changed/overriden for performance reasons
   */


  /**
   * see {@link }
   * <p>
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   *
   * @return <code>true</code> if the background is completely opaque
   * and differs from the JList's background;
   * <code>false</code> otherwise
   * @since 1.5
   */
  @Override
  public boolean isOpaque()
  {
    Color back = getBackground();
    Component p = getParent();
    if (p != null)
    {
      p = p.getParent();
    }
    // p should now be the JList.
    boolean colorMatch = (back != null) && (p != null) &&
        back.equals(p.getBackground()) &&
        p.isOpaque();
    return !colorMatch && super.isOpaque();
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   *
   * @since 1.5
   */
  @Override
  public void repaint()
  {
    //Overridden for performance reasons.
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void repaint(long pTm, int pX, int pY, int pWidth, int pHeight)
  {
    //Overridden for performance reasons.
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void repaint(Rectangle pR)
  {
    //Overridden for performance reasons.
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  protected void firePropertyChange(String pPropertyName, Object pOldValue, Object pNewValue)
  {
    // Strings get interned...
    if ("text".equals(pPropertyName)
        || (("font".equals(pPropertyName) || "foreground".equals(pPropertyName))
        && pOldValue != pNewValue
        && getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) != null))
    {

      super.firePropertyChange(pPropertyName, pOldValue, pNewValue);
    }
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String pPropertyName, byte pOldValue, byte pNewValue)
  {
    //Overridden for performance reasons.
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String pPropertyName, char pOldValue, char pNewValue)
  {
    //Overridden for performance reasons.
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String pPropertyName, short pOldValue, short pNewValue)
  {
    //Overridden for performance reasons.
  }

  /**
   * .
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String pPropertyName, int pOldValue, int pNewValue)
  {
    //Overridden for performance reasons
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String pPropertyName, long pOldValue, long pNewValue)
  {
    //Overridden for performance reasons.

  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String pPropertyName, float pOldValue, float pNewValue)
  {
    //Overridden for performance reasons.
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String pPropertyName, double pOldValue, double pNewValue)
  {
    //Overridden for performance reasons.
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String pPropertyName, boolean pOldValue, boolean pNewValue)
  {
    //Overridden for performance reasons.
  }
}
