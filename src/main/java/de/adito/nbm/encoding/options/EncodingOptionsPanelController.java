package de.adito.nbm.encoding.options;

import de.adito.nbm.encoding.CharDetEncodingProvider;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.*;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

/**
 * @author m.kaspera, 18.09.2020
 */
@OptionsPanelController.SubRegistration(displayName = "Encoding", id = "encoding", position = 500, location = "Adito")
public class EncodingOptionsPanelController extends OptionsPanelController
{

  private final Preferences preferences;
  private final EncodingOptionsPanel encodingOptionsPanel;
  private String bufferValue;

  public EncodingOptionsPanelController()
  {
    preferences = NbPreferences.forModule(EncodingOptionsPanel.class);
    encodingOptionsPanel = new EncodingOptionsPanel();
  }

  @Override
  public void update()
  {
    bufferValue = preferences.get(CharDetEncodingProvider.ENCODING_KEY, CharDetEncodingProvider.DEFAULT_DEFAULT_ENCODING);
    encodingOptionsPanel.setEncoding(bufferValue);
  }

  @Override
  public void applyChanges()
  {
    preferences.put(CharDetEncodingProvider.ENCODING_KEY, encodingOptionsPanel.getEncoding());
    bufferValue = encodingOptionsPanel.getEncoding();
  }

  @Override
  public void cancel()
  {
    // nothing to do here, the text field is re-set via the preferences if called again, and the current value is not stored anyways
  }

  @Override
  public boolean isValid()
  {
    return true;
  }

  @Override
  public boolean isChanged()
  {
    return !bufferValue.equals(encodingOptionsPanel.getEncoding());
  }

  @Override
  public JComponent getComponent(Lookup masterLookup)
  {
    return encodingOptionsPanel;
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l)
  {
    encodingOptionsPanel.getEncodingsComboBox().addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l)
  {
    encodingOptionsPanel.getEncodingsComboBox().removePropertyChangeListener(l);
  }
}
