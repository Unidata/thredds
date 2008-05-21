package thredds.server.config;

import java.io.File;
import java.util.List;

/**
 * Implements the FileSource interface using a chain of DescendantFileSource
 * objects. This allows a relative path to be given and located in the first
 * DescendantFileSource that contains a matching File.
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

    for ( DescendantFileSource dfs : chain)
      if ( dfs == null )
        throw new IllegalArgumentException( "Locator chain must not contain null items.");
    this.chain = chain;
  }

  /**
   * This implementation requires a relative path. The relative path may
   * not start with "../" or once "normalized" start with "../". Here
   * "normalized" means that "./" and "path/.." segments are removed,
   * e.g., "dir1/../../dir2" once normalized would be "../dir2".
   *
   * @param path the relative path to the desired File.
   * @return the File represented by the given relative path or null if the path is null or the File it represents does not exist.
   */
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
