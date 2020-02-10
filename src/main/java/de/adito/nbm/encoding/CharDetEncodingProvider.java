package de.adito.nbm.encoding;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.universalchardet.UniversalDetector;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author m.kaspera, 07.02.2020
 */
@ServiceProvider(service = FileEncodingQueryImplementation.class, position = 103)
public class CharDetEncodingProvider extends FileEncodingQueryImplementation
{

  private final Cache<_FileDescription, Optional<Charset>> cache =
      CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

  @Nullable
  @Override
  public Charset getEncoding(@NotNull FileObject pFileObject)
  {
    try
    {
      return cache.get(new _FileDescription(pFileObject), () -> {
        try (InputStream in = new BufferedInputStream(pFileObject.getInputStream()))
        {
          UniversalDetector detector = new UniversalDetector(null);
          byte[] buf = new byte[4096];
          int nread;
          while ((nread = in.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
          }
          detector.dataEnd();
          String detectedCharset = detector.getDetectedCharset();
          if(detectedCharset != null)
            return java.util.Optional.of(Charset.forName(detectedCharset));
          return java.util.Optional.empty();
        }
      }).orElse(null);
    }
    catch (ExecutionException e)
    {
      // Wenn das Encoding nicht bestimmt werden kann, soll das eine andere Implementierung liefern.
      return null;
    }
  }

  /**
   * Beschreibung einer Datei für den Cache.
   */
  private final static class _FileDescription
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
