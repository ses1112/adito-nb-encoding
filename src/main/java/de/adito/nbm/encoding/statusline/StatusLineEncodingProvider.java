package de.adito.nbm.encoding.statusline;

import de.adito.nbm.encoding.CharDetEncodingProvider;
import de.adito.swing.KeyForwardAdapter;
import de.adito.swing.popup.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import org.mozilla.universalchardet.Constants;
import org.netbeans.api.actions.Savable;
import org.netbeans.api.queries.FileEncodingQuery;
import org.openide.*;
import org.openide.awt.*;
import org.openide.filesystems.*;
import org.openide.loaders.DataObject;
import org.openide.util.*;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

  private static final String ENCODING_ATTRIBUTE = "ENCODING";
  private static final String ENTER_KEY_STRING = "ENTER";
  private final JLabel encodingLabel = new JLabel("N/A");
  private final JPanel encodingPanel;
  private final JList<String> encodingList;
  private final List<String> pluginSupportedEncodings = new ArrayList<>();
  private final Icon warningIcon = new ImageIcon(ImageUtilities.loadImage("de/adito/nbm/encoding/warning12.png"));
  private FileObject lastFileObject;
  private PopupWindow popupWindow;
  private EncodingQuickSearchCallback quickSearchCallback;

  public StatusLineEncodingProvider()
  {
    _getSupportedEncodings();
    encodingLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
    encodingPanel = new JPanel(new BorderLayout());
    encodingPanel.add(new StatusLineSeparator(), BorderLayout.WEST);
    encodingPanel.add(encodingLabel, BorderLayout.CENTER);
    TopComponent.getRegistry().addPropertyChangeListener(this);

    encodingList = new _JListWithTooltips(new HashSet<>(pluginSupportedEncodings));
    _setupEncodingList();
    quickSearchCallback = new EncodingQuickSearchCallback(encodingList);
    if (SwingUtilities.isEventDispatchThread())
    {
      popupWindow = new PopupWindow(WindowManager.getDefault().getMainWindow(), "File encoding", encodingList, quickSearchCallback);
      encodingLabel.addMouseListener(new PopupMouseAdapter(popupWindow, encodingPanel, encodingList));
    }
    else
    {
      SwingUtilities.invokeLater(() -> {
        popupWindow = new PopupWindow(WindowManager.getDefault().getMainWindow(), "File encoding", encodingList, quickSearchCallback);
        encodingLabel.addMouseListener(new PopupMouseAdapter(popupWindow, encodingPanel, encodingList));
      });
    }
    encodingList.addKeyListener(new KeyForwardAdapter(popupWindow.getSearchAttachComponent()));
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
          quickSearchCallback.quickSearchConfirmed();
          popupWindow.disposeWindow();
        }
      }
    });
    encodingList.addMouseListener(new EncodingSelectionMouseListener());
    encodingList.addMouseListener(new HoverMouseListener());
    encodingList.addMouseMotionListener(new HoverMouseListener());
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
    FileObject fileObject = _getFileObject();
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
    String fileAttrEncoding = null;
    if (pFileObject != null)
    {
      CharDetEncodingProvider encodingProvider = Lookup.getDefault().lookup(CharDetEncodingProvider.class);
      encoding = encodingProvider.getEncoding(pFileObject);
      if (encoding == null)
        encoding = FileEncodingQuery.getEncoding(pFileObject);
      fileAttrEncoding = (String) pFileObject.getAttribute(ENCODING_ATTRIBUTE);
    }
    if (fileAttrEncoding != null && !Charset.forName(fileAttrEncoding).equals(encoding))
    {
      encodingLabel.setToolTipText("Detected different encodings for file attribute and charset detection. File attribute: "
                                       + fileAttrEncoding + ", charset detection: " + encoding);
      encodingLabel.setIcon(warningIcon);
    }
    else
    {
      encodingLabel.setToolTipText(null);
      encodingLabel.setIcon(null);
    }
    encodingLabel.setText(encoding == null ? "N/A" : encoding.toString());
  }

  /**
   * Determines the dataObject of the current TextComponent, reads the contents in the current encoding and the writes the content in the
   * selected encoding to file
   *
   * @param pSelectedEncoding encoding that should be used to write the file contents to disk
   */
  private void _setEncoding(String pSelectedEncoding)
  {
    FileObject fileObject = _getFileObject();
    if (fileObject == null)
      return;
    _saveAll();
    try
    {
      fileObject.setAttribute(ENCODING_ATTRIBUTE, pSelectedEncoding);
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

  @Nullable
  private FileObject _getFileObject()
  {
    Mode editorMode = WindowManager.getDefault().findMode("editor");
    if (editorMode == null)
      return null;
    TopComponent selectedTopComponent = editorMode.getSelectedTopComponent();
    if (selectedTopComponent == null)
      return null;
    return selectedTopComponent.getLookup().lookup(FileObject.class);
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
        quickSearchCallback.quickSearchCanceled();
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

  private static void _saveAll()
  {
    Map<Savable, IOException> couldNotSave = new LinkedHashMap<>();
    // Alles aus 'Savable.REGISTRY' speichern.
    for (Savable savable : Savable.REGISTRY.lookupAll(Savable.class))
      if (!_save(savable, couldNotSave))
        return;
    // Alte Implementierungen finden sich u.U. nur in 'DataObject.getRegistry()'.
    for (DataObject dataObject : DataObject.getRegistry().getModifiedSet())
      for (Savable savable : dataObject.getLookup().lookupAll(Savable.class))
        if (!_save(savable, couldNotSave))
          return;

    if (!couldNotSave.isEmpty())
      _warnCouldNotSave(couldNotSave);
  }

  /**
   * Speichert das Savable, das übergeben wird
   *
   * @param pSavable Savable das gespeichert werden soll
   */
  private static boolean _save(Savable pSavable, Map<Savable, IOException> pCouldNotSave)
  {
    try
    {
      try
      {
        pSavable.save();
      }
      catch (UserQuestionException e)
      {
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(e.getLocalizedMessage(), NotifyDescriptor.YES_NO_CANCEL_OPTION);
        Object res = DialogDisplayer.getDefault().notify(nd);
        if (NotifyDescriptor.YES_OPTION.equals(res))
          e.confirmed();
        else if (NotifyDescriptor.CANCEL_OPTION.equals(res))
          return false;
      }
    }
    catch (IOException e)
    {
      //noinspection ThrowableResultOfMethodCallIgnored
      pCouldNotSave.put(pSavable, e);
    }
    return true;
  }

  private static void _warnCouldNotSave(Map<Savable, IOException> pCouldNotSave)
  {
    StringBuilder notSaveObjectsStr = new StringBuilder();
    StringBuilder details = new StringBuilder();
    for (Map.Entry<Savable, IOException> entry : pCouldNotSave.entrySet())
    {
      Savable savable = entry.getKey();
      IOException exception = entry.getValue();

      if (notSaveObjectsStr.length() != 0)
        notSaveObjectsStr.append("\n");
      notSaveObjectsStr.append(savable);

      if (details.length() != 0)
        details.append("\n\n");
      details.append(savable).append(":\n").append(ExceptionUtils.getStackTrace(exception));
    }

    throw new RuntimeException("Couldn't save a modified file before setting the encoding for the current file.\nFiles: "
                                   + notSaveObjectsStr.toString() + "\nDetails: " + details);
  }

}
