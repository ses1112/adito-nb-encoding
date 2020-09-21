package de.adito.nbm.encoding;

import com.google.common.cache.*;
import de.adito.nbm.encoding.options.EncodingOptionsPanel;
import org.jetbrains.annotations.*;
import org.mozilla.universalchardet.UniversalDetector;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author m.kaspera, 07.02.2020
 */
@ServiceProvider(service = FileEncodingQueryImplementation.class, position = 99)
public class CharDetEncodingProvider extends FileEncodingQueryImplementation
{

  public static final String ENCODING_KEY = "de.adito.plugins.encoding.default.encoding";
  public static final String NO_DEFAULT_ENCODING = "None";
  public static final String DEFAULT_DEFAULT_ENCODING = "";

  private final Cache<_FileDescription, Optional<Charset>> cache =
      CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

  @Nullable
  @Override
  public Charset getEncoding(@NotNull FileObject pFileObject)
  {
    try
    {
      String defaultEncoding = NbPreferences.forModule(EncodingOptionsPanel.class).get(ENCODING_KEY, DEFAULT_DEFAULT_ENCODING);
      // If no default encoding is set
      if (NO_DEFAULT_ENCODING.equals(defaultEncoding) || pFileObject.getSize() > 0)
      {
        Charset encoding = null;
        Object fileAttributesObj = pFileObject.getAttribute("ENCODING");
        if (fileAttributesObj != null)
          encoding = Charset.forName((String) fileAttributesObj);
        Charset uChardetEncoding = cache.get(new _FileDescription(pFileObject), () -> _getEncoding(pFileObject)).orElse(null);
        if (encoding != null && uChardetEncoding != null)
        {
          if (!encoding.equals(uChardetEncoding))
            encoding = uChardetEncoding;
        }
        else if (uChardetEncoding != null)
        {
          encoding = uChardetEncoding;
        }
        return encoding;
      }
      else
      {
        pFileObject.setAttribute("ENCODING", defaultEncoding);
        return Charset.forName(defaultEncoding);
      }
    }
    catch (ExecutionException | IOException | IllegalCharsetNameException | UnsupportedCharsetException e)
    {
      // Wenn das Encoding nicht bestimmt werden kann, soll das eine andere Implementierung liefern.
      return null;
    }
  }

  /**
   * Get the encoding of the fileObject by using UCharDet
   *
   * @param pFileObject FileObject
   * @return Optional of the Charset, empty optional if no Charset is detected/the confidence is too low
   * @throws IOException IOException if e.g. the file cannot be read
   */
  private Optional<Charset> _getEncoding(FileObject pFileObject) throws IOException
  {
    try (InputStream in = new BufferedInputStream(pFileObject.getInputStream()))
    {
      UniversalDetector detector = new UniversalDetector(null);
      byte[] buf = new byte[4096];
      int nread;
      while ((nread = in.read(buf)) > 0 && !detector.isDone())
      {
        detector.handleData(buf, 0, nread);
      }
      detector.dataEnd();
      String detectedCharset = detector.getDetectedCharset();
      if (detectedCharset != null)
        return Optional.of(Charset.forName(detectedCharset));
      return Optional.empty();
    }
  }

  /**
   * Beschreibung einer Datei für den Cache.
   */
  private static final class _FileDescription
  {
    private String path;
    private long lastModified;
    private long size;

    private _FileDescription(FileObject pFo)
    {
      path = pFo.getPath();
      Date lm = pFo.lastModified();
      lastModified = lm == null ? 0 : lm.getTime();
      size = pFo.getSize();
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      if (!(o instanceof _FileDescription))
        return false;
      _FileDescription that = (_FileDescription) o;
      return lastModified == that.lastModified && size == that.size && path.equals(that.path);
    }

    @Override
    public int hashCode()
    {
      int result = path.hashCode();
      result = 31 * result + (int) (lastModified ^ (lastModified >>> 32));
      result = 31 * result + (int) (size ^ (size >>> 32));
      return result;
    }
  }
}
