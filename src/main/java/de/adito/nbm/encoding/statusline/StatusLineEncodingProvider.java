package de.adito.nbm.encoding.statusline;

import de.adito.nbm.encoding.CharDetEncodingProvider;
import de.adito.swing.popup.*;
import org.jetbrains.annotations.Nullable;
import org.mozilla.universalchardet.Constants;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.awt.*;
import org.openide.filesystems.*;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.*;

/**
 * @author m.kaspera, 07.02.2020
 */
@ServiceProvider(service = StatusLineElementProvider.class)
public class StatusLineEncodingProvider implements StatusLineElementProvider, PropertyChangeListener, FileChangeListener
{

  private static final String ENTER_KEY_STRING = "ENTER";
  private final JLabel encodingLabel = new JLabel("encoding here");
  private final JPanel encodingPanel;
  private final JList<String> encodingList;
  private final List<String> pluginSupportedEncodings = new ArrayList<>();
  private FileObject lastFileObject;
  private PopupWindow popupWindow;

  public StatusLineEncodingProvider()
  {
    _getSupportedEncodings();
    encodingLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
    encodingPanel = new JPanel(new BorderLayout());
    encodingPanel.add(new StatusLineSeparator(), BorderLayout.WEST);
    encodingPanel.add(encodingLabel, BorderLayout.CENTER);
    EditorRegistry.addPropertyChangeListener(this);

    encodingList = new _JListWithTooltips(new HashSet<>(pluginSupportedEncodings));
    _setupEncodingList();
    if (SwingUtilities.isEventDispatchThread())
    {
      popupWindow = new PopupWindow(WindowManager.getDefault().getMainWindow(), "File encoding", encodingList);
      encodingLabel.addMouseListener(new PopupMouseAdapter(popupWindow, encodingPanel, encodingList));
    }
    else
    {
      SwingUtilities.invokeLater(() -> {
        popupWindow = new PopupWindow(WindowManager.getDefault().getMainWindow(), "File encoding", encodingList);
        encodingLabel.addMouseListener(new PopupMouseAdapter(popupWindow, encodingPanel, encodingList));
      });
    }
  }

  /**
   * Sets the model, cell renderer, action/inputMap and all other necessary settings for the encodingList
   */
  private void _setupEncodingList()
  {
    DefaultListModel<String> model = new DefaultListModel<>();

    for (String k : Charset.availableCharsets().keySet())
    {
      model.addElement(k);
    }

    encodingList.setModel(model);
    encodingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    encodingList.setCellRenderer(new EncodingListCellRenderer(pluginSupportedEncodings));
    encodingList.setFocusable(true);
    encodingList.getInputMap().put(KeyStroke.getKeyStroke(ENTER_KEY_STRING), KeyStroke.getKeyStroke(ENTER_KEY_STRING));
    encodingList.getActionMap().put(KeyStroke.getKeyStroke(ENTER_KEY_STRING), new AbstractAction()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        String selectedEncoding = encodingList.getSelectedValue();
        if (selectedEncoding != null)
        {
          _setEncoding(selectedEncoding);
          popupWindow.disposeWindow();
        }
      }
    });
    encodingList.addMouseListener(new EncodingSelectionMouseListener());
  }

  /**
   * retrieve and store the supported encodings by juniversalchardet. Works via reflection
   */
  private void _getSupportedEncodings()
  {
    Field[] declaredFields = Constants.class.getDeclaredFields();
    for (Field declaredField : declaredFields)
    {
      if (Modifier.isStatic(declaredField.getModifiers()) && declaredField.getType() == String.class)
      {
        try
        {
          pluginSupportedEncodings.add(((String) declaredField.get(null)).toUpperCase());
        }
        catch (IllegalAccessException pE)
        {
          // nothing, not added to list
        }
      }
    }
  }

  @Override
  public Component getStatusLineElement()
  {
    return encodingPanel;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    _update();
  }

  /**
   * Update listeners and the label showing the encoding of the currently selected editor
   */
  private void _update()
  {
    JTextComponent textComponent = getTextComponent();
    if (textComponent == null) return;
    Document document = textComponent.getDocument();
    if (document == null)
    {
      return;
    }

    FileObject fileObject = NbEditorUtilities.getFileObject(document);
    _updateListeners(fileObject);
    _updateLabel(fileObject);
  }

  /**
   * removes the "old" fileChangeListener and attaches it to the given fileObject, if non-null
   *
   * @param pFileObject new FileObject to attach the fileChangeListener
   */
  private void _updateListeners(@Nullable FileObject pFileObject)
  {
    if (lastFileObject != null)
    {
      lastFileObject.removeFileChangeListener(this);
    }
    if (pFileObject != null)
    {
      lastFileObject = pFileObject;
      pFileObject.addFileChangeListener(this);
    }
    else
    {
      lastFileObject = null;
    }
  }

  /**
   * retrieve the encoding for the given FileObject and set the text of the label
   *
   * @param pFileObject FileObject to analyse
   */
  private void _updateLabel(@Nullable FileObject pFileObject)
  {
    Charset encoding = null;
    if (pFileObject != null)
    {
      CharDetEncodingProvider encodingProvider = Lookup.getDefault().lookup(CharDetEncodingProvider.class);
      encoding = encodingProvider.getEncoding(pFileObject);
      if (encoding == null)
        encoding = FileEncodingQuery.getEncoding(pFileObject);
    }
    encodingLabel.setText(encoding == null ? "N/A" : encoding.toString());
  }

  /**
   * Get the TextComponent currently or last selected
   *
   * @return TextComponent or null
   */
  @Nullable
  private JTextComponent getTextComponent()
  {
    JTextComponent textComponent = EditorRegistry.focusedComponent();
    if (textComponent == null)
      textComponent = EditorRegistry.lastFocusedComponent();
    return textComponent;
  }

  /**
   * Determines the dataObject of the current TextComponent, reads the contents in the current encoding and the writes the content in the
   * selected encoding to file
   *
   * @param pSelectedEncoding encoding that should be used to write the file contents to disk
   */
  private void _setEncoding(String pSelectedEncoding)
  {
    JTextComponent textComponent = getTextComponent();
    if (textComponent == null)
      return;
    DataObject dataObject = NbEditorUtilities.getDataObject(textComponent.getDocument());
    FileObject fileObject = dataObject.getPrimaryFile();
    try
    {
      byte[] fileContents = fileObject.asBytes();
      Charset currentEncoding = FileEncodingQuery.getEncoding(fileObject);
      try (OutputStream outputStream = fileObject.getOutputStream())
      {
        byte[] changedEncodingBytes = new String(fileContents, currentEncoding).getBytes(pSelectedEncoding);
        outputStream.write(changedEncodingBytes);
      }
    }
    catch (IOException pE)
    {
      NotificationDisplayer.getDefault().notify(pE.getClass().getSimpleName() + " while setting encoding",
                                                NotificationDisplayer.Priority.NORMAL.getIcon(), pE.getMessage(), null);
    }
    encodingList.clearSelection();
  }

  /*
    Start FileChangeListener methods
   */

  @Override
  public void fileFolderCreated(FileEvent fe)
  {
    // nothing, only listening to files
  }

  @Override
  public void fileDataCreated(FileEvent fe)
  {
    // not interested, only listening to existing files
  }

  @Override
  public void fileChanged(FileEvent fe)
  {
    _updateLabel(fe.getFile());
  }

  @Override
  public void fileDeleted(FileEvent fe)
  {
    // not interested
  }

  @Override
  public void fileRenamed(FileRenameEvent fe)
  {
    // not interested
  }

  @Override
  public void fileAttributeChanged(FileAttributeEvent fe)
  {
    // not interested
  }

  /*
  End FileChangeListener methods
   */

  /**
   * Vertical Separator configured for the statusline
   */
  private static class StatusLineSeparator extends JSeparator
  {
    public StatusLineSeparator()
    {
      super(SwingConstants.VERTICAL);
      setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    }

    @Override
    public Dimension getPreferredSize()
    {
      return new Dimension(3, 3); // Y-unimportant -> gridlayout will stretch it
    }
  }

  /**
   * MouseListener that sets the encoding to the clicked one and disposes the window on mouseclick
   */
  private class EncodingSelectionMouseListener extends MouseAdapter
  {
    @Override
    public void mouseClicked(MouseEvent e)
    {
      JList<String> source = (JList<String>) e.getSource();
      int index = source.locationToIndex(e.getPoint());
      if (index != -1)
      {
        _setEncoding(source.getModel().getElementAt(index));
        popupWindow.disposeWindow();
      }
    }
  }

  /**
   * JList of Strings that displays a warning message if the encoding of the hovered-over cell is not in the list of supported encodings
   */
  private static class _JListWithTooltips extends JList<String>
  {

    private final Set<String> supportedEncodings;

    public _JListWithTooltips(Set<String> pSupportedEncodings)
    {
      super();
      supportedEncodings = pSupportedEncodings;
    }

    @Override
    public String getToolTipText(MouseEvent event)
    {
      int rowIndex = locationToIndex(event.getPoint());
      String encoding = getModel().getElementAt(rowIndex);
      if (!supportedEncodings.contains(encoding))
        return "This encoding may be set, but the plugin will not recognize it and text may be displayed improperly";
      else
        return null;
    }
  }
}
