package de.adito.nbm.encoding.options;

import de.adito.nbm.encoding.CharDetEncodingProvider;
import de.adito.nbm.encoding.statusline.StatusLineEncodingProvider;
import de.adito.swing.TableLayoutUtil;
import info.clearthought.layout.TableLayout;
import org.openide.util.NbPreferences;

import javax.swing.*;
import java.util.List;

/**
 * @author m.kaspera, 18.09.2020
 */
public class EncodingOptionsPanel extends JPanel
{

  private final JComboBox<String> encodingsComboBox;

  public EncodingOptionsPanel()
  {
    List<String> supportedEncodings = StatusLineEncodingProvider._getSupportedEncodings();
    supportedEncodings.add(CharDetEncodingProvider.NO_DEFAULT_ENCODING);
    encodingsComboBox = new JComboBox<>(supportedEncodings.toArray(new String[0]));
    encodingsComboBox.setSelectedItem(NbPreferences.forModule(EncodingOptionsPanel.class)
                                          .get(CharDetEncodingProvider.ENCODING_KEY, CharDetEncodingProvider.DEFAULT_DEFAULT_ENCODING));
    double fill = TableLayout.FILL;
    double pref = TableLayout.PREFERRED;
    final double gap = 15;
    double[] cols = {gap, pref, gap, fill, gap};
    double[] rows = {gap,
                     pref,
                     gap,
                     pref,
                     gap};
    setLayout(new TableLayout(cols, rows));
    TableLayoutUtil tlu = new TableLayoutUtil(this);
    tlu.add(1, 1, new JLabel("Default encoding"));
    tlu.add(3, 1, encodingsComboBox);
  }

  /**
   * sets the bufferSize field
   *
   * @param pEncoding size of the buffer that should be set in the field
   */
  void setEncoding(String pEncoding)
  {
    encodingsComboBox.setSelectedItem(pEncoding);
  }

  /**
   * @return the bufferSize currently set in the field
   */
  String getEncoding()
  {
    return (String) encodingsComboBox.getSelectedItem();
  }

  /**
   * @return the JTextField used for displaying the bufferSize
   */
  JComboBox<String> getEncodingsComboBox()
  {
    return encodingsComboBox;
  }
}
