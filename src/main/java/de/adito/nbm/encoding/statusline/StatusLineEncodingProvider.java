package de.adito.nbm.encoding.statusline;

import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.awt.StatusLineElementProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.beans.*;
import java.nio.charset.Charset;

/**
 * @author m.kaspera, 07.02.2020
 */
@ServiceProvider(service = StatusLineElementProvider.class)
public class StatusLineEncodingProvider implements StatusLineElementProvider, PropertyChangeListener
{

  private final JLabel encodingLabel = new JLabel("encoding here");
  private final JPanel encodingPanel;

  public StatusLineEncodingProvider()
  {
    JSeparator separator = new JSeparator(SwingConstants.VERTICAL)
    {
      @Override
      public Dimension getPreferredSize()
      {
        return new Dimension(3, 3); // Y-unimportant -> gridlayout will stretch it
      }
    };
    separator.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

    encodingLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
    encodingPanel = new JPanel(new BorderLayout());
    encodingPanel.add(separator, BorderLayout.WEST);
    encodingPanel.add(encodingLabel, BorderLayout.CENTER);
    EditorRegistry.addPropertyChangeListener(this);
  }

  @Override
  public Component getStatusLineElement()
  {
    return encodingPanel;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    _updateLabel();
  }

  private void _updateLabel()
  {
    JTextComponent textComponent = EditorRegistry.focusedComponent();
    if (textComponent == null)
      textComponent = EditorRegistry.lastFocusedComponent();
    if (textComponent == null)
      return;
    Document document = textComponent.getDocument();
    if (document == null)
    {
      return;
    }

    FileObject fileObject = NbEditorUtilities.getFileObject(document);
    Charset encoding = FileEncodingQuery.getEncoding(fileObject);
    encodingLabel.setText(encoding == null ? "N/A" : encoding.toString());
  }
}
