package thredds.server.config;

import java.io.File;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ChainedFileSource implements FileSource
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( ChainedFileSource.class );

  private final List<DescendantFileSource> chain;

  public ChainedFileSource( List<DescendantFileSource> chain )
  {
    if ( chain == null || chain.isEmpty() )
      throw new IllegalArgumentException( "Locator chain must not be null or empty.");
    this.chain = chain;
  }

  public File getFile( String path )
  {
    File file;
    for ( DescendantFileSource curLocator : chain )
    {
      file = curLocator.getFile( path );
      if ( file != null )
        return file;
    }
    return null;
  }

}
